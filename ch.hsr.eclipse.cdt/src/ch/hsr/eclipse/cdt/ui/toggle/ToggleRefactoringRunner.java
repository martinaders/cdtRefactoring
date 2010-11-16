package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringRunner;
import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Responsible for scheduling a job which runs the ToggleRefactoring. Differs
 * from other subclasses of RefactoringRunner in the way that it does not use a
 * wizard but calls the refactoring directly.
 */
@SuppressWarnings("restriction")
public class ToggleRefactoringRunner extends RefactoringRunner {

	private final class RefactoringJob extends Job {
		private RefactoringJob() {
			super("'toggle body position' code automation");
			setPriority(Job.SHORT);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());
				if (status.hasFatalError())
					return JobStatus.CANCEL_STATUS;
				Change change = refactoring.createChange(new NullProgressMonitor());
				change.perform(new NullProgressMonitor());
				refactoring.openEditorIfNeeded();
			} catch (Exception e) {
				System.err.println("Failure during generation of changes:");
				e.printStackTrace();
			}
			return JobStatus.OK_STATUS;
		}
	}

	private ToggleRefactoring refactoring;

	public ToggleRefactoringRunner(IFile file, TextSelection selection,
			ICElement element, IShellProvider shellProvider, ICProject project) {
		super(file, selection, element, shellProvider, project);
		refactoring = new ToggleRefactoring(file, selection, project);
	}

	@Override
	public void run() {
		new RefactoringJob().schedule();
	}

}
