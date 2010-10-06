package ch.hsr.eclipse.cdt.ui.tests.nullrefactoring;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class NullRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new NullRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("NullRefactoringTest", "resources/refactoring/NullRefactoring.rts"));
		return suite;
	}

}
