package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToClassStrategy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit declaration_unit;

	public ToggleFromImplementationToClassStrategy(
			IASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition,
			IASTTranslationUnit definition_unit, IASTTranslationUnit declaration_unit) {
		super(selectedDeclaration, selectedDefinition, definition_unit);
		this.declaration_unit = declaration_unit;
	}

	public ToggleFromImplementationToClassStrategy(
			ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
		this.declaration_unit = context.getDeclarationUnit();
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
	
	@Override
	protected void removeNewlines(CompositeChange finalChange) {
		CompositeChange file1 = (CompositeChange) finalChange.getChildren()[0];
		CompositeChange file2 = (CompositeChange) finalChange.getChildren()[1];
		TextEdit edit1 = ((TextChange) file1.getChildren()[0]).getEdit();
		TextEdit edit2 = ((TextChange) file2.getChildren()[0]).getEdit();

		// Needed, because the order of the edits may change (usually when
		// toggling very fast).
		boolean variation = ((ReplaceEdit) edit2.getChildren()[0]).getText().length() == 0;

		ReplaceEdit removEdit = null;
		ReplaceEdit repEdit = null;
		if (variation) {
			removEdit = (ReplaceEdit) edit2.getChildren()[0];
			repEdit = (ReplaceEdit) edit1.getChildren()[0];
		} else {
			removEdit = (ReplaceEdit) edit1.getChildren()[0];
			repEdit = (ReplaceEdit) edit2.getChildren()[0];
		}

		String before = repEdit.getText().substring(0, repEdit.getText().lastIndexOf("\n"));
		String after = repEdit.getText().substring(repEdit.getText().lastIndexOf("\n") + 1, repEdit.getText().length());
		repEdit = new ReplaceEdit(repEdit.getOffset(), repEdit.getLength(), before.concat(after));

		// must be +1 to leave a newline at end of file
		removEdit = new ReplaceEdit(removEdit.getOffset() - 1, removEdit.getLength() + 1, "");

		edit1.removeChild(0);
		edit2.removeChild(0);
		if (variation) {
			edit1.addChild(repEdit);
			edit2.addChild(removEdit);
		} else {
			edit2.addChild(repEdit);
			edit1.addChild(removEdit);			
		}
	}
}
