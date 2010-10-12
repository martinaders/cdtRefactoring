package ch.hsr.eclipse.cdt.ui.implement;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Used to start a refactoring without wizard if no warnings are present.
 */
class SilentWizard extends RefactoringWizard {
	SilentWizard(Refactoring refactoring) {
		super(refactoring, RefactoringWizard.NO_PREVIEW_PAGE);
	}

	@Override
	protected void addUserInputPages() {
	}
}