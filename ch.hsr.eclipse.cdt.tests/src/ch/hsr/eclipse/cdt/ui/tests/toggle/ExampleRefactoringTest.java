package ch.hsr.eclipse.cdt.ui.tests.toggle;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;

import ch.hsr.eclipse.cdt.ui.example.ExampleRefactoring;

public class ExampleRefactoringTest extends RefactoringTest {

	public ExampleRefactoringTest(String name, Vector<TestSourceFile> files) {
		super(name, files);
	}

	@Override
	protected void configureRefactoring(Properties refactoringProperties) {
	}

	@Override
	protected void runTest() throws Throwable {
		Refactoring refactoring = new ExampleRefactoring(project.getFile(fileName), selection, null, null);
		assertConditionsOk(refactoring.checkInitialConditions(NULL_PROGRESS_MONITOR));
		Change changes = refactoring.createChange(NULL_PROGRESS_MONITOR);
		assertConditionsOk(refactoring.checkFinalConditions(NULL_PROGRESS_MONITOR));
		changes.perform(NULL_PROGRESS_MONITOR);
		compareFiles(fileMap);
	}
}