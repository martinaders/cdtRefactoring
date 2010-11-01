package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy extends
		ToggleRefactoringAbstractStrategy {

	public ToggleFromClassToInHeaderStrategy(
			ICPPASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition, IASTTranslationUnit unit) {
		super(selectedDeclaration, selectedDefinition, unit);
	}

	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(unit);
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);

		rewriter.replace(selectedDefinition, declaration, infoText);
		rewriter.insertBefore(unit, null, getQualifiedNameDefinition(true),
				infoText);
	}
}
