package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

class RefactoringJob extends Job {
	public final static Object FAMILY_TOGGLE_DEFINITION = new Object();
	private final Refactoring refactoring;
	
	RefactoringJob(Refactoring refactoring) {
		super("'toggle function definition' code automation");
		this.refactoring = refactoring;
		setPriority(Job.SHORT);
	}
	
	@Override
	public boolean belongsTo(Object family) {
		return family == FAMILY_TOGGLE_DEFINITION;
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
				return Status.CANCEL_STATUS;
			change = refactoring.createChange(monitor);
			change.initializeValidationData(monitor);
			if (!change.isValid(monitor).isOK()) {
				return Status.CANCEL_STATUS;
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
				if (success && undoChange.isValid(monitor).isOK()) {
					// Note: addUndo MUST be called AFTER changePerformed or
					// the change won't be unlocked correctly. (17.11.2010)
					undoManager.addUndo("toggle function definition", undoChange);
				}
			} catch (OperationCanceledException e) {
			} catch (CoreException e) {
			}
		}
		return Status.OK_STATUS;
	}
}