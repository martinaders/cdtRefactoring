package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.core.dom.rewrite.commenthandler.ASTCommenter;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext fcontext;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		this.fcontext = context;
	}

	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(fcontext.getDefinitionUnit());
		CPPNodeFactory factory = new CPPNodeFactory();
		IASTFunctionDeclarator funcdecl = fcontext.getDefinition().getDeclarator().copy();
		ICPPASTDeclSpecifier spec = (ICPPASTDeclSpecifier) fcontext.getDefinition().getDeclSpecifier().copy();
		IASTSimpleDeclaration simpledec = factory.newSimpleDeclaration(spec);
		simpledec.addDeclarator(funcdecl);
		simpledec.setParent(fcontext.getDefinition().getParent());
		ASTCommenter.addCommentMapping(fcontext.getDefinition(), simpledec);
		rewriter.replace(fcontext.getDefinition(), simpledec, infoText);

		IASTNode newDefinition = ToggleNodeHelper.getQualifiedNameDefinition(true, 
				fcontext.getDefinition(), fcontext.getDeclaration(), 
				fcontext.getDefinitionUnit());
		ToggleNodeHelper.remapAllComments(fcontext.getDefinition(), newDefinition);

		rewriter.insertBefore(fcontext.getDefinitionUnit(), null, 
				newDefinition,infoText);
	}
}
