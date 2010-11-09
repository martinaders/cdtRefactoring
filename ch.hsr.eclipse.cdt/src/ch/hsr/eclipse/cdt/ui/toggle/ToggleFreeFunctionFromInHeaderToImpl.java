package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.model.ext.SourceRange;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;
	private final ToggleRefactoringContext context;

	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) throws CModelException, CoreException {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
		this.siblingfile_translation_unit = context.loadTUForSiblingFile();
		this.context = context;
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite astrewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);
		astrewriter.replace(selectedDeclaration.getParent(), declaration, infoText);
		
		if (siblingfile_translation_unit == null) {
			ICProject project = CoreModel.getDefault().create(context.getFile().getLocation()).getCProject();
			
			String nl = System.getProperty("line.separator", "\n");
			String sep = System.getProperty("file.separator", "/");
			String headerFile = definition_unit.getFilePath().substring(definition_unit.getFilePath().lastIndexOf(sep) + 1, definition_unit.getFilePath().length());
			String filename = definition_unit.getFilePath().substring(definition_unit.getFilePath().lastIndexOf(sep) + 1, definition_unit.getFilePath().lastIndexOf('.')) + ".cpp";
			String path = definition_unit.getFilePath().substring(definition_unit.getFilePath().indexOf(project.getPath().toString()), definition_unit.getFilePath().lastIndexOf(sep) + 1);

			try {
				String sourceText = "#include \"" + headerFile + "\"" + nl + nl + selectedDefinition.getRawSignature() + nl;
				int lastSemicolon = sourceText.lastIndexOf(';');
				if (lastSemicolon < 0)
					lastSemicolon = sourceText.lastIndexOf('{') + 1;
				if (lastSemicolon < 0)
					lastSemicolon = 0;
				sourceRangeToBeShown = new SourceRange(lastSemicolon, 0);
				CreateFileChange change = new CreateFileChange(filename, new Path(path + filename), sourceText, context.getFile().getCharset());
				modifications.addFileChange(change);
				shouldOpenFile = new Path(path + filename);
			} catch (CoreException e) {
			}
		} else {
			ASTRewrite otherrewrite = modifications
			.rewriterForTranslationUnit(siblingfile_translation_unit);
			
			otherrewrite.insertBefore(
					siblingfile_translation_unit.getTranslationUnit(), null,
					selectedDefinition.copy(), infoText);
		}
	}

	@Override
	protected void removeNewlines(CompositeChange finalChange) {
		CompositeChange file1 = (CompositeChange) finalChange.getChildren()[0];
		CompositeChange file2 = (CompositeChange) finalChange.getChildren()[1];
		TextEdit edit1 = ((TextChange) file1.getChildren()[0]).getEdit();
		TextEdit edit2 = ((TextChange) file2.getChildren()[0]).getEdit();
		
		boolean variation = edit2.getChildren()[0] instanceof ReplaceEdit;

		ReplaceEdit repEdit = null;
		InsertEdit insEdit = null;
		if (variation) {
			repEdit = (ReplaceEdit) edit2.getChildren()[0];
			insEdit = (InsertEdit) edit1.getChildren()[0];
		} else {
			repEdit = (ReplaceEdit) edit1.getChildren()[0];
			insEdit = (InsertEdit) edit2.getChildren()[0];
		}

		insEdit = new InsertEdit(insEdit.getOffset(), insEdit.getText().substring(0, insEdit.getText().length() - 2));
		repEdit = new ReplaceEdit(repEdit.getOffset(), repEdit.getLength(), repEdit.getText().substring(0, repEdit.getText().length() - 1));

		edit1.removeChild(0);
		edit2.removeChild(0);
		if (variation) {
			edit1.addChild(insEdit);
			edit2.addChild(repEdit);
		} else {
			edit2.addChild(insEdit);
			edit1.addChild(repEdit);			
		}
	}
}
