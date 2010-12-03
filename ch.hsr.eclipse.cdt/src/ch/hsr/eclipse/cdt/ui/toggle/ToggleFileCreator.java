/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 * 		Martin Schwab & Thomas Kallenberg - initial API and implementation 
 ******************************************************************************/
package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.UIPlugin;

@SuppressWarnings("restriction")
public class ToggleFileCreator {

	private ToggleRefactoringContext context;
	private String ending;

	public ToggleFileCreator(ToggleRefactoringContext context, String ending) {
		this.context = context;
		this.ending = ending;
	}
	
	public IASTTranslationUnit loadTranslationUnit() {
		String filename;
		if (context.getDeclaration() != null)
			filename = context.getDeclaration().getContainingFilename();
		else
			filename = context.getDefinition().getContainingFilename();
		String other;
		if (ending.equals(".h"))
			other = ".cpp";
		else
			other = ".h";
		filename = filename.replaceAll("\\w*" + other + "$", "");
		filename = filename + getNewFileName();
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(filename));
		IASTTranslationUnit result = null;
		try {
			result = CoreModelUtil.findTranslationUnitForLocation(file.getFullPath(), null).getAST();
		} catch (CModelException e) {
		} catch (CoreException e) {
		}
		if (result == null)
			throw new NotSupportedException("Cannot find translation unit for sibling file");
		return result;
	}
	
	public void createNewFile() {
		CreateFileChange change;
		String filename = getNewFileName();
		try {
			change = new CreateFileChange(filename, new	Path(getPath()+filename), 
					"", context.getSelectionFile().getCharset());
			change.perform(new NullProgressMonitor());
		} catch (CoreException e) {
			throw new NotSupportedException("Cannot create new filechange");
		}
	}
	
	public boolean askUserForFileCreation(final ToggleRefactoringContext context) {
		final Container<Boolean> answer = new Container<Boolean>();
		Runnable r = new Runnable() {
			@Override
			public void run() {
				Shell shell = UIPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getShell();
				String functionname;
				if (context.getDeclaration() != null)
					functionname = context.getDeclaration().getRawSignature();
				else
					functionname = context.getDefinition().getDeclarator().getRawSignature();
				boolean createnew = MessageDialog.openQuestion(shell, "New Implementation file?", 
						"Create a new file named: " + getNewFileName() + " and move " + functionname + "?");
				answer.setObject(createnew);
			}
		};
		PlatformUI.getWorkbench().getDisplay().syncExec(r);
		return answer.getObject();
	}

	public String getIncludeStatement() {
		return "#include \"" + ToggleNodeHelper.getFilenameWithoutExtension(getNewFileName()) + ".h\"";
	}
	
	private String getNewFileName() {
		return ToggleNodeHelper.getFilenameWithoutExtension(context.getSelectionFile().getFullPath().toString()) + ending;
	}
	
	private String getPath() {
		String result = context.getSelectionFile().getFullPath().toOSString();
		return result.replaceAll("(\\w)*\\.(\\w)*", "");
	}
}
