package ch.hsr.eclipse.cdt.ui.tests.toggle;

/**
 * Runs common/all toggling scenarios and measures their performance.
 */
public class ToggleRefactoringPerformanceTest extends PerformanceTestPrinter {

	public void testWithIncludeStatements() throws Exception {
		runTests("IncludeStatements.rts");
	}

	public void testWithoutIncludeStatements() throws Exception {
		runTests("NoIncludeStatements.rts");
	}

	public void testInClassToInHeader() throws Exception {
		runTests("InClassToInHeader.rts");
	}

	public void testInHeaderToInClass() throws Exception {
		runTests("InHeaderToInClass.rts");
	}

	public void testAllTests() throws Exception {
		runTests("../ToggleRefactoring.rts", 2);
	}
	
	public void testReferenceTest() {
		referenceTest();
	}

}
