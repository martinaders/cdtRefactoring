package ch.hsr.eclipse.cdt.ui.tests.toggle;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;

import ch.hsr.eclipse.cdt.ui.toggle.ToggleRefactoring;
import ch.hsr.eclipse.cdt.ui.toggle.ToggleRefactoringContext;

public class MockToggleRefactoringTest extends ToggleRefactoring {

	public MockToggleRefactoringTest(IFile file, TextSelection selection,
			ICProject proj) {
		super(file, selection, proj);
	}

	public ToggleRefactoringContext getContext() {
		return context;
	}
}
