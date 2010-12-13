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

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorEndifStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorObjectStyleMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.ui.refactoring.utils.TranslationUnitHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.UIPlugin;

import ch.hsr.ifs.redhead.helpers.IndexToASTNameHelper;

@SuppressWarnings("restriction")
public class ToggleRefactoringContext {

	private IASTFunctionDefinition targetDefinition;
	private IASTFunctionDeclarator targetDeclaration;
	private IASTTranslationUnit targetDefinitionUnit;
	private IASTTranslationUnit targetDeclarationUnit;
	private IIndex index;
	private IASTTranslationUnit selectionUnit;
	private IFile selectionFile;
	private IIndexBinding binding;
	private IASTName selectionName;

	public ToggleRefactoringContext(IIndex index, IFile file,
			TextSelection selection) {
		this.index = index;
		this.selectionFile = file;
		System.out.print("Stage 1: ");
		findSelectionUnit();
		System.out.print("complete\nStage 2: ");
		findSelectedFunctionDeclarator(selection);
		System.out.print("complete\nStage 3: ");
		findBinding();
		System.out.print("complete\nStage 4: ");
		findDeclaration();
		System.out.print("complete\nStage 5: ");
		findDefinition();
		System.out.print("complete\n\nStrategy: ");
	}

	private boolean hasUnsupportedNodes(IASTTranslationUnit unit) {
		for(IASTPreprocessorStatement ppstmt: unit.getAllPreprocessorStatements()) {
			if (ppstmt instanceof IASTPreprocessorIncludeStatement ||
					ppstmt instanceof IASTPreprocessorEndifStatement) {
				continue;
			}
			else if (ppstmt instanceof IASTPreprocessorObjectStyleMacroDefinition) {
				String name = ((IASTPreprocessorObjectStyleMacroDefinition) ppstmt)
					.getName().toString();
				if (!name.contains("_H_")) {
					return true;
				}
			}
			else if (ppstmt instanceof IASTPreprocessorIfndefStatement) {
				String name = ((IASTPreprocessorIfndefStatement) ppstmt)
					.getRawSignature();
				if (!name.contains("_H_")) {
					return true;
				}
			}
			else {
				return true;
			}
		}
		return false;
	}

	private void warn() {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				Shell shell = UIPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getShell();
				MessageDialog.openWarning(shell, "Macro found", "Your code contains Macros. These could be removed from your code!");
			}
		};
		PlatformUI.getWorkbench().getDisplay().syncExec(r);
	}

	public void findSelectedFunctionDeclarator(TextSelection selection) {
		selectionName = new DeclaratorFinder(selection, selectionUnit)
				.getName();
	}

	public void findBinding() {
		try {
			binding = index.findBinding(selectionName);
		} catch (CoreException e) {
		}
		if (binding == null)
			System.err.println("no binding was found, hopefully falling back to visitors");
	}

	// Declaration may still be null afterwards, but thats ok.
	public void findDeclaration() {
		try {
			IIndexName[] decnames = index.findNames(binding,
					IIndex.FIND_DECLARATIONS);
			if (decnames.length > 1)
				throw new NotSupportedException(
						"multiple declarations would result in ambiguous results");
			for (IIndexName iname : decnames) {
				selectionUnit = getTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						selectionUnit, iname, index);
				if (astname != null) {
					targetDeclaration = findFunctionDeclarator(astname);
					targetDeclarationUnit = selectionUnit;
					if (hasUnsupportedNodes(targetDeclarationUnit));
						warn();
					break;
				}
			}
		} catch (CoreException e) {
		}
		if (targetDeclaration == null)
			System.out.print("(no declaration found) ");
	}

	public void findDefinition() {
		try {
			IIndexName[] defnames = index.findNames(binding,
					IIndex.FIND_DEFINITIONS);
			if (defnames.length > 1)
				throw new NotSupportedException("one-definition-rule broken");
			for (IIndexName iname : defnames) {
				
				IASTTranslationUnit unit = getTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						unit, iname, index);
				if (astname != null) {
					targetDefinition = findFunctionDefinition(astname);
					targetDefinitionUnit = unit;
					if (hasUnsupportedNodes(targetDefinitionUnit))
						warn();
					break;
				}
			}
		} catch (CoreException e) {
		}
		if (targetDefinition == null)
			throw new NotSupportedException("cannot work without definition");
	}

	public IASTFunctionDeclarator getDeclaration() {
		return targetDeclaration;
	}

	public IASTFunctionDefinition getDefinition() {
		return targetDefinition;
	}

	public IASTTranslationUnit getDeclarationUnit() {
		return targetDeclarationUnit;
	}

	public IASTTranslationUnit getDefinitionUnit() {
		return targetDefinitionUnit;
	}

	public IFile getSelectionFile() {
		return selectionFile;
	}

	public IASTTranslationUnit getTUForSiblingFile() {
		IASTTranslationUnit unit = getDeclarationUnit();
		if (unit == null)
			unit = getDefinitionUnit();
		try {
			return ToggleNodeHelper.getSiblingFile(getSelectionFile(), unit);
		} catch (CoreException e) {
			return null;
		}
	}
	
	private void findSelectionUnit() {
		try {
			selectionUnit = TranslationUnitHelper.loadTranslationUnit(
					selectionFile, true);
		} catch (Exception e) {
		}
		if (selectionUnit == null)
			throw new NotSupportedException(
					"not able to work without translation unit");
	}

	private IASTTranslationUnit getTUForNameinFile(IIndexName iname)
			throws CModelException, CoreException {
		if (isSameFileAsInTU(iname))
			return selectionUnit;
		IASTTranslationUnit asttu = null;
		IPath path = new Path(iname.getFileLocation().getFileName());
		asttu = TranslationUnitHelper
				.loadTranslationUnit(path.toString(), true);
		return asttu;
	}

	private boolean isSameFileAsInTU(IIndexName iname) {
		return iname.getFileLocation().getFileName()
				.equals(selectionUnit.getFileLocation().getFileName());
	}

	private IASTFunctionDeclarator findFunctionDeclarator(IASTNode node) {
		if (node instanceof IASTSimpleDeclaration) {
			return (IASTFunctionDeclarator) ((IASTSimpleDeclaration) node)
					.getDeclarators()[0];
		}
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof IASTFunctionDeclarator)
				return (IASTFunctionDeclarator) node;
		}
		return null;
	}

	private IASTFunctionDefinition findFunctionDefinition(IASTNode node) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof IASTFunctionDefinition)
				return (IASTFunctionDefinition) node;
		}
		return null;
	}
}
