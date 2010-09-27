package ch.hsr.eclipse.cdt.ui.tests.nullrefactoring;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
import org.eclipse.core.resources.IFile;

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
		IFile refFile = null;
		CRefactoring refactoring = new NullRefactoring(refFile ,selection, null, cproject);
	}

}
