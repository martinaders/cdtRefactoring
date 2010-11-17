package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy implements ToggleRefactoringStrategy {

	protected IASTFunctionDeclarator selectedDeclaration;
	protected IASTFunctionDefinition selectedDefinition;
	protected IASTTranslationUnit definition_unit;
	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");

	public ToggleFromInHeaderToClassStrategy(ToggleRefactoringContext context) {
		this.selectedDeclaration = context.getDeclaration();
		this.selectedDefinition = context.getDefinition();
		this.definition_unit = context.getDefinitionUnit();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTNode toremove = selectedDefinition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = selectedDefinition.getParent();

		rewriter.remove(toremove, infoText);
		// TODO: use deFINItion?
		rewriter.replace(selectedDeclaration.getParent(),
				getInClassDefinition(selectedDefinition, selectedDeclaration, definition_unit),
				infoText);
	}
	
	private IASTFunctionDefinition getInClassDefinition(
			IASTFunctionDefinition definition,
			IASTFunctionDeclarator declaration, IASTTranslationUnit unit) {
		IASTDeclSpecifier newDeclSpec = definition.getDeclSpecifier().copy();
		newDeclSpec.setInline(false);
		IASTFunctionDeclarator newDeclaration = declaration.copy();

		ICPPASTFunctionDefinition newfunc = ToggleNodeHelper.assembleFunctionDefinitionWithBody(
				newDeclSpec, newDeclaration, selectedDefinition);

		newfunc.setParent(getParentInsertionPoint(definition, unit));
		return newfunc;
	}
	
	private IASTNode getParentInsertionPoint(IASTNode node,
			IASTTranslationUnit alternative) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				return (ICPPASTCompositeTypeSpecifier) node;
			}
		}
		return definition_unit;
	}
}
