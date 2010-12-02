package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl implements ToggleRefactoringStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;
	private IASTFunctionDefinition selectedDefinition;
	private IASTTranslationUnit definition_unit;
	private TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private final ToggleRefactoringContext fcontext;

	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) {
		this.fcontext = context;
		if (isScopedFreeFunction())
			throw new NotSupportedException("namespaced+templated free functions not supported yet");
		IASTTranslationUnit siblingTU = null;
		siblingTU = context.getTUForSiblingFile();
		if (siblingTU == null)
			throw new NotSupportedException("Cannot decide where to put the code");
		this.selectedDefinition = context.getDefinition();
		this.definition_unit = context.getDefinitionUnit();
		this.siblingfile_translation_unit = siblingTU;
	}
	
	private boolean isScopedFreeFunction() {
		return isNamespacedOrTemplated(fcontext.getDefinition().getDeclarator(), fcontext.getDeclaration());
	}
	
	private boolean isNamespacedOrTemplated(IASTFunctionDeclarator declarator, IASTFunctionDeclarator backup) {
		if (declarator.getName() instanceof ICPPASTQualifiedName)
			declarator = backup;
		IASTNode node = declarator;
		while (node != null) {
			if (node instanceof ICPPASTNamespaceDefinition
					|| node instanceof ICPPASTTemplateDeclaration)
				return true;
			node = node.getParent();
		}
		return false;
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
