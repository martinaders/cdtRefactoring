package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.refactoring.CTextFileChange;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy extends
		ToggleRefactoringAbstractStrategy {

	public ToggleFromInHeaderToClassStrategy(ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTNode toremove = selectedDefinition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = selectedDefinition.getParent();

		rewriter.remove(toremove, infoText);
		rewriter.replace(selectedDeclaration.getParent(),
				getInClassDefinition(selectedDefinition, selectedDeclaration, definition_unit),
				infoText);
	}
	
	@Override
	protected void removeNewlines(CompositeChange finalChange) {
		CompositeChange cc = (CompositeChange) finalChange.getChildren()[0];
		TextEdit edit = ((CTextFileChange) cc.getChildren()[0]).getEdit();
		
		ReplaceEdit repEdit = (ReplaceEdit) edit.getChildren()[0];
		ReplaceEdit remEdit = (ReplaceEdit) edit.getChildren()[1];
		
		String before = repEdit.getText().substring(0, repEdit.getText().lastIndexOf("\n\n"));
		String after = repEdit.getText().substring(repEdit.getText().lastIndexOf("\n\n") + 1, repEdit.getText().length());
		repEdit = new ReplaceEdit(repEdit.getOffset(), repEdit.getLength(), before.concat(after));
		remEdit = new ReplaceEdit(remEdit.getOffset() - 1, remEdit.getLength() + 2, "");
		edit.removeChildren();
		edit.addChild(repEdit);
		edit.addChild(remEdit);
	}
}
