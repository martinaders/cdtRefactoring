package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.model.ISourceRange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.Path;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public abstract class ToggleRefactoringAbstractStrategy {

	protected IASTFunctionDeclarator selectedDeclaration;
	protected IASTFunctionDefinition selectedDefinition;
	protected IASTTranslationUnit definition_unit;
	protected TextEditGroup infoText;
	public Path shouldOpenFile = null;
	public ISourceRange sourceRangeToBeShown = null;

	public ToggleRefactoringAbstractStrategy(
			IASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition,
			IASTTranslationUnit definition_unit) {
		this.selectedDeclaration = selectedDeclaration;
		this.selectedDefinition = selectedDefinition;
		this.definition_unit = definition_unit;
		infoText = new TextEditGroup("Toggle function body placement");
	}

	public abstract void run(ModificationCollector modifications);

	protected IASTNode getQualifiedNameDefinition(boolean inline) {
		ICPPASTDeclSpecifier newdeclspec = (ICPPASTDeclSpecifier) selectedDefinition
				.getDeclSpecifier().copy();
		newdeclspec.setVirtual(false);
		newdeclspec.setInline(inline);
		// was: declaration
		IASTFunctionDeclarator funcdecl = selectedDefinition.getDeclarator()
				.copy();

		// TODO: rethink correctness of this statement
		if (selectedDeclaration != null)
			funcdecl.setName(ToggleSelectionHelper
					.getQualifiedName(selectedDeclaration));
		else
			funcdecl.setName(ToggleSelectionHelper
					.getQualifiedName(selectedDefinition.getDeclarator()));
		ToggleNodeHelper.removeParameterInitializations(funcdecl);

		ICPPASTFunctionDefinition newfunc = ToggleNodeHelper.assembleFunctionDefinitionWithBody(
				newdeclspec, funcdecl, selectedDefinition);

		// was: declaration
		ICPPASTTemplateDeclaration templdecl = ToggleNodeHelper
				.getTemplateDeclaration(selectedDefinition);
		if (templdecl != null) {
			templdecl.setDeclaration(newfunc);
			templdecl.setParent(definition_unit);
			return templdecl;
		} else {
			newfunc.setParent(definition_unit);
			return newfunc;
		}
	}
}
