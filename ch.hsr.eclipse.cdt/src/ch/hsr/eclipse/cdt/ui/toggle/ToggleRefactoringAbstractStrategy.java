package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
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
}
