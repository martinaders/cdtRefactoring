package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
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

	private IASTTranslationUnit siblingtu;
	private ToggleRefactoringContext context;
	private TextEditGroup infoText;
	private boolean newfile;

	public ToggleFromInHeaderToImplementationStrategy(ToggleRefactoringContext context) throws CModelException, CoreException {
		this.infoText = new TextEditGroup("Toggle function body placement");
		this.context = context;
		this.siblingtu = context.getTUForSiblingFile();
		if (this.siblingtu == null) {
			Shell shell = UIPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getShell();
			newfile = MessageDialog.openQuestion(shell, "Create new implementation file?", "Create a new file named: " + getNewFileName() + " and move " + context.getDeclaration().getRawSignature() + " there?");
		}
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		rewriter.remove(ToggleNodeHelper.getParentRemovePoint(context.getDefinition()), infoText);
		
		IASTFunctionDefinition newImpldef = copyDefinitionFromInHeader();
		if (this.siblingtu != null) {
			ASTRewrite otherrewrite = modifications.rewriterForTranslationUnit(siblingtu);
			otherrewrite.insertBefore(siblingtu.getTranslationUnit(), null, newImpldef, infoText);
			return;
		}
		if (newfile) {
			try {
				//set a parent or will not be able to print the signature
				newImpldef.setParent(context.getDeclarationUnit());
				CreateFileChange change = createNewImplementationFile(
						getNewFileContent(newImpldef.getRawSignature()), getNewFileName());
				modifications.addFileChange(change);			
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String getNewFileName() {
		return ToggleSelectionHelper.getFilenameWithoutExtension(
				context.getSelectionFile().getFullPath().toString()) + ".cpp";
	}
	
	private IASTFunctionDefinition copyDefinitionFromInHeader() {
		IASTFunctionDefinition newImpldef = context.getDefinition().copy();
		newImpldef.getDeclSpecifier().setInline(false);
		return newImpldef;
	}

	private String getNewFileContent(String definition) {
		String originfile = ToggleSelectionHelper.getFilenameWithoutExtension(
				context.getSelectionFile().getFullPath().toString()) + ".h";
		return "#include " + "\"" + originfile + "\"\n\n" + definition;
	}
	
	private CreateFileChange createNewImplementationFile(String content, String filename)
			throws CoreException {
		CreateFileChange change;
		change = new CreateFileChange(filename, new	Path(getPath()+filename), 
				content, context.getSelectionFile().getCharset());
		return change;
	}

	private String getPath() {
		String result = context.getSelectionFile().getFullPath().toString();
		return result.replaceAll("(\\w)*\\.(\\w)*", "");
	}
}
