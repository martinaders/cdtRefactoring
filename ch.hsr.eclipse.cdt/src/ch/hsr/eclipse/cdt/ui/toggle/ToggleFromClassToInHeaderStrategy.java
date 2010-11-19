package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.core.parser.scanner.ILocationResolver;
import org.eclipse.cdt.internal.core.parser.scanner.LocationMap;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.NodeContainer;
import org.eclipse.cdt.internal.ui.refactoring.extractfunction.ExtractedFunctionConstructionHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext fcontext;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		this.fcontext = context;
	}

	public void run(ModificationCollector modifications) {
		CPPNodeFactory factory = new CPPNodeFactory();
		IASTFunctionDeclarator funcdecl = fcontext.getDefinition().getDeclarator().copy();
		ICPPASTDeclSpecifier spec = (ICPPASTDeclSpecifier) fcontext.getDefinition().getDeclSpecifier().copy();
		CPPASTSimpleDeclaration simpledec = (CPPASTSimpleDeclaration) factory.newSimpleDeclaration(spec);
		simpledec.addDeclarator(funcdecl);
		simpledec.setParent(fcontext.getDefinition().getParent());
		

		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(fcontext.getDefinitionUnit());
		rewriter.replace(fcontext.getDefinition(), simpledec, infoText);
//		rewriter.insertBefore(fcontext.getDefinitionUnit(), null, 
//				ToggleNodeHelper.getQualifiedNameDefinition(true, 
//						fcontext.getDefinition(), fcontext.getDeclaration(), 
//						fcontext.getDefinitionUnit()),infoText);

		IASTNode[] children = fcontext.getDefinition().getBody().getChildren(); // stmt, expr
		
		ASTRewrite re = rewriter.insertBefore(fcontext.getDefinitionUnit(), null, 
				fcontext.getDefinition().getDeclarator(), infoText);
		
		IASTFunctionDefinition newFunction = new CPPASTFunctionDefinition();
		newFunction.setParent(fcontext.getDefinitionUnit());
		ASTRewrite subRW = rewriter.insertBefore(fcontext.getDefinition(), fcontext.getDefinition().getDeclarator(), newFunction, infoText);
		
		List<IASTNode> list = findExtractableNodes().getNodesToWrite();
		ExtractedFunctionConstructionHelper helper = ExtractedFunctionConstructionHelper.createFor(list);

		ICPPASTFunctionDefinition newDef = (ICPPASTFunctionDefinition) ToggleNodeHelper.getQualifiedNameDefinition(true, fcontext.getDefinition(), fcontext.getDeclaration(), fcontext.getDefinitionUnit());
		IASTCompoundStatement compound = new CPPASTCompoundStatement();
		newDef.setBody(compound);
		for (IASTNode each : list) {
			subRW.insertBefore(compound, null, each, infoText);
		}
	}
	
	/** copied from ExtractFunctionRefactoring */
	private NodeContainer findExtractableNodes() {
		final NodeContainer container = new NodeContainer();
		fcontext.getDefinition().accept(new ASTVisitor() {
			{
				shouldVisitStatements = true;
				shouldVisitExpressions = true;
			}

			@Override
			public int visit(IASTStatement stmt) {
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
