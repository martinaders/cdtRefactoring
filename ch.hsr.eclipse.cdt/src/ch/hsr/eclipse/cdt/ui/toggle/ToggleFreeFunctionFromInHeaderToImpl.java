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

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.core.dom.rewrite.commenthandler.ASTCommenter;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl implements ToggleRefactoringStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;
	private IASTFunctionDefinition selectedDefinition;
	private IASTTranslationUnit definition_unit;
	private TextEditGroup infoText = new TextEditGroup("Toggle function body placement");

	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) throws CModelException, CoreException {
		this.selectedDefinition = context.getDefinition();
		this.definition_unit = context.getDefinitionUnit();
		this.siblingfile_translation_unit = context.getTUForSiblingFile();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite astrewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTSimpleDeclaration declaration = ToggleNodeHelper.createDeclarationFromDefinition(selectedDefinition);
		ASTCommenter.addCommentMapping(selectedDefinition, declaration);
		astrewriter.replace(selectedDefinition, declaration, infoText);
		
		ASTRewrite otherrewrite = modifications
		.rewriterForTranslationUnit(siblingfile_translation_unit);

		IASTFunctionDefinition newDefinition = selectedDefinition.copy();
		ToggleNodeHelper.remapAllComments(selectedDefinition, newDefinition, true);
		otherrewrite.insertBefore(
				siblingfile_translation_unit.getTranslationUnit(), null,
				newDefinition, infoText);
	}
}
