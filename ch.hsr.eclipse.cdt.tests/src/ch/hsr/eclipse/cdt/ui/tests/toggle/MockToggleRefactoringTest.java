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

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;

import ch.hsr.eclipse.cdt.ui.toggle.ToggleRefactoring;
import ch.hsr.eclipse.cdt.ui.toggle.ToggleRefactoringContext;

public class MockToggleRefactoringTest extends ToggleRefactoring {

	public MockToggleRefactoringTest(IFile file, TextSelection selection,
			ICProject proj) {
		super(file, selection, proj);
	}

	public ToggleRefactoringContext getContext() {
		return context;
	}
}
