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
		ASTRewrite header_rewrite = modifications.rewriterForTranslationUnit(other_tu);
		IASTFunctionDefinition newfunction = context.getDefinition().copy();
		newfunction.setParent(other_tu);
		header_rewrite.insertBefore(other_tu.getTranslationUnit(), null, newfunction, infoText);
	}

	private void addDefinitionToClass(ModificationCollector modifications) {
		ASTRewrite headerast = modifications.rewriterForTranslationUnit(context.getDeclarationUnit());
		IASTFunctionDefinition newdefinition = ToggleNodeHelper.createInClassDefinition(
				context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
		headerast.replace(context.getDeclaration().getParent(), newdefinition, infoText);
		ToggleNodeHelper.restoreBody(headerast, newdefinition, context.getDefinition(), context.getDefinitionUnit(), infoText);
		ToggleNodeHelper.restoreLeadingComments(
				headerast, newdefinition, 
				context.getDefinition(), context.getDefinitionUnit(),
				context.getDeclaration(), context.getDeclarationUnit(),
				infoText);
	}
	
	private void removeDefinitionFromImplementation(
			ModificationCollector modifications) {
		ASTRewrite implast = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		ICPPASTNamespaceDefinition ns = getNamespaceDefinition(context.getDefinition());
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

	private ICPPASTNamespaceDefinition getNamespaceDefinition(IASTNode node) {
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTNamespaceDefinition)
				return (ICPPASTNamespaceDefinition) node;
		}
		return null;
	}
}
