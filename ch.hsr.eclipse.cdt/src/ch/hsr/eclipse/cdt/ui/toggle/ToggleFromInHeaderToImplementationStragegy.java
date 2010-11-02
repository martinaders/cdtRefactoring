package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToImplementationStragegy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;

	public ToggleFromInHeaderToImplementationStragegy(
			ICPPASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition,
			IASTTranslationUnit unit, IASTTranslationUnit other_translation_unit) {
		super(selectedDeclaration, selectedDefinition, unit);
		this.siblingfile_translation_unit = other_translation_unit;
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(unit);
		ASTRewrite otherrewrite = modifications
				.rewriterForTranslationUnit(siblingfile_translation_unit);

		IASTNode toremove = selectedDefinition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = selectedDefinition.getParent();
		rewriter.remove(toremove, infoText);
		otherrewrite.insertBefore(
				siblingfile_translation_unit.getTranslationUnit(), null,
				getQualifiedNameDefinition(false), infoText);
	}
}
