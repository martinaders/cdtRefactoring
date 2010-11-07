package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.CCompositeChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.refactoring.CTextFileChange;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy extends
		ToggleRefactoringAbstractStrategy {

	private final ToggleRefactoringContext context;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
		this.context = context;
	}

	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);

		rewriter.replace(selectedDefinition, declaration, infoText);
		rewriter.insertBefore(definition_unit, null, getQualifiedNameDefinition(true),
				infoText);
	}
	
	@Override
	protected void removeTrailingNewlines(CCompositeChange finalChange) {
		CompositeChange cc = (CompositeChange) finalChange.getChildren()[0];
		TextEdit edit = ((CTextFileChange) cc.getChildren()[0]).getEdit();
		ReplaceEdit rEdit = (ReplaceEdit) edit.getChildren()[0];
		InsertEdit iEdit = (InsertEdit) edit.getChildren()[1];
		// assert (replace is above the insert)
		int newlinePosition = iEdit.getOffset() + iEdit.getText().length() - rEdit.getLength() + rEdit.getText().length() - 2;
		TextFileChange tx = new CTextFileChange("", context.getFile());
		tx.setEdit(new DeleteEdit(newlinePosition, 2));
		finalChange.add(tx);
	}
}
