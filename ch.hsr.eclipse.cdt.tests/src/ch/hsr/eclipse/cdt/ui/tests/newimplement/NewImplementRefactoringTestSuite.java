package ch.hsr.eclipse.cdt.ui.tests.newimplement;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class NewImplementRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new NewImplementRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("NewImplementRefactoringTest", "resources/refactoring/NewImplementRefactoring.rts"));
		return suite;
	}

}
