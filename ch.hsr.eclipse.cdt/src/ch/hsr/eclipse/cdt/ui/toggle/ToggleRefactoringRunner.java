package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringRunner;
import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Responsible for scheduling a job which runs the ToggleRefactoring. Differs
 * from other subclasses of RefactoringRunner in the way that it does not use a
 * wizard but calls the refactoring directly.
 */
@SuppressWarnings("restriction")
public class ToggleRefactoringRunner extends RefactoringRunner {
	
	private static final Object FAMILY_TOGGLE_BODY = new Object();
	private ToggleRefactoring refactoring;

	private final class RefactoringJob extends Job {
		private RefactoringJob() {
			super("'toggle body position' code automation");
			setPriority(Job.SHORT);
		}
		
		@Override
		public boolean belongsTo(Object family) {
			return family == FAMILY_TOGGLE_BODY;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IUndoManager undoManager = RefactoringCore.getUndoManager();
			Change change = new NullChange();
			Change undoChange = new NullChange();
			boolean success = false;
			try {
				RefactoringStatus status = refactoring.checkAllConditions(monitor);
				if (status.hasFatalError())
					return JobStatus.CANCEL_STATUS;
				change = refactoring.createChange(monitor);
				change.initializeValidationData(monitor);
				if (!change.isValid(monitor).isOK()) {
					return JobStatus.CANCEL_STATUS;
				}
				undoManager.aboutToPerformChange(change);
				undoChange = change.perform(monitor);
				success = true;
			} catch (IllegalStateException e) {
				System.err.println("Another refactoring is still in progress, aborting.");
			} catch (CoreException e) {
				System.err.println("Failure during generation of changes. Toggled too fast?");
			} finally {
				undoChange.initializeValidationData(monitor);
				undoManager.changePerformed(change, success);					
				try {
					if (success && undoChange != null) {
						// Note: addUndo MUST be called AFTER changePerformed or
						// the change won't be unlocked correctly. (17.11.2010)
						System.out.println("add undo");
						undoManager.addUndo("toggle function body", undoChange);
					}
				} catch (OperationCanceledException e) {
					e.printStackTrace();
				}
			}
			return JobStatus.OK_STATUS;
		}
	}

	public ToggleRefactoringRunner(IFile file, TextSelection selection,
			ICElement element, IShellProvider shellProvider, ICProject project) {
		super(file, selection, element, shellProvider, project);
		refactoring = new ToggleRefactoring(file, selection, project);
	}

	@Override
	public void run() {
		Job[] jobs = Job.getJobManager().find(FAMILY_TOGGLE_BODY);
		if (jobs.length > 0)
			System.err.println("Another Toggling-Job still in progress, aborting.");
		new RefactoringJob().schedule();
	}
}
