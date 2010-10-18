package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {
	
	IASTFunctionDefinition memberdefinition;

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		super.checkInitialConditions(pm);
		if (initStatus.hasFatalError()) {
			return initStatus;
		}
		//replace this with indexing
		memberdefinition = ToggleSelectionHelper.getFirstSelectedFunctionDefinition(region, unit);
		
		if (memberdefinition == null) {
			initStatus.addFatalError("could not get a definition");
		}
		else if (!(memberdefinition instanceof IASTFunctionDefinition)) {
			System.out.println("Is not a declaration abort");
			initStatus.addFatalError("Is not a function definition");
		}
		return initStatus;
	}

	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new CRefactoringDescription("id", "proj", "desc", "comment", 0,
				new HashMap<String, String>()) {
			@Override
			public Refactoring createRefactoring(RefactoringStatus status)
					throws CoreException {
				return new ToggleRefactoring(getFile(), getSelection(),
						getCProject());
			}
		};
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException {
		try {
			lockIndex();
			try {
				collectMoveChanges(collector);
			} finally {
				unlockIndex();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void collectMoveChanges(ModificationCollector collector) {
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition();
		
		IASTDeclSpecifier newdeclspec = memberdefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = new CPPASTFunctionDeclarator(getQualifiedName());
		//TODO: add the parameters
		
		IASTStatement newbody = memberdefinition.getBody().copy();
		IASTFunctionDefinition newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl, newbody);
		ICPPASTTemplateDeclaration decl = getTemplateDeclaration();
		decl.setDeclaration(newfunc);
		newfunc.setParent(decl);
		
		ASTRewrite rewrite = collector.rewriterForTranslationUnit(unit);
		TextEditGroup edit = new TextEditGroup("Toggle");
		rewrite.replace(memberdefinition, declaration, edit);
		rewrite.insertBefore(unit, null, decl, edit);
	}

	private ICPPASTTemplateDeclaration getTemplateDeclaration() {
		IASTNode node = memberdefinition;
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTTemplateDeclaration)
				break;
		}
		return (ICPPASTTemplateDeclaration) node.copy();
	}

	private IASTSimpleDeclaration createDeclarationFromDefinition() {
		IASTFunctionDeclarator declarator = memberdefinition.getDeclarator().copy();
		IASTDeclSpecifier specifier = memberdefinition.getDeclSpecifier().copy();
		IASTSimpleDeclaration result = new CPPASTSimpleDeclaration(specifier);
		result.addDeclarator(declarator);
		return result;
	}

	private ICPPASTQualifiedName getQualifiedName() {
		ICPPASTQualifiedName newdecl = new CPPASTQualifiedName();
		for (IASTName name : getAllQualifiedNames(memberdefinition)) {
			newdecl.addName(name.copy());
		}
		newdecl.addName(memberdefinition.getDeclarator().getName().copy());
		newdecl.setFullyQualified(true);
		return newdecl;
	}
	
	private ArrayList<IASTName> getAllQualifiedNames(IASTFunctionDefinition memberdefinition) {
		ArrayList<IASTName> names = new ArrayList<IASTName>();
		IASTNode node = memberdefinition; 
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				names.add(((ICPPASTCompositeTypeSpecifier) node).getName());
			}
		}
		Collections.reverse(names);
		return names;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
	throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return super.checkFinalConditions(pm);
	}
}
