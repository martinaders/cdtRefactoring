package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.model.ext.SourceRange;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.util.EditorUtility;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl implements ToggleRefactoringStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;
	private final ToggleRefactoringContext context;
	private IASTFunctionDefinition selectedDefinition;
	private IASTTranslationUnit definition_unit;
	private TextEditGroup infoText = new TextEditGroup("Toggle function body placement");


	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) throws CModelException, CoreException {
		this.selectedDefinition = context.getDefinition();
		this.definition_unit = context.getDefinitionUnit();
		this.siblingfile_translation_unit = context.getTUForSiblingFile();
		this.context = context;
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite astrewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTSimpleDeclaration declaration = ToggleNodeHelper.createDeclarationFromDefinition(selectedDefinition);
		astrewriter.replace(selectedDefinition, declaration, infoText);
		
		// TODO: Decide what to do when no cpp file has been found
//		if (siblingfile_translation_unit == null) {
//			ICProject project = CoreModel.getDefault().create(context.getSelectionFile().getLocation()).getCProject();
//			
//			String nl = System.getProperty("line.separator", "\n");
//			String sep = System.getProperty("file.separator", "/");
//			String headerFile = definition_unit.getFilePath().substring(definition_unit.getFilePath().lastIndexOf(sep) + 1, definition_unit.getFilePath().length());
//			String filename = definition_unit.getFilePath().substring(definition_unit.getFilePath().lastIndexOf(sep) + 1, definition_unit.getFilePath().lastIndexOf('.')) + ".cpp";
//			String path = definition_unit.getFilePath().substring(definition_unit.getFilePath().indexOf(project.getPath().toString()), definition_unit.getFilePath().lastIndexOf(sep) + 1);
//
//			try {
//				String sourceText = "#include \"" + headerFile + "\"" + nl + nl + selectedDefinition.getRawSignature() + nl;
//				int lastSemicolon = sourceText.lastIndexOf(';');
//				if (lastSemicolon < 0)
//					lastSemicolon = sourceText.lastIndexOf('{') + 1;
//				if (lastSemicolon < 0)
//					lastSemicolon = 0;
//				CreateFileChange change = new CreateFileChange(filename, new Path(path + filename), sourceText, context.getSelectionFile().getCharset());
//				modifications.addFileChange(change);
//				
//				CEditor editor = (CEditor) EditorUtility.openInEditor(new Path(path + filename), null);
//				if (editor != null)
//					editor.setSelection(new SourceRange(lastSemicolon, 0), true);
//				else
//					System.err.println("Failed to open editor with " + path + filename);
//			} catch (CoreException e) {
//			}
//		} else {
			ASTRewrite otherrewrite = modifications
			.rewriterForTranslationUnit(siblingfile_translation_unit);
			
			otherrewrite.insertBefore(
					siblingfile_translation_unit.getTranslationUnit(), null,
					selectedDefinition.copy(), infoText);
//		}
	}

	
}
