package ch.hsr.eclipse.cdt.ui.tests.newimplement;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;

import ch.hsr.eclipse.cdt.ui.implement.NewImplementRefactoring;;


public class NewImplementRefactoringTest extends RefactoringTest {

	public NewImplementRefactoringTest(String name, Vector<TestSourceFile> files) {
		super(name, files);
	}
	
	protected void setUp() throws Exception{
		super.setUp();
	}

	@Override
	protected void configureRefactoring(Properties refactoringProperties) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void runTest() throws Throwable {
		Refactoring refactoring = new NewImplementRefactoring(project.getFile(fileName), selection, cproject);
		// Needed for the tu `unit' to be loaded.
		assertConditionsOk(refactoring.checkInitialConditions(NULL_PROGRESS_MONITOR));
		Change changes = refactoring.createChange(NULL_PROGRESS_MONITOR);
		assertConditionsOk(refactoring.checkFinalConditions(NULL_PROGRESS_MONITOR));
		changes.perform(NULL_PROGRESS_MONITOR);
		compareFiles(fileMap);
	}

}
