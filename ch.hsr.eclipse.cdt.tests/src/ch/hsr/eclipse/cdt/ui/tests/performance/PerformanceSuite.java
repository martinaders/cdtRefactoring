package ch.hsr.eclipse.cdt.ui.tests.performance;

import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;
import ch.hsr.eclipse.cdt.ui.tests.toggle.ToggleRefactoringTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

public class PerformanceSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new ToggleRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("PerformanceTest", "resources/refactoring/zzPerformance.rts"));
		return suite;
	}


}
