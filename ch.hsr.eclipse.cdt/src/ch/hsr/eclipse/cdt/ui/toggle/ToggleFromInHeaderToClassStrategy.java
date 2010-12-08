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

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy implements
		ToggleRefactoringStrategy {

	private TextEditGroup infoText;
	private ToggleRefactoringContext context;

	public ToggleFromInHeaderToClassStrategy(ToggleRefactoringContext context) {
		this.context = context;
		this.infoText =  new TextEditGroup("Toggle function body placement");
		if (isFreeFunction(context))
			throw new NotSupportedException("Cannot toggle templated free function");
	}

	private boolean isFreeFunction(ToggleRefactoringContext context) {
		return !ToggleNodeHelper.isInsideAClass(context.getDefinition().getDeclarator(), context.getDeclaration());
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(context
				.getDefinitionUnit());
		rewriter.remove(ToggleNodeHelper.getParentRemovePoint(context.getDefinition()), infoText);
		rewriter.replace(context.getDeclaration().getParent(),
				ToggleNodeHelper.createInClassDefinition( context.getDeclaration(), 
						context.getDefinition(), context.getDefinitionUnit()), infoText);

	}
}
