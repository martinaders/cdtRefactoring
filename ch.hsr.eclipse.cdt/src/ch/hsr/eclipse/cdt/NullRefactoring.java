package ch.hsr.eclipse.cdt;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class NullRefactoring extends CRefactoring {

	public NullRefactoring(IFile file, ISelection selection, ICElement element,
			ICProject proj) {
		super(file, selection, element, proj);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		final NullRefactoring ref = this;
		return new CRefactoringDescription("id", "proj", "desc", "comment", 0, null) {
			@Override
			public Refactoring createRefactoring(RefactoringStatus status)
					throws CoreException {
				return ref;
			}
		};
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException,
			OperationCanceledException {
		
	}

}
