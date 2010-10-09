package ch.hsr.eclipse.cdt.ui.implement;

import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.ASTHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
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
public class NewImplementRefactoring extends CRefactoring {

	public NewImplementRefactoring(IFile file, ISelection selection,
			ICProject proj) {
		super(file, selection, null, proj);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new CRefactoringDescription("id1", "proj1", "desc1", "comment1", 0,
				new HashMap<String, String>()) {
			@Override
			public Refactoring createRefactoring(RefactoringStatus status)
					throws CoreException {
				return new NewImplementRefactoring(getFile(), getSelection(),
						getCProject());
			}
		};
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException,
			OperationCanceledException {
		try {
			lockIndex();
			try {
				collectAddChange(collector);
			} finally {
				unlockIndex();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void collectAddChange(ModificationCollector collector) {
		IASTSimpleDeclaration memberDeclaration = SelectionHelper
				.findFirstSelectedDeclaration(region, unit);
		IASTNode parent = memberDeclaration.getParent().getParent();

		IASTNode clazz = parent.getParent();

		 IASTFunctionDefinition func = new CPPASTFunctionDefinition();
		 func.setParent(null);
		 func.setDeclSpecifier(new CPPASTSimpleDeclSpecifier());
		 IASTName name = memberDeclaration.getDeclarators()[0].getName().copy();
		 func.setDeclarator(new CPPASTFunctionDeclarator(name));
		 func.setBody(new CPPASTCompoundStatement());

		//return type
		IASTNode returnvalue = ASTHelper.getDeclarationForNode(memberDeclaration);
		IASTDeclSpecifier value = ASTHelper.getDeclarationSpecifier(returnvalue).copy();
		func.setDeclSpecifier(value);
			
		ASTRewrite rewrite = collector.rewriterForTranslationUnit(parent
				.getTranslationUnit());
		TextEditGroup edit = new TextEditGroup("Toggle");
		rewrite.replace(memberDeclaration, func, edit);
		//rewrite.insertBefore(parent, null, func, edit);
	}

}
