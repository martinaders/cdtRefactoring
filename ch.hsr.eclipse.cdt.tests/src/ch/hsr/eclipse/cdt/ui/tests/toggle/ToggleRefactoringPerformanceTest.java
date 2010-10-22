package ch.hsr.eclipse.cdt.ui.tests.toggle;

import org.eclipse.test.performance.PerformanceTestCase;

public class ToggleRefactoringPerformanceTest extends PerformanceTestCase {

	public void testMyOperation() {
		for (int i= 0; i < 10; i++) {
			startMeasuring();
			toMeasure();
			stopMeasuring();
		}
		commitMeasurements();
		assertPerformance();
	}

	private void toMeasure() {
		System.out.println("HELLLLLLLLLLLLLLL YYEEEEEEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAHHHHHHHHH!");
		
	}

}
