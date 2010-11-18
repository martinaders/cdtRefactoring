package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;

import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;

@SuppressWarnings("restriction")
public interface ToggleRefactoringStrategy {

	public void run(ModificationCollector modifications);

	public ArrayList<String> getAffectedFiles();
}
