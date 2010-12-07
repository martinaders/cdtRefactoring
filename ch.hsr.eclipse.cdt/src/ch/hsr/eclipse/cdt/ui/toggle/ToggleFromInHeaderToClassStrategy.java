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
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
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
		declaration = getParentSimpleDeclaration(context.getDeclaration());
		if (declaration == null) {
			throw new NotSupportedException("Parent needs to be a SimpleDeclaration.");
		}
	}

	private CPPASTSimpleDeclaration getParentSimpleDeclaration(IASTFunctionDeclarator node) {
		if (node == null || node.getParent() == null || !(node.getParent() instanceof CPPASTSimpleDeclaration))
			return null;
		return (CPPASTSimpleDeclaration) node.getParent();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(context
				.getDefinitionUnit());
		rewriter.remove(ToggleNodeHelper.getParentRemovePoint(context.getDefinition()), infoText);
		IASTFunctionDefinition newDefinition = ToggleNodeHelper.createInClassDefinition( context.getDeclaration(), 
				context.getDefinition(), context.getDefinitionUnit());
		rewriter.replace(context.getDeclaration().getParent(), newDefinition, infoText);
		rewriter.replace(newDefinition.getBody(), new ASTLiteralNode(format(context.getDefinition().getBody().getRawSignature())), infoText);
		rewriteCatchHandlers(rewriter, newDefinition);

		String declarationString = format(addLeadingComments(declaration));
		rewriter.replace(newDefinition.getDeclSpecifier(), new ASTLiteralNode(declarationString), infoText);
	}

	private String addLeadingComments(CPPASTSimpleDeclaration decl) {
		String code = decl.getDeclSpecifier().toString();
		ArrayList<IASTComment> leadingComments = ASTCommenter.getCommentedNodeMap(context.getDeclarationUnit()).getLeadingCommentsForNode(decl);
		Collections.reverse(leadingComments);
		for (IASTComment c : leadingComments)
			code = c.getRawSignature() + "\n    " + code;
		return code;
	}

	private void rewriteCatchHandlers(ASTRewrite rewriter, IASTFunctionDefinition newDefinition) {
		if (newDefinition instanceof CPPASTFunctionWithTryBlock) {
			ICPPASTCatchHandler[] newCatches = ((CPPASTFunctionWithTryBlock)newDefinition).getCatchHandlers();
			ICPPASTCatchHandler[] oldCatches = ((CPPASTFunctionWithTryBlock)context.getDefinition()).getCatchHandlers();
			for (int i = 0; i < oldCatches.length; i++)
				rewriter.replace(newCatches[i], new ASTLiteralNode(" " + format(oldCatches[i].getRawSignature())), infoText);
		}
	}

	private String format(String rawSignature) {
		String result = rawSignature.replace("\n", "\n    ");
		return result;
	}
}
