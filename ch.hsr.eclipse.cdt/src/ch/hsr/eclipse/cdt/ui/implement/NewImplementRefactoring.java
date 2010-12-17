package ch.hsr.eclipse.cdt.ui.implement;

import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeConstructorExpression;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTConstructorInitializer;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTReturnStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleTypeConstructorExpression;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.ASTHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

import ch.hsr.eclipse.cdt.ui.toggle.DeclaratorFinder;
import ch.hsr.eclipse.cdt.ui.toggle.ToggleNodeHelper;

@SuppressWarnings("restriction")
public class NewImplementRefactoring extends CRefactoring {

	private ISelection selection;

	public NewImplementRefactoring(IFile file, ISelection selection,
			ICProject proj) {
		super(file, selection, null, proj);
		this.selection = selection;
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new CRefactoringDescription("id1", "proj1", "desc1", "comment1",
				0, new HashMap<String, String>()) {
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
		collectAddChange(collector);
	}

	private void collectAddChange(ModificationCollector collector) {
		IASTName declaratorName = new DeclaratorFinder((TextSelection) selection, unit).getName();
		CPPASTSimpleDeclaration declaration = ToggleNodeHelper.getAncestorOfType(
				declaratorName, IASTSimpleDeclaration.class);
		
		IASTNode parent = getParent(declaration);
		
		IASTFunctionDefinition func = new CPPASTFunctionDefinition();
		func.setParent(null);
		func.setDeclSpecifier(new CPPASTSimpleDeclSpecifier());
		IASTName name = declaration.getDeclarators()[0].getName().copy();
		func.setDeclarator(new CPPASTFunctionDeclarator(name));

		IASTNode returnvalue = ASTHelper.getDeclarationForNode(declaration);
		ICPPASTDeclSpecifier declSpec = (ICPPASTDeclSpecifier) ASTHelper.getDeclarationSpecifier(returnvalue).copy();
		func.setDeclSpecifier(declSpec);
		ICPPASTDeclSpecifier returndeclspec = declSpec.copy();
		returndeclspec.setStorageClass(ICPPASTDeclSpecifier.sc_unspecified);
		
		CPPASTReturnStatement returnstmt = new CPPASTReturnStatement();
		ICPPASTSimpleTypeConstructorExpression returntype = new CPPASTSimpleTypeConstructorExpression(returndeclspec, new CPPASTConstructorInitializer());
		returnstmt.setReturnValue(returntype);
		returnstmt.setParent(func);
		
		CPPASTCompoundStatement body = new CPPASTCompoundStatement();
		body.addStatement(returnstmt);
		func.setBody(body);
		func.setParent(parent);
		
		ASTRewrite rewrite = null;
		if (parent != null) {
			rewrite = collector.rewriterForTranslationUnit(parent.getTranslationUnit());
		}
		else {
			rewrite = collector.rewriterForTranslationUnit(unit);
		}
		TextEditGroup edit = new TextEditGroup("Toggle");
		rewrite.replace(declaration, func, edit);
		if (parent instanceof ICPPASTCompositeTypeSpecifier)
			restoreLeadingComments(declaration, func, rewrite, edit);
	}

	private IASTNode getParent(IASTSimpleDeclaration declaration) {
		ICPPASTCompositeTypeSpecifier klass = ToggleNodeHelper.getAncestorOfType(declaration, ICPPASTCompositeTypeSpecifier.class);
		if (klass != null) {
			return klass;
		}
		else {
			return unit;
		}
	}

	private void restoreLeadingComments(
			IASTSimpleDeclaration oldDeclaration,
			IASTFunctionDefinition func, ASTRewrite rewrite, TextEditGroup edit) {
		String leadingcomment = ToggleNodeHelper.getLeadingComments(oldDeclaration, unit);
		System.out.println("leadingcomment: " + leadingcomment);
		String declSpec = func.getDeclSpecifier().toString();
		rewrite.replace(func.getDeclSpecifier(), new ASTLiteralNode(leadingcomment + declSpec), edit);
	}
}
