package ch.hsr.eclipse.cdt.ui.tests.empty;

import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.cdt.ui.tests.refactoring.TestSourceFile;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class EmptyTest extends RefactoringTest {

	protected boolean fatalError;

	public EmptyTest(String name, Vector<TestSourceFile> files) {
		super(name, files);
	}

	@Override
	protected void configureRefactoring(Properties refactoringProperties) {
		fatalError = Boolean.valueOf(refactoringProperties.getProperty("fatalerror", "false")).booleanValue(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected void runTest() throws Throwable {
		Refactoring refactoring = null;
		RefactoringStatus checkInitialConditions = new RefactoringStatus();
		assertConditionsOk(checkInitialConditions);
		executeRefactoring(refactoring);
	}

	private void executeRefactoring(Refactoring refactoring) throws Exception {
		compareFiles(fileMap);		
	}
	
}
