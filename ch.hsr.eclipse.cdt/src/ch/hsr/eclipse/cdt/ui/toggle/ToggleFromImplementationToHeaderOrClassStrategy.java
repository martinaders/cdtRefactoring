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
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToHeaderOrClassStrategy implements ToggleRefactoringStrategy {

	private ToggleRefactoringContext context;
	protected TextEditGroup infoText;
	private IASTTranslationUnit other_tu;
	private ASTLiteralNode includenode;

	public ToggleFromImplementationToHeaderOrClassStrategy(
			ToggleRefactoringContext context) {
		this.context = context;
		this.infoText = new TextEditGroup("Toggle function body placement");
		if (context.getDeclarationUnit() == null) {
			if (context.getDefinition().getDeclarator().getName() instanceof ICPPASTQualifiedName)
				throw new NotSupportedException("Not a free function. Cannot decide where to toggle");
			other_tu = context.getTUForSiblingFile();
			if (other_tu == null) {
				ToggleFileCreator filecreator = new ToggleFileCreator(context, ".h");
				if (filecreator.askUserForFileCreation(context)) {
					filecreator.createNewFile();
					other_tu = filecreator.loadTranslationUnit();
					includenode = new ASTLiteralNode(filecreator.getIncludeStatement() + "\n\n");
				} else {
					throw new NotSupportedException("Cannot create new File");
				}
			}
		}
	}
	
	@Override
	public void run(ModificationCollector modifications) {
		removeDefinitionFromImplementation(modifications);
		if (context.getDeclarationUnit() != null)
			addDefinitionToClass(modifications);
		else
			addDefinitionToHeader(modifications);
	}

	private void addDefinitionToHeader(ModificationCollector modifications) {
		ASTRewrite headerRewrite = modifications.rewriterForTranslationUnit(other_tu);
		IASTFunctionDefinition newDefinition = ToggleNodeHelper.createFunctionSignatureWithEmptyBody(
				context.getDefinition().getDeclSpecifier().copy(), context.getDefinition().getDeclarator().copy(), context.getDefinition().copy());
		newDefinition.setParent(other_tu);
		ASTRewrite newRewriter = headerRewrite.insertBefore(other_tu.getTranslationUnit(), null, newDefinition, infoText);
		
		restoreBody(newRewriter, newDefinition);
		restoreLeadingComments(newRewriter, newDefinition);
	}

	private void addDefinitionToClass(ModificationCollector modifications) {
		ASTRewrite headerRewrite = modifications.rewriterForTranslationUnit(context.getDeclarationUnit());
		IASTFunctionDefinition newDefinition = ToggleNodeHelper.createInClassDefinition(
				context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
		IASTNode parent = ToggleNodeHelper.getAncestorOfType(context.getDefinition(), ICPPASTCompositeTypeSpecifier.class);
		if (parent != null)
			newDefinition.setParent(parent);
		else
			newDefinition.setParent(context.getDeclarationUnit());
		
		headerRewrite.replace(context.getDeclaration().getParent(), newDefinition, infoText);
		
		restoreBody(headerRewrite, newDefinition);
		restoreLeadingComments(headerRewrite, newDefinition);
	}

	private void restoreLeadingComments(ASTRewrite headerRewrite,
			IASTFunctionDefinition newDefinition) {
		String leading = ToggleNodeHelper.restoreLeadingComments(newDefinition,
				context.getDefinition(), context.getDefinitionUnit(),
				context.getDeclaration(), context.getDeclarationUnit());
		if (leading != null) {
			headerRewrite.replace(newDefinition.getDeclSpecifier(), new ASTLiteralNode(leading), infoText);
		}
	}

	private void restoreBody(ASTRewrite headerRewrite,
			IASTFunctionDefinition newDefinition) {
		String body = ToggleNodeHelper.restoreBody(context.getDefinition(), context.getDefinitionUnit());
		headerRewrite.replace(newDefinition.getBody(), new ASTLiteralNode(body), infoText);
	}
	
	private void removeDefinitionFromImplementation(
			ModificationCollector modifications) {
		ASTRewrite implast = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		ICPPASTNamespaceDefinition ns = ToggleNodeHelper.getAncestorOfType(context.getDefinition(), ICPPASTNamespaceDefinition.class);
		if (ns != null && isSingleElementInNamespace(ns, context.getDefinition()))
			implast.remove(ns, infoText);
		else
			implast.remove(context.getDefinition(), infoText);
		if (includenode != null) {
			implast.insertBefore(context.getDefinitionUnit(), context.getDefinitionUnit().getChildren()[0], includenode, infoText);
		}
	}

	private boolean isSingleElementInNamespace(ICPPASTNamespaceDefinition ns,
			IASTFunctionDefinition definition) {
		return ns.getChildren().length == 2 && (ns.contains(definition));
	}
}
