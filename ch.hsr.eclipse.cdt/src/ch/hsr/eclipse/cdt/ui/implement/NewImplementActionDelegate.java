package ch.hsr.eclipse.cdt.ui.implement;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

import ch.hsr.eclipse.cdt.ui.implement.SilentWizard;

public class NewImplementActionDelegate implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	private TextSelection selection;
	private TextEditor editor;
	private ICProject project;
	private IFile file;

	public NewImplementActionDelegate() {
	}

	@Override
	public void run(IAction action) {
		if (!initialize()) {
			MessageDialog.openInformation(window.getShell(), "Information",
					"Implement function body is not available.");
			return;
		}
		runRefactoring(new NewImplementRefactoring(file, selection, project));
	}

	private void runRefactoring(NewImplementRefactoring refactoring) {
		RefactoringWizard wizard = new SilentWizard(refactoring);
		RefactoringWizardOpenOperation operator = new RefactoringWizardOpenOperation(wizard);
		try {
			operator.run(editor.getSite().getShell(), refactoring.getName());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean initialize() {
		if (window.getActivePage() == null)
			return false;
		editor = (TextEditor) window.getActivePage().getActiveEditor();
		if (editor == null || editor.getEditorInput() == null)
			return false;
		IWorkingCopy wc = CUIPlugin.getDefault().getWorkingCopyManager()
				.getWorkingCopy(editor.getEditorInput());
		project = wc.getCProject();
		file = (IFile) wc.getResource();
		return project != null && file != null;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof TextSelection))
			return;
		this.selection = (TextSelection) selection;
		action.setEnabled(!selection.isEmpty());
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
		assert (window != null);
	}
}
