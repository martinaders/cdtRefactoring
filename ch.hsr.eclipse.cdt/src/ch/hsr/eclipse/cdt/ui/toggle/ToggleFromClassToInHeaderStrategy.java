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
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromClassToInHeaderStrategy implements ToggleRefactoringStrategy {

	protected TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private ToggleRefactoringContext fcontext;

	public ToggleFromClassToInHeaderStrategy(ToggleRefactoringContext context) {
		this.fcontext = context;
	}

	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(fcontext.getDefinitionUnit());
		CPPNodeFactory factory = new CPPNodeFactory();
		IASTFunctionDefinition oldDefinition = fcontext.getDefinition();
		IASTFunctionDeclarator funcdecl = oldDefinition.getDeclarator().copy();
		ICPPASTDeclSpecifier spec = (ICPPASTDeclSpecifier) oldDefinition.getDeclSpecifier().copy();
		IASTSimpleDeclaration newDeclaration = factory.newSimpleDeclaration(spec);
		newDeclaration.addDeclarator(funcdecl);
		newDeclaration.setParent(oldDefinition.getParent());

		InsertionPointFinder finder = new InsertionPointFinder(fcontext.getDefinitionUnit(), fcontext.getDefinitionUnit(), oldDefinition.getDeclarator());

		rewriter.remove(oldDefinition.getBody(), infoText);
		if (oldDefinition instanceof CPPASTFunctionWithTryBlock)
			rewriter.remove(((CPPASTFunctionWithTryBlock)oldDefinition).getCatchHandlers()[0], infoText);
		rewriter.replace(oldDefinition.getDeclarator(), new ASTLiteralNode("" + oldDefinition.getDeclarator().getRawSignature() + ";"), infoText);

		IASTNode newDefinition = ToggleNodeHelper.getQualifiedNameDefinition(true, 
				oldDefinition, fcontext.getDeclaration(), 
				fcontext.getDefinitionUnit());
		
		ICPPASTFunctionDefinition newDef;
		if (newDefinition instanceof ICPPASTFunctionDefinition)
			newDef=(ICPPASTFunctionDefinition)newDefinition;
		else
			newDef = (ICPPASTFunctionDefinition) ((ICPPASTTemplateDeclaration)newDefinition).getDeclaration();

		ASTRewrite newRw = rewriter.insertBefore(fcontext.getDefinitionUnit(), finder.getPosition(), 
				newDefinition, infoText);
		newRw.replace(newDef.getBody(), new ASTLiteralNode(oldDefinition.getBody().getRawSignature()), infoText);
	}

}
