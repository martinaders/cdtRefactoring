package ch.hsr.eclipse.cdt.ui.tests.toggle;

import junit.framework.Test;
import junit.framework.TestResult;

import org.eclipse.test.performance.PerformanceTestCase;

import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class ToggleRefactoringPerformanceTest extends PerformanceTestCase {

	public void testAllTests() {
		for (int i= 0; i < 3; i++) {
			startMeasuring();
			toMeasure();
			stopMeasuring();
		}
		commitMeasurements();
		assertPerformance();
	}

	private void toMeasure() {
		try {
			Test suite = ExternalRefactoringTester.suite("ToggleRefactoringTest", "resources/refactoring/ToggleRefactoring.rts");
			suite.run(new TestResult());
		} catch (Exception e) {
		}
	}
}
