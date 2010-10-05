package ch.hsr.eclipse.cdt.ui.tests.toggle;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
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
		RefactoringStatus preconditions = refactoring.checkInitialConditions(NULL_PROGRESS_MONITOR);
		assertFalse(preconditions.hasError());
		compareFiles(fileMap);
	}

}
