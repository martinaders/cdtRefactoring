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
package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
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
import org.eclipse.jface.text.TextSelection;

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
	private boolean defaultAnswer;
	private boolean settedDefaultAnswer;

	public ToggleRefactoringContext(IIndex index, IFile file, TextSelection selection) {
		this.index = index;
		this.selectionFile = file;
		findSelectionUnit();
		findSelectedFunctionDeclarator(selection);
		findBinding();
		findDeclaration();
		findDefinition();
	}

	public void findSelectedFunctionDeclarator(TextSelection selection) {
		selectionName = new DeclaratorFinder(selection, selectionUnit).getName();
	}

	public void findBinding() {
		try {
			binding = index.findBinding(selectionName);
		} catch (CoreException e) {
		}
	}

	// Declaration may still be null afterwards, but thats ok.
	public void findDeclaration() {
		try {
			IIndexName[] decnames = index.findNames(binding, IIndex.FIND_DECLARATIONS);
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
					break;
				}
			}
		} catch (CoreException e) {
		}
	}

	public void findDefinition() {
		try {
			IIndexName[] defnames = index.findNames(binding, IIndex.FIND_DEFINITIONS);
			if (defnames.length > 1) {
				throw new NotSupportedException("one-definition-rule broken");
			}
			for (IIndexName iname : defnames) {
				IASTTranslationUnit unit = getTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						unit, iname, index);
				if (astname != null) {
					targetDefinition = findFunctionDefinition(astname);
					targetDefinitionUnit = unit;
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
			throw new NotSupportedException("not able to work without translation unit");
	}

	private IASTTranslationUnit getTUForNameinFile(IIndexName iname)
			throws CModelException, CoreException {
		if (isSameFileAsInTU(iname)) {
			return selectionUnit;
		}
		IPath path = new Path(iname.getFileLocation().getFileName());
		return TranslationUnitHelper.loadTranslationUnit(path.toString(), true);
	}

	private boolean isSameFileAsInTU(IIndexName iname) {
		return iname.getFileLocation().getFileName().equals(
				selectionUnit.getFileLocation().getFileName());
	}

	private IASTFunctionDeclarator findFunctionDeclarator(IASTNode node) {
		if (node instanceof IASTSimpleDeclaration) {
			return (IASTFunctionDeclarator) ((IASTSimpleDeclaration) node).getDeclarators()[0];
		}
		return ToggleNodeHelper.getAncestorOfType(node, IASTFunctionDeclarator.class);
	}

	private IASTFunctionDefinition findFunctionDefinition(IASTNode node) {
		return ToggleNodeHelper.getAncestorOfType(node, IASTFunctionDefinition.class);
	}

	public void setDefaultAnswer(boolean defaultAnswer) {
		this.defaultAnswer = defaultAnswer;
	}

	public boolean getDefaultAnswer() {
		return defaultAnswer;
	}

	public void setSettedDefaultAnswer(boolean settedDefaultAnswer) {
		this.settedDefaultAnswer = settedDefaultAnswer;
	}

	public boolean isSettedDefaultAnswer() {
		return settedDefaultAnswer;
	}
}
