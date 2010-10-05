package ch.hsr.eclipse.cdt.ui;

import java.util.HashMap;

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

	static protected final String NAME = "NullRefactoring"; //$NON-NLS-1$
	
	public NullRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new CRefactoringDescription("id", "proj", "desc", "comment", 0, new HashMap<String, String>()) {
			@Override
			public Refactoring createRefactoring(RefactoringStatus status)
					throws CoreException {
				return new NullRefactoring(getFile(), getSelection(), getCProject());
			}
		};
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException,
			OperationCanceledException {
	}

}
