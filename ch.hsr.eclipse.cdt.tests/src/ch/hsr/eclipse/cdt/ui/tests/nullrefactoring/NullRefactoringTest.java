package ch.hsr.eclipse.cdt.ui.tests.nullrefactoring;

import java.util.Properties;
import java.util.Vector;
import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import ch.hsr.eclipse.cdt.ui.NullRefactoring;

public class NullRefactoringTest extends RefactoringTest {

	public NullRefactoringTest(String name, Vector<TestSourceFile> files) {
		super(name, files);
	}

	@Override
	protected void configureRefactoring(Properties refactoringProperties) {
	}

	@Override
	protected void runTest() throws Throwable {
		Refactoring refactoring = new NullRefactoring(null, selection, null, cproject);
		RefactoringStatus preconditions = refactoring.checkInitialConditions(NULL_PROGRESS_MONITOR);
		if (preconditions.hasError())
			fail("Initial conditions not satisfied.");
		compareFiles(fileMap);
	}

}
