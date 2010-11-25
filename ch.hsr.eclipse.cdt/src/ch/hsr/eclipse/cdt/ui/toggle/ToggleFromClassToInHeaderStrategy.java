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

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.core.dom.rewrite.commenthandler.ASTCommenter;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext fcontext;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		this.fcontext = context;
	}

	/*
	 * Written with a close look at ExtractFunctionRefactoring.addMethod and
	 * ExtractFunctionRefactoring.findExtractableNodes. When copying every
	 * expression/statement separately, comments inside the moved function body
	 * will be preserved by the rewriter.
	 */
	public void run(ModificationCollector modifications) {
		CPPNodeFactory factory = new CPPNodeFactory();
		IASTFunctionDeclarator funcdecl = fcontext.getDefinition().getDeclarator().copy();
		ICPPASTDeclSpecifier spec = (ICPPASTDeclSpecifier) fcontext.getDefinition().getDeclSpecifier().copy();
		CPPASTSimpleDeclaration simpledec = (CPPASTSimpleDeclaration) factory.newSimpleDeclaration(spec);
		simpledec.addDeclarator(funcdecl);
		simpledec.setParent(fcontext.getDefinition().getParent());
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(fcontext.getDefinitionUnit());
		ASTCommenter.map.addCommentMapping(fcontext.getDefinition(), simpledec);
		rewriter.replace(fcontext.getDefinition(), simpledec, infoText);

		IASTNode newDefinition = ToggleNodeHelper.getQualifiedNameDefinition(true, 
				fcontext.getDefinition(), fcontext.getDeclaration(), 
				fcontext.getDefinitionUnit());
		ToggleNodeHelper.remapAllComments(fcontext.getDefinition(), newDefinition);

		InsertionPointFinder finder = new InsertionPointFinder(fcontext.getDefinitionUnit(), fcontext.getDefinitionUnit(), fcontext.getDefinition().getDeclarator());
		rewriter.insertBefore(fcontext.getDefinitionUnit(), finder.getPosition(), 
				newDefinition,infoText);
	}

}
