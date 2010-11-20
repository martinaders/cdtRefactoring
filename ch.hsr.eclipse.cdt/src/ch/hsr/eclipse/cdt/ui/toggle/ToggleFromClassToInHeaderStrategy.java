package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.NodeContainer;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext fcontext;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		this.fcontext = context;
	}

	/*
	 * Written with a close look at ExtractFunctionRefactoring.addMethod and
	 * ExtractFunctionRefactoring.findExtractableNodes. When copying every
	 * expression/statement separately, comments inside the moved function body
	 * will be preserved by the rewriter.
	 */
	public void run(ModificationCollector modifications) {
		CPPNodeFactory factory = new CPPNodeFactory();
		IASTFunctionDeclarator funcdecl = fcontext.getDefinition().getDeclarator().copy();
		ICPPASTDeclSpecifier spec = (ICPPASTDeclSpecifier) fcontext.getDefinition().getDeclSpecifier().copy();
		CPPASTSimpleDeclaration simpledec = (CPPASTSimpleDeclaration) factory.newSimpleDeclaration(spec);
		simpledec.addDeclarator(funcdecl);
		simpledec.setParent(fcontext.getDefinition().getParent());
		
		// TODO: Support comments above the declaration
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(fcontext.getDefinitionUnit());
		rewriter.replace(fcontext.getDefinition(), simpledec, infoText);

		// TODO: Support ICPPASTTemplateDeclaration (will throw ClassCastEx now)
		IASTFunctionDefinition synth = (IASTFunctionDefinition) ToggleNodeHelper.getQualifiedNameDefinition(true,fcontext.getDefinition(), fcontext.getDeclaration(), fcontext.getDefinitionUnit());
		IASTFunctionDefinition newFunction = new CPPASTFunctionDefinition();
		newFunction.setParent(fcontext.getDefinitionUnit());
		newFunction.setDeclSpecifier(synth.getDeclSpecifier());
		newFunction.setDeclarator(synth.getDeclarator());
		IASTCompoundStatement compound = new CPPASTCompoundStatement();
		newFunction.setBody(compound);
		ASTRewrite subRW = rewriter.insertBefore(fcontext.getDefinitionUnit(), null, newFunction, infoText);

		List<IASTNode> list = findExtractableNodes().getNodesToWrite();
		for (IASTNode each : list) {
			subRW.insertBefore(compound, null, each, infoText);
		}
	}

	private NodeContainer findExtractableNodes() {
		final NodeContainer container = new NodeContainer();
		fcontext.getDefinition().accept(new ASTVisitor() {
			{
				shouldVisitStatements = true;
				shouldVisitExpressions = true;
			}
			boolean isCompoundStatementAlreadySkipped = false;

			@Override
			public int visit(IASTStatement stmt) {
				if (!isCompoundStatementAlreadySkipped) {
					isCompoundStatementAlreadySkipped = true;
					return PROCESS_CONTINUE;
				}
				container.add(stmt);
				return PROCESS_SKIP;
			}

			@Override
			public int visit(IASTExpression expression) {
				container.add(expression);
				return PROCESS_SKIP;
			}
		});
		return container;
	}
}
