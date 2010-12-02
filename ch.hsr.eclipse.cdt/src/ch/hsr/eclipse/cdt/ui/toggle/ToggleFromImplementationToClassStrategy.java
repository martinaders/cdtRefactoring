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
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToClassStrategy implements ToggleRefactoringStrategy {

	private ToggleRefactoringContext context;
	protected TextEditGroup infoText;

	public ToggleFromImplementationToClassStrategy(
			ToggleRefactoringContext context) {
		if (context.getDeclarationUnit() == null)
			throw new NotSupportedException("Not supported if no declaration is found");
		if (context.getDeclarationUnit() == null
				|| context.getDeclarationUnit() == context.getDefinitionUnit())
			throw new NotSupportedException(
					"Definition and declaration both in a cpp file: not clear where to move definition.");
		this.context = context;
		this.infoText = new TextEditGroup("Toggle function body placement");
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite implast = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		implast.remove(context.getDefinition(), infoText);
		
		if (context.getDeclarationUnit() != null) {
			ASTRewrite headerast = modifications.rewriterForTranslationUnit(context.getDeclarationUnit());
			IASTFunctionDefinition newdefinition = ToggleNodeHelper.createInClassDefinition(context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
			headerast.replace(context.getDeclaration().getParent(), newdefinition, infoText);
		} else {
			IASTTranslationUnit other_unit = null;
			try {
				other_unit = ToggleNodeHelper.getSiblingFile(context.getSelectionFile(), context.getDefinitionUnit());
				IASTFunctionDefinition function = context.getDefinition().copy();
				ASTRewrite classast = modifications.rewriterForTranslationUnit(context.getDeclarationUnit());
				classast.insertBefore(other_unit, null, function, infoText);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
}
