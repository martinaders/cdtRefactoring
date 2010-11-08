package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy extends
		ToggleRefactoringAbstractStrategy {

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
	}

	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);

		
		rewriter.insertBefore(selectedDefinition.getParent(),null, declaration, infoText);
		rewriter.remove(selectedDefinition, infoText);
		rewriter.insertBefore(definition_unit, null, getQualifiedNameDefinition(true),
				infoText);
	}

	@Override
	protected void removeNewlines(CompositeChange finalChange) {
		CompositeChange cc = (CompositeChange) finalChange.getChildren()[0];
		TextEdit edit = ((TextChange) cc.getChildren()[0]).getEdit();
		
		InsertEdit insEdit = (InsertEdit) edit.getChildren()[1];
		insEdit = new InsertEdit(insEdit.getOffset(), insEdit.getText().substring(0, insEdit.getText().length() - 2));
		edit.removeChild(1);
		edit.addChild(insEdit);
	}
}
