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

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.core.dom.rewrite.commenthandler.ASTCommenter;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext fcontext;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		if (ToggleNodeHelper.getParentCompositeTypeSpecifier(context
				.getDefinition()) != null
				&& ToggleNodeHelper.getParentCompositeTypeSpecifier(context
						.getDeclaration()) != null)
			throw new NotSupportedException("behavior when def + decl both inside a class is undefined");
		this.fcontext = context;
	}

	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(fcontext.getDefinitionUnit());
		IASTFunctionDeclarator funcdecl = fcontext.getDefinition().getDeclarator().copy();
		ICPPASTDeclSpecifier spec = (ICPPASTDeclSpecifier) fcontext.getDefinition().getDeclSpecifier().copy();
		
		CPPNodeFactory factory = new CPPNodeFactory();
		
		IASTSimpleDeclaration simpledec = factory.newSimpleDeclaration(spec);
		simpledec.addDeclarator(funcdecl);
		IASTNode par = fcontext.getDefinition().getParent();
		simpledec.setParent(par);

		IASTNode parent_ns = getParentNamespace(fcontext.getDefinition());
		IASTTranslationUnit unit = parent_ns.getTranslationUnit();
		
		rewriter.replace(fcontext.getDefinition(), simpledec, infoText);
		String declSpecString = fcontext.getDefinition().getDeclSpecifier().getRawSignature();
		String declarationString = declSpecString + " " + fcontext.getDefinition().getDeclarator().getRawSignature();
		ArrayList<IASTComment> leadingComments = ASTCommenter.getCommentedNodeMap(fcontext.getDefinitionUnit()).getLeadingCommentsForNode(fcontext.getDefinition());
		Collections.reverse(leadingComments);
		for (IASTComment c : leadingComments)
			declarationString = format(c.getRawSignature() + "\n    " + declarationString);

		rewriter.replace(simpledec.getDeclSpecifier(), new ASTLiteralNode(""), infoText);
		rewriter.replace(simpledec.getDeclarators()[0], new ASTLiteralNode(format(declarationString)), infoText);

		IASTNode insertion_point = InsertionPointFinder.findInsertionPoint(unit, unit, fcontext.getDefinition().getDeclarator());
		IASTNode newDefinition = ToggleNodeHelper.getQualifiedNameDefinition(fcontext.getDefinition(), fcontext.getDefinitionUnit(), parent_ns);
		ASTRewrite newRw = rewriter.insertBefore(parent_ns, insertion_point, newDefinition, infoText);
		ICPPASTFunctionDefinition functionDefinition = null;
		functionDefinition = findFuncDef(newDefinition);
		newRw.replace(functionDefinition.getBody(), new ASTLiteralNode(format(fcontext.getDefinition().getBody().getRawSignature())), infoText);
		if (functionDefinition instanceof ICPPASTFunctionWithTryBlock) {
			ICPPASTCatchHandler[] newCatches = ((ICPPASTFunctionWithTryBlock)functionDefinition).getCatchHandlers();
			ICPPASTCatchHandler[] oldCatches = ((ICPPASTFunctionWithTryBlock)fcontext.getDefinition()).getCatchHandlers();
			for (int i = 0; i < oldCatches.length; i++)
				newRw.replace(newCatches[i], new ASTLiteralNode(format(oldCatches[i].getRawSignature())), infoText);
		}
	}

	private ICPPASTFunctionDefinition findFuncDef(IASTNode newDefinition) {
		if (newDefinition instanceof ICPPASTFunctionDefinition)
			return (ICPPASTFunctionDefinition) newDefinition;
		IASTNode node = newDefinition;
		while (node != null && node instanceof ICPPASTTemplateDeclaration) {
			IASTDeclaration declaration = ((ICPPASTTemplateDeclaration)node).getDeclaration();
			node = declaration;
			if (declaration instanceof ICPPASTFunctionDefinition)
				return (ICPPASTFunctionDefinition) declaration;
		}
		return null;
	}

	private String format(String rawSignature) {
		String result = rawSignature.replace("\n        ", "\n    ");
		result = result.replace("\n    	", "\n    ");
		result = result.replace("\n	}", "\n}");
		result = result.replace("\n    }", "\n}");
		result = result.replace(") \n    {", ") {");
		result = result.replace(")\n    {", ") {");
		result = result.replace("        return;", "    return;");
		return result;
	}

	private IASTNode getParentNamespace(IASTNode node) {
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTNamespaceDefinition)
				return node;
		}
		return fcontext.getDefinitionUnit();
	}
}
