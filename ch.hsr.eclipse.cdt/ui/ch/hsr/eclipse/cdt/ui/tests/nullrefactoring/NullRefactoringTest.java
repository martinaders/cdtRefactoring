package ch.hsr.eclipse.cdt.ui.tests.nullrefactoring;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
import org.eclipse.ltk.core.refactoring.Change;

import ch.hsr.eclipse.cdt.NullRefactoring;

public class NullRefactoringTest extends RefactoringTest {

	public NullRefactoringTest(String name, Vector<TestSourceFile> files) {
		super(name, files);
	}

	@Override
	protected void configureRefactoring(Properties refactoringProperties) {
	}

	@Override
	protected void runTest() throws Throwable {
		CRefactoring refactoring = new NullRefactoring(null, selection, null, cproject);
		Change change = refactoring.createChange(NULL_PROGRESS_MONITOR);
		change.perform(NULL_PROGRESS_MONITOR);
		compareFiles(fileMap);
	}

}
