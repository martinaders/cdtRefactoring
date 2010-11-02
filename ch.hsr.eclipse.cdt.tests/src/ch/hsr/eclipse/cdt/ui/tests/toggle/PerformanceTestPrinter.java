package ch.hsr.eclipse.cdt.ui.tests.toggle;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestResult;

import org.eclipse.test.performance.PerformanceTestCase;

import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

/**
 * Provides a facility to append performance statistics to a separate file.
 */
public class PerformanceTestPrinter extends PerformanceTestCase {
	private static final String TEST_RESULTS_FILE = "performanceResults.txt";
	private static final String TEST_RESOURCE_DIRECTORY = "resources/refactoring/togglePerformance/";
	private static final String EXECUTING_TEST_CLASS = "ToggleRefactoringTest";
	private static final int DEFAULT_SAMPLE_SIZE = 100;
	private static PrintStream resultsFile;
	private PrintStream outstreamBackup;

	public PerformanceTestPrinter() {
		setupTestResultFileRedirection();
	}

	private void setupTestResultFileRedirection() {
		if (resultsFile != null)
			return;
		try {
			FileOutputStream outstream = new FileOutputStream(
					TEST_RESULTS_FILE, true);
			resultsFile = new java.io.PrintStream(outstream);
			resultsFile.println("******************************************************************");
			resultsFile.println("******** performance tests run on "
					+ new SimpleDateFormat("dd.MM.yyyy 'at' HH:mm:ss ").format(new Date()) + "*********");
			resultsFile.println("******************************************************************");
		} catch (FileNotFoundException e) {
		}
	}

	private void redirectPerformanceResults() {
		outstreamBackup = System.out;
		System.setOut(resultsFile);
	}

	private void redirectPerformanceResultsUndo() {
		System.setOut(outstreamBackup);
	}

	public void referenceTest() {
		for (int i = 0; i < 10; i++) {
			startMeasuring();
			stopMeasuring();
		}
		redirectPerformanceResults();
		commitMeasurements();
		redirectPerformanceResultsUndo();
		assertPerformance();
	}

	protected void runTests(String testFile) throws Exception {
		runTests(testFile, DEFAULT_SAMPLE_SIZE);
	}

	protected void runTests(String testFile, int sampleSize) throws Exception {
		for (int i = 0; i < sampleSize; i++) {
			startMeasuring();
			ExternalRefactoringTester.suite(EXECUTING_TEST_CLASS,
					TEST_RESOURCE_DIRECTORY + testFile).run(new TestResult());
			stopMeasuring();
		}
		redirectPerformanceResults();
		commitMeasurements();
		redirectPerformanceResultsUndo();
		assertPerformance();
	}
}
