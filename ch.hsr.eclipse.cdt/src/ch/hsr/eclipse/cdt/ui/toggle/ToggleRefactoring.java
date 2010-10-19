package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {
	
	IASTFunctionDefinition memberdefinition;
	private IASTFunctionDefinition selectedDefinition;
	private CPPASTFunctionDeclarator selectedDeclaration;
	private final TextSelection selection;
	private IASTNode parentInsertionPoint;
	private ModificationCollector modifications;

	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
		this.selection = (TextSelection) selection;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		super.checkInitialConditions(pm);
		if (initStatus.hasFatalError()) {
			return initStatus;
		}
		//replace this with indexing
		memberdefinition = ToggleSelectionHelper.getSelectedDefinition(unit, selection);
		
		if (memberdefinition == null) {
			initStatus.addFatalError("could not get a definition");
		}
		else if (!(memberdefinition instanceof IASTFunctionDefinition)) {
			System.out.println("Is not a declaration abort");
			initStatus.addFatalError("Is not a function definition");
		}
		return initStatus;
	}
	
	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new EmptyRefactoringDescription();
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector modifications) throws CoreException {
		this.modifications = modifications;
		try {
			lockIndex();
			try {
				collectModificationsSafely();
			} finally {
				unlockIndex();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void collectModificationsSafely() {
		selectedDeclaration = ToggleSelectionHelper.getSelectedDeclaration(unit, selection);
		selectedDefinition  = ToggleSelectionHelper.getSelectedDefinition(unit, selection);
		if (!determinePosition())
			return;
		if (isInClassSituation())
			handleInClassSituation();
		else
			handleInHeaderSituation();
	}

	private boolean isInClassSituation() {
		return selectedDefinition.getDeclarator() == selectedDeclaration;
	}

	private void handleInHeaderSituation() {
		System.out.println("We're in the in-header situation.");
		IASTFunctionDefinition newfunc = getInClassDefinition();
		addDeclarationReplaceModification(newfunc);
	}

	private void handleInClassSituation() {
		System.out.println("We're in the in-class situation.");
		IASTFunctionDefinition newfunc = getInHeaderDefinition();
		addDefinitionAppendModification(newfunc);
	}

	private void addDeclarationReplaceModification(IASTFunctionDefinition definition) {
		ASTRewrite rewrite = modifications.rewriterForTranslationUnit(unit);
		TextEditGroup infoText = new TextEditGroup("Toggle");
		rewrite.remove(selectedDefinition, infoText);
		rewrite.replace(selectedDeclaration.getParent(), definition, infoText);
	}

	private void collectMoveChanges(ModificationCollector collector) {
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition();
		
		IASTDeclSpecifier newdeclspec = memberdefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = new CPPASTFunctionDeclarator(ToggleSelectionHelper.getQualifiedName(memberdefinition));
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

	private IASTFunctionDefinition getInClassDefinition() {
		IASTDeclSpecifier newdeclspec = selectedDefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(false);
		IASTFunctionDeclarator funcdecl = selectedDeclaration.copy();

		IASTStatement newbody = selectedDefinition.getBody().copy();
		IASTFunctionDefinition newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl, newbody);
		parentInsertionPoint = getParentInsertionPoint(selectedDeclaration, unit);
		newfunc.setParent(parentInsertionPoint);
		return newfunc;
	}

	public IASTNode getParentInsertionPoint(CPPASTFunctionDeclarator child, IASTTranslationUnit alternative) {
		IASTNode node = child;
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				ICPPASTCompositeTypeSpecifier type = (ICPPASTCompositeTypeSpecifier) node;
				System.out.println("Will insert copied function here: " + new String(type.getName().getSimpleID()));
				return type;
			}
		}
		return unit;
	}

	private boolean determinePosition() {
		if (selectedDeclaration == null || selectedDefinition == null) {
			System.out.println("declaration AND definition needed. Cannot toggle. Stopping.");
			return false;
		}
		return true;
	}

	private IASTFunctionDefinition getInHeaderDefinition() {
		IASTDeclSpecifier newdeclspec = selectedDefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = selectedDeclaration.copy();
		ICPPASTQualifiedName quali = ToggleSelectionHelper.getQualifiedName(selectedDefinition);
		funcdecl.setName(quali);
		for (IASTNode node : funcdecl.getChildren()) {
			if (node instanceof ICPPASTParameterDeclaration) {
				ICPPASTParameterDeclaration param = (ICPPASTParameterDeclaration) node;
				ICPPASTDeclarator d = param.getDeclarator();
				d.setInitializer(null);
			}
		}
		
		IASTStatement newbody = selectedDefinition.getBody().copy();
		IASTFunctionDefinition newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl, newbody);
		newfunc.setParent(unit);
		return newfunc;
	}

	private void addDefinitionAppendModification(IASTFunctionDefinition definition) {
		ASTRewrite rewrite = modifications.rewriterForTranslationUnit(unit);
		TextEditGroup infoText = new TextEditGroup("Toggle");
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);
		rewrite.replace(selectedDefinition, declaration, infoText);
		rewrite.insertBefore(unit, null, definition, infoText);
	}

	private IASTSimpleDeclaration createDeclarationFromDefinition(
			IASTFunctionDefinition memberdefinition) {
		IASTDeclarator declarator = memberdefinition.getDeclarator().copy();
		IASTDeclSpecifier specifier = memberdefinition.getDeclSpecifier().copy();
		IASTSimpleDeclaration result = new CPPASTSimpleDeclaration(specifier);
		result.addDeclarator(declarator);
		return result;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
	throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return super.checkFinalConditions(pm);
	}
}
