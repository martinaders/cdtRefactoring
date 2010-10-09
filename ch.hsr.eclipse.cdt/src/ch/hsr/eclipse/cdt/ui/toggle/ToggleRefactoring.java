package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.ASTHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

import ch.hsr.eclipse.cdt.ui.NullRefactoring;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {

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
				return new NullRefactoring(getFile(), getSelection(),
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
		IASTSimpleDeclaration memberDeclaration = SelectionHelper
				.findFirstSelectedDeclaration(region, unit);
		IASTNode parent = memberDeclaration.getParent().getParent();
		
		IASTNode clazz = parent.getParent();
		
//		System.out.println(parent.getRawSignature());
//		System.out.println(parent.getParent().getRawSignature());
//		IASTFunctionDefinition func = new CPPASTFunctionDefinition();
//		func.setParent(null);
//		func.setDeclSpecifier(new CPPASTSimpleDeclSpecifier());
//		func.setDeclarator(new CPPASTFunctionDeclarator(new CPPASTName("asdfasdf".toCharArray())));
//		func.setBody(new CPPASTCompoundStatement());

		//function name
		IASTSimpleDeclaration func = new CPPASTSimpleDeclaration();
		func.setParent(clazz);
		
		IASTName name = memberDeclaration.getDeclarators()[0].getName().copy();
		IASTStandardFunctionDeclarator declaration = new CPPASTFunctionDeclarator(name);
		declaration.setParent(func);
		func.addDeclarator(declaration);
		
		//inline --TODO:
//		IASTSimpleDeclSpecifier inline = new CPPASTSimpleDeclSpecifier();
//		inline.setInline(true);
//		inline.setParent(declaration);
//		func.setDeclSpecifier(inline);
		
		//return type
		IASTNode returnvalue = ASTHelper.getDeclarationForNode(memberDeclaration);
		IASTDeclSpecifier value = ASTHelper.getDeclarationSpecifier(returnvalue).copy();
		func.setDeclSpecifier(value);
		
		ASTRewrite rewrite = collector.rewriterForTranslationUnit(parent.getTranslationUnit());
		TextEditGroup edit = new TextEditGroup("Toggle");
		System.out.println("class: " + clazz.getRawSignature().toString());
		rewrite.insertBefore(clazz, null, func, edit);
		rewrite.remove(memberDeclaration, edit);
		System.out.println("djfjd lk ajsdkfl jaösfdlasddlkas fdalksd faslkdf öalsd kjfafjasldkfalaskdf");
	}

}
