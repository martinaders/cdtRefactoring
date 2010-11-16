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

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy extends
		ToggleRefactoringAbstractStrategy {

	public ToggleFromInHeaderToClassStrategy(ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
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

		ICPPASTFunctionDefinition newfunc = assembleFunctionDefinitionWithBody(
				newDeclSpec, newDeclaration);

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
