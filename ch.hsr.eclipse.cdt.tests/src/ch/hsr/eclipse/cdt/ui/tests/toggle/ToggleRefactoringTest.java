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

	protected boolean fatalError;

	public ToggleRefactoringTest(String name, Vector<TestSourceFile> files) {
		super(name, files);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void configureRefactoring(Properties refactoringProperties) {
		fatalError = Boolean.valueOf(refactoringProperties.getProperty("fatalerror", "false")).booleanValue(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected void runTest() throws Throwable {
		Refactoring refactoring = new ToggleRefactoring(project.getFile(fileName), selection, cproject);
		RefactoringStatus checkInitialConditions = refactoring.checkInitialConditions(NULL_PROGRESS_MONITOR);
		// Needed for the tu `unit' to be loaded.
		if (fatalError) {
			assertConditionsFatalError(checkInitialConditions);
			return;
		} else {
			assertConditionsOk(checkInitialConditions);
			executeRefactoring(refactoring);
		}
	}

	private void executeRefactoring(Refactoring refactoring) throws Exception {
		Change changes = refactoring.createChange(NULL_PROGRESS_MONITOR);
		assertConditionsOk(refactoring.checkFinalConditions(NULL_PROGRESS_MONITOR));
		changes.perform(NULL_PROGRESS_MONITOR);
		compareFiles(fileMap);		
	}

}
