package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToClassStragegy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit declaration_unit;

	public ToggleFromImplementationToClassStragegy(
			ICPPASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition,
			IASTTranslationUnit definition_unit, IASTTranslationUnit declaration_unit) {
		super(selectedDeclaration, selectedDefinition, definition_unit);
		this.declaration_unit = declaration_unit;
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite implast = modifications
				.rewriterForTranslationUnit(definition_unit);
		ASTRewrite headerast = modifications
				.rewriterForTranslationUnit(declaration_unit);

		implast.remove(selectedDefinition, infoText);
		headerast.replace(selectedDeclaration.getParent(),getInClassDefinition(selectedDefinition, selectedDeclaration, declaration_unit),infoText);
	}
}
