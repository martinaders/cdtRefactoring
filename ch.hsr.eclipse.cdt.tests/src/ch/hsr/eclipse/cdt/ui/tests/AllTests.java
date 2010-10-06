package ch.hsr.eclipse.cdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.toggle.ToggleRefactoringTestSuite;

public class AllTests extends TestSuite {
	
	public static Test suite() throws Exception {
		return new AllTests();
	}

	public AllTests() throws Exception {
		addTest(ToggleRefactoringTestSuite.suite());
	}
}
