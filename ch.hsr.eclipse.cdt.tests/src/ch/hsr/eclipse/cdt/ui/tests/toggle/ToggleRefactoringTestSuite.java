package ch.hsr.eclipse.cdt.ui.tests.toggle;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class ToggleRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new ToggleRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("ToggleErrorRefactoring", "resources/refactoring/ToggleErrorRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleSelectionTest", "resources/refactoring/ToggleSelection.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleSimpleFunctionRefactoringTest", "resources/refactoring/ToggleSimpleFunctionRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleTemplateRefactoringTest", "resources/refactoring/ToggleTemplateRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleNamespaceRefacotringTest", "resources/refactoring/ToggleNamespaceRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleTryCatchRefactoringTest", "resources/refactoring/ToggleTryCatchRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleDefaultParameterRefactoringTest", "resources/refactoring/ToggleDefaultParameterRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleCtorDtorRefactoringTest", "resources/refactoring/ToggleCtorDtorRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleNestedRefactoringTest", "resources/refactoring/ToggleNestedRefactoring.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleDifferentSelectionsTest", "resources/refactoring/ToggleDifferentSelections.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleFreeFunctionTest", "resources/refactoring/ToggleFreeFunction.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleVirtualFunctionTest", "resources/refactoring/ToggleVirtualFunction.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleOrderintTest", "resources/refactoring/ToggleOrdering.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleCommentsClassToHeader", "resources/refactoring/ToggleCommentsClassToHeader.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleCommentsHeaderToClass", "resources/refactoring/ToggleCommentsHeaderToClass.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleCommentsHeaderToImpl", "resources/refactoring/ToggleCommentsHeaderToImpl.rts"));
		suite.addTest(ExternalRefactoringTester.suite("ToggleCommentsImplToHeader", "resources/refactoring/ToggleCommentsImplToHeader.rts"));
		return suite;
	}

}
