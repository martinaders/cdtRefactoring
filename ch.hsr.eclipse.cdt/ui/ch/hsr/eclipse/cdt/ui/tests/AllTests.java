package ch.hsr.eclipse.cdt.ui.tests;

import ch.hsr.eclipse.cdt.ui.tests.nullrefactoring.NullRefactoringTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(NullRefactoringTestSuite.suite());
		return suite;
	}
}
