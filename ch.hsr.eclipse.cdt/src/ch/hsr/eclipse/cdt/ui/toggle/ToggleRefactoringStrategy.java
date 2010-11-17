package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;

@SuppressWarnings("restriction")
public interface ToggleRefactoringStrategy {

	public void run(ModificationCollector modifications);
}
