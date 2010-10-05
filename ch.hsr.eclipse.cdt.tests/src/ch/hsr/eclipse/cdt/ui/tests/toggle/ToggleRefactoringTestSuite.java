package ch.hsr.eclipse.cdt.ui.tests.toggle;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class ToggleRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite(ToggleRefactoringTestSuite.class.getName());
		suite.addTest(ExternalRefactoringTester.suite("NullRefactoringTest", "resources/refactoring/NullRefactoring.rts"));
		return suite;
	}

}
