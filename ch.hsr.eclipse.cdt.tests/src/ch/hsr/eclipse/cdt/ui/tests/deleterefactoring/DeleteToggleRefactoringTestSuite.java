package ch.hsr.eclipse.cdt.ui.tests.deleterefactoring;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class DeleteToggleRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite(DeleteToggleRefactoringTestSuite.class.getName());
		suite.addTest(ExternalRefactoringTester.suite("NullRefactoringTest", "resources/refactoring/NullRefactoring.rts"));
		return suite;
	}

}
