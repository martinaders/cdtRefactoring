package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToImplementationStragegy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;

	public ToggleFromInHeaderToImplementationStragegy(ToggleRefactoringContext context) throws CModelException, CoreException {
		super(context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
		this.siblingfile_translation_unit = context.loadTUForSiblingFile();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(definition_unit);
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
