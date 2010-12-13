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

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;

public class ToggleStrategyFactory {
	
	private ToggleRefactoringContext context;

	public ToggleStrategyFactory(ToggleRefactoringContext context) {
		this.context = context;
	}

	public ToggleRefactoringStrategy getAppropriateStategy() {
		if (context.getDefinition() == null)
			throw new NotSupportedException("cannot work without function defintion");
		if (!context.getDefinitionUnit().isHeaderUnit())
			return new ToggleFromImplementationToHeaderOrClassStrategy(context);
		if (isInClassSituation())
			return new ToggleFromClassToInHeaderStrategy(context);
		if (isTemplateSituation())
			return new ToggleFromInHeaderToClassStrategy(context);
		if (isinHeaderSituation())
			return new ToggleFromInHeaderToImplementationStrategy(context);
		throw new NotSupportedException("Unsupported situation for moving function body.");
	}
	
	private boolean isinHeaderSituation() {
		return (context.getDefinition() != null) 
			&& (context.getDefinitionUnit().isHeaderUnit());
	}

	private boolean isInClassSituation() {
		return (context.getDeclaration() == null) && 
			(ToggleNodeHelper.getAncestorOfType(context.getDefinition(), 
					IASTCompositeTypeSpecifier.class) != null);
	}

	private boolean isTemplateSituation() {
		return (ToggleNodeHelper.getAncestorOfType(context.getDefinition(), 
				ICPPASTTemplateDeclaration.class) != null);
	}
}
