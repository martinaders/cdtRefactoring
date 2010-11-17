package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl implements ToggleRefactoringStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;
	private IASTFunctionDefinition selectedDefinition;
	private IASTTranslationUnit definition_unit;
	private TextEditGroup infoText = new TextEditGroup("Toggle function body placement");


	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) throws CModelException, CoreException {
		this.selectedDefinition = context.getDefinition();
		this.definition_unit = context.getDefinitionUnit();
		this.siblingfile_translation_unit = context.getTUForSiblingFile();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite astrewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTSimpleDeclaration declaration = ToggleNodeHelper.createDeclarationFromDefinition(selectedDefinition);
		astrewriter.replace(selectedDefinition, declaration, infoText);
		
		ASTRewrite otherrewrite = modifications
		.rewriterForTranslationUnit(siblingfile_translation_unit);
		
		otherrewrite.insertBefore(
				siblingfile_translation_unit.getTranslationUnit(), null,
				selectedDefinition.copy(), infoText);
	}

	
}
