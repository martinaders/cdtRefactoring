package ch.hsr.eclipse.cdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.nullrefactoring.NullRefactoringTestSuite;
import ch.hsr.eclipse.cdt.ui.tests.toggle.ToggleRefactoringTestSuite;

public class AllTests extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(NullRefactoringTestSuite.suite());
		suite.addTest(ToggleRefactoringTestSuite.suite());
		return suite;
	}
}
