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
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.core.dom.rewrite.commenthandler.ASTCommenter;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy implements
		ToggleRefactoringStrategy {

	private TextEditGroup infoText;
	private ToggleRefactoringContext context;
	private CPPASTSimpleDeclaration declaration;

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
		IASTNode parentRemovePoint = ToggleNodeHelper.getParentRemovePoint(context.getDefinition());
		rewriter.remove(parentRemovePoint, infoText);
		IASTFunctionDefinition newDefinition = ToggleNodeHelper.createInClassDefinition( context.getDeclaration(), 
				context.getDefinition(), context.getDefinitionUnit());
		IASTSimpleDeclaration fullDeclaration = ToggleNodeHelper.getSimpleDeclaration(context.getDeclaration());
		ASTRewrite newRewriter = rewriter.replace(fullDeclaration,
				newDefinition, infoText);
		IASTNode parentTemplateDeclaration = ToggleNodeHelper.getParentTemplateDeclaration(context.getDeclaration());
		if (parentTemplateDeclaration instanceof ICPPASTTemplateDeclaration) {
			ToggleNodeHelper.restoreBody(newRewriter, newDefinition, context.getDefinition(), context.getDefinitionUnit(), infoText);
		} else {
			ToggleNodeHelper.restoreBody(rewriter, newDefinition, context.getDefinition(), context.getDefinitionUnit(), infoText);
			
			ToggleNodeHelper.restoreLeadingComments(
					rewriter, newDefinition, 
					context.getDefinition(), context.getDefinitionUnit(),
					context.getDeclaration(), context.getDeclarationUnit(),
					infoText);
		}
	}
}
