package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.internal.UIPlugin;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToImplementationStrategy implements ToggleRefactoringStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;
	private String path;
	private String filename;
	private ToggleRefactoringContext context;
	private String origin_filename;
	private boolean newfile;
	protected IASTFunctionDeclarator selectedDeclaration;
	protected IASTFunctionDefinition selectedDefinition;
	protected IASTTranslationUnit definition_unit;
	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");

	public ToggleFromInHeaderToImplementationStrategy(ToggleRefactoringContext context) throws CModelException, CoreException {
		this.selectedDeclaration = context.getDeclaration();
		this.selectedDefinition = context.getDefinition();
		this.definition_unit = context.getDeclarationUnit();
		this.context = context;
		this.siblingfile_translation_unit = context.getTUForSiblingFile();
		if (this.siblingfile_translation_unit == null) {
			path = context.getSelectionFile().getFullPath().toString();
			filename = ToggleSelectionHelper.getFilenameWithoutExtension(path);
			path = path.replaceAll("(\\w)*\\.(\\w)*", "");
			if (context.getSelectionFile().getFileExtension().equals("h")) {
				origin_filename = filename + ".h";
				filename += ".cpp";
			}
			if (context.getSelectionFile().getFileExtension().equals("cpp")) {
				origin_filename = filename + ".cpp";
				filename += ".h";
			}
			Shell shell = UIPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getShell();
			newfile = MessageDialog.openQuestion(shell, "Create new implementation file?", "Create a new file named: " + filename + " and move " + context.getDeclaration().getRawSignature() + " there?");
		}
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTNode toremove = selectedDefinition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = selectedDefinition.getParent();
		rewriter.remove(toremove, infoText);
		
		
		if (this.siblingfile_translation_unit == null && newfile) {
			IASTFunctionDefinition func = selectedDefinition.copy();
			IASTDeclSpecifier spec = new CPPASTSimpleDeclSpecifier();
			spec.setInline(false);
			func.setDeclSpecifier(spec);
			func.setParent(selectedDefinition.getParent());
			String declaration = func.getRawSignature();
			declaration = declaration.replaceAll("inline ", "");
			CreateFileChange change;
			try {
				change = new CreateFileChange(filename, new
				Path(path+filename), "#include " + "\"" + origin_filename + "\"\n\n" + declaration, context.getSelectionFile().getCharset());
				modifications.addFileChange(change);			
			} catch (CoreException e) {
				e.printStackTrace();
			}
		} else {
			ASTRewrite otherrewrite = modifications
			.rewriterForTranslationUnit(siblingfile_translation_unit);
			otherrewrite.insertBefore(siblingfile_translation_unit.getTranslationUnit(), null,
					ToggleNodeHelper.getQualifiedNameDefinition(false, selectedDefinition, selectedDeclaration, definition_unit), infoText);
			//TODO: maybe not using qualified name because we already have it...
		}
	}
	
	
}
