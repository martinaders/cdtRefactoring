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
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFreeFunctionFromInHeaderToImpl implements ToggleRefactoringStrategy {

	private IASTTranslationUnit sibling_tu;
	private TextEditGroup infoText = new TextEditGroup("Toggle function body placement");
	private final ToggleRefactoringContext context;
	private ASTLiteralNode includenode;

	public ToggleFreeFunctionFromInHeaderToImpl(ToggleRefactoringContext context) {
		this.context = context;
		if (isScopedFreeFunction())
			throw new NotSupportedException("namespaced+templated free functions not supported yet");
		sibling_tu = context.getTUForSiblingFile();
		if (sibling_tu == null) {
			ToggleFileCreator filecreator = new ToggleFileCreator(context, ".cpp");
			if (filecreator.askUserForFileCreation(context)) {
				filecreator.createNewFile();
				sibling_tu = filecreator.loadTranslationUnit();
				includenode = new ASTLiteralNode(filecreator.getIncludeStatement());
			} else {
				throw new NotSupportedException("Could not find sibling translation unit");
			}
		}
	}
	
	private boolean isScopedFreeFunction() {
		IASTFunctionDeclarator declarator = context.getDefinition().getDeclarator();
		if (declarator.getName() instanceof ICPPASTQualifiedName)
			declarator = context.getDeclaration();
		IASTNode node = declarator;
		while (node != null) {
			if (node instanceof ICPPASTNamespaceDefinition
					|| node instanceof ICPPASTTemplateDeclaration)
				return true;
			node = node.getParent();
		}
		return false;
	}
	
	@Override
	public void run(ModificationCollector modifications) {
		removeDefinitionFromHeader(modifications);
		addDefinitionToImplementation(modifications);
	}
	
	private void removeDefinitionFromHeader(ModificationCollector modifications) {
		ASTRewrite astrewriter = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		ASTLiteralNode declaration = new ASTLiteralNode(context.getDefinition().getDeclSpecifier().getRawSignature() + " " + context.getDefinition().getDeclarator().getRawSignature() + ";");
		astrewriter.replace(context.getDefinition(), declaration, infoText);
	}

	private void addDefinitionToImplementation(
			ModificationCollector modifications) {
		ASTRewrite otherrewrite = modifications.rewriterForTranslationUnit(sibling_tu);
		if (includenode != null) {
			otherrewrite.insertBefore(sibling_tu.getTranslationUnit(), null, includenode, infoText);
		}
		IASTFunctionDefinition newDefinition = context.getDefinition().copy();
		ASTRewrite newRw = otherrewrite.insertBefore(sibling_tu.getTranslationUnit(), null,
				newDefinition, infoText);
		newRw.replace(newDefinition.getBody(), new ASTLiteralNode(context.getDefinition().getBody().getRawSignature()), infoText);
	}
}
