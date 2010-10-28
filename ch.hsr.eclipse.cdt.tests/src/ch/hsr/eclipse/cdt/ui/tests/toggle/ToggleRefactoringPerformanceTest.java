package ch.hsr.eclipse.cdt.ui.tests.toggle;

/**
 * Runs common/all toggling scenarios and measures their performance.
 */
public class ToggleRefactoringPerformanceTest extends PerformanceTestPrinter {

	public void testWithIncludeStatements() throws Exception {
		runTests("IncludeStatements.rts", 100);
	}

	public void testWithoutIncludeStatements() throws Exception {
		runTests("NoIncludeStatements.rts", 100);
	}

	public void testInClassToInHeader() throws Exception {
		runTests("InClassToInHeader.rts", 100);
	}

	public void testInHeaderToInClass() throws Exception {
		runTests("InHeaderToInClass.rts", 100);
	}

	public void testAllTests() throws Exception {
		runTests("../ToggleRefactoring.rts");
	}

}
