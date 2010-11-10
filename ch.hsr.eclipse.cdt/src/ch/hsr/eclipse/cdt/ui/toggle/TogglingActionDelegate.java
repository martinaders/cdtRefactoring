package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * Responsible for starting the ToggleRefactoring when the user invokes the
 * refactoring.
 * 
 * Order of execution is: constructor, init, selectionChanged, run
 */
public class TogglingActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private TextSelection selection;
	private ICProject project;
	private IFile file;
	
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
		assert (window != null);
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		boolean isTextSelection = selection instanceof TextSelection;
		action.setEnabled(isTextSelection);
		if (!isTextSelection)
			return;
		this.selection = (TextSelection) selection;
	}

	@Override
	public void run(IAction action) {
		System.out.println("run");
		if (!initialize()) {
			MessageDialog.openInformation(window.getShell(), "Information",
					"Toggling function body is not available.");
			return;
		}
		runRefactoring(new ToggleRefactoring(file, selection, project));
	}

	@SuppressWarnings("restriction")
	private void runRefactoring(ToggleRefactoring refactoring) {
		try {
			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());
			if (status.hasEntries())
				return;
			Change change = refactoring.createChange(new NullProgressMonitor());
			change.perform(new NullProgressMonitor());
			refactoring.openEditorIfNeeded();
		} catch (OperationCanceledException e) {
		} catch (CoreException e) {
		}
	}

	private boolean initialize() {
		if (window.getActivePage() == null)
			return false;
		TextEditor editor = (TextEditor) window.getActivePage().getActiveEditor();
		if (editor == null || editor.getEditorInput() == null)
			return false;
		IWorkingCopy wc = CUIPlugin.getDefault().getWorkingCopyManager()
				.getWorkingCopy(editor.getEditorInput());
		project = wc.getCProject();
		file = (IFile) wc.getResource();
		return project != null && file != null;
	}

	@Override
	public void dispose() {
	}
}
