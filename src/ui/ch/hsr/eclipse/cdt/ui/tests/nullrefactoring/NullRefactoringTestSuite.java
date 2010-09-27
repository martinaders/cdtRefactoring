package ch.hsr.eclipse.cdt.ui.tests.nullrefactoring;

import org.eclipse.cdt.ui.tests.refactoring.RefactoringTester;

import junit.framework.Test;
import junit.framework.TestSuite;

public class NullRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite(
				NullRefactoringTestSuite.class.getName());
		suite.addTest(RefactoringTester.suite("NullRefactoringTests", "resources/refactoring/NullRefactoring.rts"));
		return suite;
	}

}
