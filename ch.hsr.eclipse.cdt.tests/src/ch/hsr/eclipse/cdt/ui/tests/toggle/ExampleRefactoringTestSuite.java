package ch.hsr.eclipse.cdt.ui.tests.toggle;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class ExampleRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new ExampleRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("ExampleRefactoringTest", "resources/refactoring/ExampleComments.rts"));
		return suite;
	}

}
