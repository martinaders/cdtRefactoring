package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;

	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) throws CModelException, CoreException {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
		this.siblingfile_translation_unit = context.loadTUForSiblingFile();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite astrewriter = modifications.rewriterForTranslationUnit(definition_unit);
		ASTRewrite otherrewrite = modifications
		.rewriterForTranslationUnit(siblingfile_translation_unit);
		
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);
		
		astrewriter.replace(selectedDeclaration.getParent(), declaration, infoText);
		otherrewrite.insertBefore(
				siblingfile_translation_unit.getTranslationUnit(), null,
				selectedDefinition.copy(), infoText);
	}

	
}
