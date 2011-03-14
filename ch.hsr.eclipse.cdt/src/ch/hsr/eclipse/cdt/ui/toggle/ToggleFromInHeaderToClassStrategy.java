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

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy implements IToggleRefactoringStrategy {

	private TextEditGroup infoText;
	private ToggleRefactoringContext context;

	public ToggleFromInHeaderToClassStrategy(ToggleRefactoringContext context) {
		if (isFreeFunction(context))
			throw new NotSupportedException("Cannot toggle templated free function");
		this.context = context;
		this.infoText =  new TextEditGroup("Toggle function body placement");
	}

	private boolean isFreeFunction(ToggleRefactoringContext context) {
		return isNotInsideAClass(context.getDefinition().getDeclarator(),
				context.getDeclaration());
	}
	
	boolean isNotInsideAClass(IASTFunctionDeclarator declarator, IASTFunctionDeclarator backup) {
		if (declarator.getName() instanceof ICPPASTQualifiedName) {
			declarator = backup;
		}
		return (ToggleNodeHelper.getAncestorOfType(declarator,
				IASTCompositeTypeSpecifier.class) == null);
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = removeDefinition(modifications);
		IASTFunctionDefinition newDefinition = getNewDefinition();
		ASTRewrite newRewriter = replaceDeclarationWithDefinition(rewriter, newDefinition);
		IASTNode parentTemplateDeclaration = 
			ToggleNodeHelper.getParentTemplateDeclaration(context.getDeclaration());
		if (parentTemplateDeclaration instanceof ICPPASTTemplateDeclaration) {
			restoreBody(newRewriter, newDefinition);
		} else {
			restoreBody(rewriter, newDefinition);
			restoreLeadingComments(rewriter, newDefinition);
		}
	}

	private ASTRewrite removeDefinition(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		IASTNode parentRemovePoint = ToggleNodeHelper.getParentRemovePoint(context.getDefinition());
		rewriter.remove(parentRemovePoint, infoText);
		return rewriter;
	}

	private IASTFunctionDefinition getNewDefinition() {
		IASTFunctionDefinition newDefinition = ToggleNodeHelper.createInClassDefinition(
				context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
		IASTNode parent = ToggleNodeHelper.getAncestorOfType(context.getDefinition(), 
				ICPPASTCompositeTypeSpecifier.class);
		if (parent != null) {
			newDefinition.setParent(parent);
		}
		else {
			newDefinition.setParent(context.getDefinitionUnit());
		}
		return newDefinition;
	}

	private ASTRewrite replaceDeclarationWithDefinition(ASTRewrite rewriter,
			IASTFunctionDefinition newDefinition) {
		IASTSimpleDeclaration fullDeclaration = ToggleNodeHelper.getAncestorOfType(
				context.getDeclaration(), CPPASTSimpleDeclaration.class);
		ASTRewrite newRewriter = rewriter.replace(fullDeclaration, newDefinition, infoText);
		return newRewriter;
	}

	private void restoreBody(ASTRewrite rewriter,
			IASTFunctionDefinition newDefinition) {
		String body = ToggleNodeHelper.getBody(context.getDefinition(), context.getDefinitionUnit());
		rewriter.replace(newDefinition.getBody(), new ASTLiteralNode(body), infoText);
	}

	private void restoreLeadingComments(ASTRewrite rewriter,
			IASTFunctionDefinition newDefinition) {
		String newDeclSpec = newDefinition.getDeclSpecifier().toString();
		String declarationLeading = ToggleNodeHelper.getLeadingComments(
				context.getDeclaration(), context.getDeclarationUnit());
		String definitionLeading = ToggleNodeHelper.getLeadingComments(
				context.getDefinition(), context.getDefinitionUnit());
		rewriter.replace(newDefinition.getDeclSpecifier(), new ASTLiteralNode(
				declarationLeading + definitionLeading + newDeclSpec), infoText);
	}
}
