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
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext fcontext;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		if (ToggleNodeHelper.getAncestorOfType(context.getDefinition(), ICPPASTCompositeTypeSpecifier.class) != null
			    && ToggleNodeHelper.getAncestorOfType(context.getDeclaration(), ICPPASTCompositeTypeSpecifier.class) != null)
			throw new NotSupportedException("behavior when def + decl both inside a class is undefined");
		this.fcontext = context;
	}

	public void run(ModificationCollector modifications) {
		IASTSimpleDeclaration newDeclaration = getNewDeclaration();
		
		IASTNode parentNamespace = ToggleNodeHelper.getAncestorOfType(
				fcontext.getDefinition(), ICPPASTNamespaceDefinition.class);
		if (parentNamespace == null)
			parentNamespace = fcontext.getDefinitionUnit();
		
		IASTNode newDefinition = ToggleNodeHelper.getQualifiedNameDefinition(
				fcontext.getDefinition(), fcontext.getDefinitionUnit(), parentNamespace);
		IASTTranslationUnit unit = parentNamespace.getTranslationUnit();
		IASTNode insertion_point = InsertionPointFinder.findInsertionPoint(
				unit, unit, fcontext.getDefinition().getDeclarator());

		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(
				fcontext.getDefinitionUnit());
		rewriter.replace(fcontext.getDefinition(), newDeclaration, infoText);
		ASTRewrite newRewriter = rewriter.insertBefore(
				parentNamespace, insertion_point, newDefinition, infoText);

		ICPPASTFunctionDefinition funcDefinition = ToggleNodeHelper.getFunctionDefinition(newDefinition);
		String bodyWithComments = ToggleNodeHelper.restoreBody(
				fcontext.getDefinition(), fcontext.getDefinitionUnit());
		newRewriter.replace(funcDefinition.getBody(), new ASTLiteralNode(bodyWithComments), infoText);
		String leading = ToggleNodeHelper.restoreLeadingComments(fcontext.getDefinition(), fcontext.getDefinitionUnit());
		System.out.println("leading: " + leading);
		if (leading != null) {
			rewriter.replace(newDeclaration.getDeclSpecifier(), new ASTLiteralNode(leading), infoText);
		}
	}

	private IASTSimpleDeclaration getNewDeclaration() {
		CPPNodeFactory factory = new CPPNodeFactory();
		IASTDeclSpecifier newDeclSpecifier = fcontext.getDefinition().getDeclSpecifier().copy();
		newDeclSpecifier.setInline(false);
		IASTSimpleDeclaration newDeclaration = factory.newSimpleDeclaration(newDeclSpecifier);
		IASTFunctionDeclarator newDeclarator = fcontext.getDefinition().getDeclarator().copy();
		newDeclaration.addDeclarator(newDeclarator);
		newDeclaration.setParent(fcontext.getDefinition().getParent());
		return newDeclaration;
	}
}
