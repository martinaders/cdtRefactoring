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
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToImplementationStrategy implements ToggleRefactoringStrategy {

	private IASTTranslationUnit impl_unit;
	private ToggleRefactoringContext context;
	private TextEditGroup infoText;
	private ASTLiteralNode includenode;

	public ToggleFromInHeaderToImplementationStrategy(final ToggleRefactoringContext context) {
		this.infoText = new TextEditGroup("Toggle function body placement");
		this.context = context;
		impl_unit = context.getTUForSiblingFile();
		if (this.impl_unit == null) {
			ToggleFileCreator filecreator = new ToggleFileCreator(context, ".cpp");
			if (filecreator.askUserForFileCreation(context)) {
				filecreator.createNewFile();
				impl_unit = filecreator.loadTranslationUnit();
				includenode = new ASTLiteralNode(filecreator.getIncludeStatement());
			}
			else {
				throw new NotSupportedException("Cannot create new Implementation File");
			}
		}
	}

	@Override
	public void run(ModificationCollector collector) {
		IASTFunctionDefinition new_definition = copyDefinitionFromInHeader();
		removeDefinitionFromHeader(collector);

		ASTRewrite impl_rewrite = collector.rewriterForTranslationUnit(impl_unit);
		if (includenode != null) {
			impl_rewrite.insertBefore(impl_unit, null, includenode, infoText);
		}
		
		IASTNode insertion_parent = null;
		IASTNode parent = getParentNamespace(context.getDeclaration());
		if (parent instanceof ICPPASTNamespaceDefinition) {
			adaptQualifiedNameToNamespaceLevel(new_definition, parent);
			ICPPASTNamespaceDefinition parent_namespace = (ICPPASTNamespaceDefinition) parent; 
			insertion_parent = searchNamespaceInImpl(parent_namespace.getName());
			if (insertion_parent == null) {
				insertion_parent = createNamespace(parent_namespace);
				insertion_parent.setParent(impl_unit);
				impl_rewrite = impl_rewrite.insertBefore(impl_unit.getTranslationUnit(), null, insertion_parent, infoText);
			}
		}
		else {
			insertion_parent = impl_unit.getTranslationUnit();
		}
		addToImplementationFile(new_definition, insertion_parent, impl_rewrite);	
	}
	
	private IASTFunctionDefinition copyDefinitionFromInHeader() {
		IASTFunctionDefinition newImpldef = context.getDefinition().copy();
		newImpldef.getDeclSpecifier().setInline(false);
		return newImpldef;
	}

	private void adaptQualifiedNameToNamespaceLevel(
			IASTFunctionDefinition new_definition, IASTNode parent) {
		if (parent instanceof ICPPASTNamespaceDefinition) {
			ICPPASTNamespaceDefinition ns = (ICPPASTNamespaceDefinition) parent;
			if (new_definition.getDeclarator().getName() instanceof ICPPASTQualifiedName) {
				ICPPASTQualifiedName qname = (ICPPASTQualifiedName) new_definition.getDeclarator().getName();
				ICPPASTQualifiedName qname_new = new CPPASTQualifiedName();
				boolean start = false;
				for(IASTName partname: qname.getNames()) {
					if (partname.toString().equals(ns.getName().toString())) {
						start = true;
						continue;
					}
					if (start)
						qname_new.addName(partname);
				}
				if (start)
					new_definition.getDeclarator().setName(qname_new);
			}
		}
	}

	private void addToImplementationFile(IASTFunctionDefinition new_definition,
			IASTNode insertion_parent, ASTRewrite impl_rewrite) {
		new_definition.setParent(insertion_parent);
		IASTNode insertion_point = InsertionPointFinder.findInsertionPoint(
				context.getDeclarationUnit(), insertion_parent.getTranslationUnit(), 
				context.getDeclaration());
		ASTRewrite newRw = impl_rewrite.insertBefore(insertion_parent, insertion_point, new_definition, infoText);
		String bodyString = context.getDefinition().getBody().getRawSignature();
		newRw.replace(new_definition.getBody(), new ASTLiteralNode(bodyString), infoText);
		if (context.getDefinition() instanceof ICPPASTFunctionWithTryBlock) {
			ICPPASTCatchHandler[] oldCatches = ((ICPPASTFunctionWithTryBlock)context.getDefinition()).getCatchHandlers();
			ICPPASTCatchHandler[] newCatches = ((ICPPASTFunctionWithTryBlock)new_definition).getCatchHandlers();
			for (int i = 0; i < newCatches.length; i++)
				newRw.replace(newCatches[i], new ASTLiteralNode(" " + oldCatches[i].getRawSignature()), infoText);
		}
	}

	private CPPASTNamespaceDefinition createNamespace(
			ICPPASTNamespaceDefinition parent_namespace) {
		return new CPPASTNamespaceDefinition(parent_namespace.getName().copy());
	}

	private void removeDefinitionFromHeader(ModificationCollector collector) {
		ASTRewrite header_rewrite = collector.rewriterForTranslationUnit(
				context.getDefinitionUnit());
		header_rewrite.remove(ToggleNodeHelper.getParentRemovePoint(context.getDefinition()), infoText);
	}

	private IASTNode searchNamespaceInImpl(final IASTName name) {
		final Container<IASTNode> result = new Container<IASTNode>();
		this.impl_unit.accept(
				new CPPASTVisitor() {
					{
						shouldVisitNamespaces = true;
					}
					@Override
					public int visit(ICPPASTNamespaceDefinition namespaceDefinition) {
						if (name.toString().equals(namespaceDefinition.getName().toString())) {
							result.setObject(namespaceDefinition);
							return PROCESS_ABORT;
						}
						return super.visit(namespaceDefinition);
					}
		});
		return result.getObject();
	}

	private IASTNode getParentNamespace(IASTNode node) {
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTNamespaceDefinition)
				return node;
		}
		return context.getDefinitionUnit();
	}
}
