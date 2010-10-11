package ch.hsr.eclipse.cdt.ui;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.hidemethod.HideMethodRefactoringWizard;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;

import ch.hsr.eclipse.cdt.ui.toggle.ToggleRefactoring;

public class TogglingActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private TextSelection selection;
	private TextEditor editor;

	public TogglingActionDelegate() {
	}

	@Override
	public void run(IAction action) {
		if (PlatformUI.getWorkbench() == null || PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null || PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null || PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() == null) {
			MessageDialog.openInformation(window.getShell(), "Information", "Toggling function body is not available if no editor is active.");
			return;
		}
		editor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor.getEditorInput() == null) {
			MessageDialog.openInformation(window.getShell(), "Information", "No editor input available for refactoring. Check whether you're editing a valid resource.");
			return;
		}
		String filename = editor.getEditorInput().getName();
		if (filename == null) {
			return;
		}
		ICProject project = null;
		try {
			project = (ICProject) ResourcesPlugin.getWorkspace().getRoot().getProject();
		} catch (ClassCastException e) {
			return;
		}
		IWorkingCopy wc = CUIPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editor.getEditorInput());
		project = wc.getCProject();
		IFile resource = (IFile)wc.getResource();
		
		if (project == null) {
			return;
		}

		ToggleRefactoring refactoring = new ToggleRefactoring(resource, selection, project);
		try {
			HideMethodRefactoringWizard wizard = new HideMethodRefactoringWizard(refactoring);
			RefactoringWizardOpenOperation operator = new RefactoringWizardOpenOperation(wizard);
			try {
				operator.run(editor.getSite().getShell(), refactoring.getName());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} catch (Exception e) {}
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
	}
}
