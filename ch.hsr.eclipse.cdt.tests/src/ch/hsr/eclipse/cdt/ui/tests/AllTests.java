package ch.hsr.eclipse.cdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.nullrefactoring.NullRefactoringTestSuite;

public class AllTests extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(NullRefactoringTestSuite.suite());
		return suite;
	}
}
