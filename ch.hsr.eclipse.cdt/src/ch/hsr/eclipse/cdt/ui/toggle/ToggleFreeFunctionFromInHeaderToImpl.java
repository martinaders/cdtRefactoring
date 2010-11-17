package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl implements ToggleRefactoringStrategy {

	private ToggleRefactoringContext context;
	private IASTTranslationUnit fsiblingfile_translation_unit;
	private TextEditGroup infoText = new TextEditGroup("Toggle function body placement");

	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) throws CModelException, CoreException {
		this.context = context;
		this.fsiblingfile_translation_unit = context.getTUForSiblingFile();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite astrewriter = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		IASTSimpleDeclaration declaration = ToggleNodeHelper.createDeclarationFromDefinition(context.getDefinition());
		astrewriter.replace(context.getDefinition(), declaration, infoText);
		
//		if (fsiblingfile_translation_unit == null) {
//			ICProject project = CoreModel.getDefault().create(context.getSelectionFile().getLocation()).getCProject();
//			
//			String nl = System.getProperty("line.separator", "\n");
//			String sep = System.getProperty("file.separator", "/");
//			String headerFile = context.getDefinitionUnit().getFilePath().substring(context.getDefinitionUnit().getFilePath().lastIndexOf(sep) + 1, context.getDefinitionUnit().getFilePath().length());
//			String filename = context.getDefinitionUnit().getFilePath().substring(context.getDefinitionUnit().getFilePath().lastIndexOf(sep) + 1, context.getDefinitionUnit().getFilePath().lastIndexOf('.')) + ".cpp";
//			String path = context.getDefinitionUnit().getFilePath().substring(context.getDefinitionUnit().getFilePath().indexOf(project.getPath().toString()), context.getDefinitionUnit().getFilePath().lastIndexOf(sep) + 1);
//
//			try {
//				String sourceText = "#include \"" + headerFile + "\"" + nl + nl + context.getDefinition().getRawSignature() + nl;
//				int lastSemicolon = sourceText.lastIndexOf(';');
//				if (lastSemicolon < 0)
//					lastSemicolon = sourceText.lastIndexOf('{') + 1;
//				if (lastSemicolon < 0)
//					lastSemicolon = 0;
//				CreateFileChange change = new CreateFileChange(filename, new Path(path + filename), sourceText, context.getSelectionFile().getCharset());
//				modifications.addFileChange(change);
//				
//				CEditor editor = (CEditor) EditorUtility.openInEditor(new Path(path + filename), null);
//				editor.setSelection(new SourceRange(lastSemicolon, 0), true);
//			} catch (CoreException e) {
//			}
//		} else {
			ASTRewrite otherrewrite = modifications
			.rewriterForTranslationUnit(fsiblingfile_translation_unit);
			
			otherrewrite.insertBefore(
					fsiblingfile_translation_unit.getTranslationUnit(), null,
					context.getDefinition().copy(), infoText);
		}
//	}
}
