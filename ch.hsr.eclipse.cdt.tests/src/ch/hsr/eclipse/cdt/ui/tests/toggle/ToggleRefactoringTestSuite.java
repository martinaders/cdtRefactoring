/*******************************************************************************
 * Copyright (c) 2011 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 * 		Martin Schwab & Thomas Kallenberg - initial API and implementation 
 ******************************************************************************/
package ch.hsr.eclipse.cdt.ui.tests.toggle;

import junit.framework.Test;
import junit.framework.TestSuite;
import ch.hsr.eclipse.cdt.ui.tests.ExternalRefactoringTester;

public class ToggleRefactoringTestSuite extends TestSuite {

	public static Test suite() throws Exception {
		TestSuite suite = new ToggleRefactoringTestSuite();
		suite.addTest(ExternalRefactoringTester.suite("NewCreationTest", "resources/refactoring/NewCreationTest.rts"));
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
