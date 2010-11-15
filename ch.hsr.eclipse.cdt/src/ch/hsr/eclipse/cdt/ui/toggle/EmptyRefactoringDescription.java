package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.HashMap;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
class EmptyRefactoringDescription extends CRefactoringDescription {
	public EmptyRefactoringDescription() {
		super("id", "proj", "desc", "comment", 0, new HashMap<String, String>());
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
		return new ToggleRefactoring(getFile(), (TextSelection)getSelection(), getCProject());
	}
}