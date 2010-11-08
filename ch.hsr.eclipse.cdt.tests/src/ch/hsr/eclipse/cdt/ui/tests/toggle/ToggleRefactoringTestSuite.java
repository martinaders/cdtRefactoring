package ch.hsr.eclipse.cdt.ui.tests.toggle;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class ToggleRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new ToggleRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("ToggleRefactoringTest", "resources/refactoring/ToggleRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleRefactoringTest", "resources/refactoring/ToggleRefactoringSelection.rts"));
		return suite;
	}

}
