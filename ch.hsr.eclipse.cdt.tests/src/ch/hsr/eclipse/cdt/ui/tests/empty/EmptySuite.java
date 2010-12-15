package ch.hsr.eclipse.cdt.ui.tests.empty;

import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;
import ch.hsr.eclipse.cdt.ui.tests.toggle.ToggleRefactoringTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

public class EmptySuite {

	public static Test suite() throws Exception {
		TestSuite suite = new ToggleRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("EmptyTest", "resources/refactoring/zzEmptyTest.rts"));
		suite.addTest(ExternalRefactoringTester.suite("EmptyTestNext1", "resources/refactoring/zzEmptyTest2.rts"));
		return suite;
	}


}
