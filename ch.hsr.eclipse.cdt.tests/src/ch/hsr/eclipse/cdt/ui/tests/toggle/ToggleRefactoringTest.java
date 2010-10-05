package ch.hsr.eclipse.cdt.ui.tests.toggle;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import ch.hsr.eclipse.cdt.ui.toggle.ToggleRefactoring;

public class ToggleRefactoringTest extends RefactoringTest {

	public ToggleRefactoringTest(String name, Vector<TestSourceFile> files) {
		super(name, files);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void configureRefactoring(Properties refactoringProperties) {
	}

	@Override
	protected void runTest() throws Throwable {
		Refactoring refactoring = new ToggleRefactoring(project.getFile(fileName), selection, cproject);
		// this is needed for the tu `unit' to be loaded.
		refactoring.checkInitialConditions(NULL_PROGRESS_MONITOR);
		Change changes = refactoring.createChange(NULL_PROGRESS_MONITOR);
		RefactoringStatus status = refactoring.checkFinalConditions(NULL_PROGRESS_MONITOR);
		assertConditionsOk(status);
		changes.perform(NULL_PROGRESS_MONITOR);
		compareFiles(fileMap);
	}

}
