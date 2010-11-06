package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.CCompositeChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.refactoring.CTextFileChange;
import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy extends
		ToggleRefactoringAbstractStrategy {

	private final IFile file;

	public ToggleFromClassToInHeaderStrategy(
			ICPPASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition, IASTTranslationUnit unit, IFile file) {
		super(selectedDeclaration, selectedDefinition, unit);
		this.file = file;
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
		TextFileChange tx = new CTextFileChange("", file);
		tx.setEdit(new DeleteEdit(newlinePosition, 2));
		finalChange.add(tx);
	}
}
