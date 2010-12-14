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

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext context;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		if (isInClass(context.getDeclaration()) && isInClass(context.getDefinition()))
			throw new NotSupportedException("Definition and Declaration both inside class. Behavoir is undefined.");
		this.context = context;
	}

	private boolean isInClass(IASTNode node) {
		return ToggleNodeHelper.getAncestorOfType(node, 
				ICPPASTCompositeTypeSpecifier.class) != null;
	}

	public void run(ModificationCollector modifications) {
		IASTNode parentNamespace = getParentNamespace();
		IASTNode newDefinition = getNewDefinition(parentNamespace);
		IASTSimpleDeclaration newDeclaration = getNewDeclaration();
		ASTRewrite rewriter = replaceDefinitionWithDeclaration(modifications, newDeclaration);
		IASTNode insertion_point = getInsertionPoint(parentNamespace);
		ASTRewrite newRewriter = rewriter.insertBefore(parentNamespace, 
				insertion_point, newDefinition, infoText);
		restoreBody(newDefinition, newRewriter);
		restoreLeadingComments(newDeclaration, rewriter);
	}

	private IASTNode getNewDefinition(IASTNode parentNamespace) {
		IASTNode newDefinition = ToggleNodeHelper.getQualifiedNameDefinition(
				context.getDefinition(), context.getDefinitionUnit(), parentNamespace);
		
		ICPPASTTemplateDeclaration templdecl = ToggleNodeHelper.getTemplateDeclaration(
				context.getDefinition(), (IASTFunctionDefinition) newDefinition);
		if (templdecl != null) {
			newDefinition = templdecl;
		}
		newDefinition.setParent(context.getDefinitionUnit());
		return newDefinition;
	}

	private IASTNode getParentNamespace() {
		IASTNode parentNamespace = ToggleNodeHelper.getAncestorOfType(
				context.getDefinition(), ICPPASTNamespaceDefinition.class);
		if (parentNamespace == null)
			parentNamespace = context.getDefinitionUnit();
		return parentNamespace;
	}

	private IASTNode getInsertionPoint(IASTNode parentNamespace) {
		IASTTranslationUnit unit = parentNamespace.getTranslationUnit();
		IASTNode insertion_point = InsertionPointFinder.findInsertionPoint(
				unit, unit, context.getDefinition().getDeclarator());
		return insertion_point;
	}

	private ASTRewrite replaceDefinitionWithDeclaration(
			ModificationCollector modifications,
			IASTSimpleDeclaration newDeclaration) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(
				context.getDefinitionUnit());
		rewriter.replace(context.getDefinition(), newDeclaration, infoText);
		return rewriter;
	}

	private void restoreBody(IASTNode newDefinition, ASTRewrite newRewriter) {
		ICPPASTFunctionDefinition funcDefinition = ToggleNodeHelper.getFunctionDefinition(newDefinition);
		String bodyWithComments = ToggleNodeHelper.getBody(
				context.getDefinition(), context.getDefinitionUnit());
		newRewriter.replace(funcDefinition.getBody(), 
				new ASTLiteralNode(bodyWithComments), infoText);
	}

	private void restoreLeadingComments(IASTSimpleDeclaration newDeclaration,
			ASTRewrite rewriter) {
		String leadingDefComments = ToggleNodeHelper.getLeadingComments(
				context.getDefinition(), context.getDefinitionUnit());
		String leadingDecComments = ToggleNodeHelper.getLeadingComments(
				context.getDeclaration(), context.getDeclarationUnit());
		String newDeclSpec = newDeclaration.getDeclSpecifier().toString();
		rewriter.replace(newDeclaration.getDeclSpecifier(), new ASTLiteralNode(
				leadingDecComments + leadingDefComments + newDeclSpec), infoText);
	}

	private IASTSimpleDeclaration getNewDeclaration() {
		CPPNodeFactory factory = new CPPNodeFactory();
		IASTDeclSpecifier newDeclSpecifier = context.getDefinition().getDeclSpecifier().copy();
		newDeclSpecifier.setInline(false);
		IASTSimpleDeclaration newDeclaration = factory.newSimpleDeclaration(newDeclSpecifier);
		IASTFunctionDeclarator newDeclarator = context.getDefinition().getDeclarator().copy();
		newDeclaration.addDeclarator(newDeclarator);
		newDeclaration.setParent(context.getDefinition().getParent());
		return newDeclaration;
	}
}
