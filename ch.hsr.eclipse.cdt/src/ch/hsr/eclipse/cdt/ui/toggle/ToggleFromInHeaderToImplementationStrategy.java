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
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
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
public class ToggleFromInHeaderToImplementationStrategy implements IToggleRefactoringStrategy {

	private IASTTranslationUnit impl_unit;
	private ToggleRefactoringContext context;
	private TextEditGroup infoText;
	private ASTLiteralNode includenode;

	public ToggleFromInHeaderToImplementationStrategy(final ToggleRefactoringContext context) {
		this.infoText = new TextEditGroup("Toggle function body placement");
		this.context = context;
	}

	@Override
	public void run(ModificationCollector collector) {
		newFileCheck();
		ICPPASTFunctionDefinition newDefinition = getNewDefinition();
		if (context.getDeclaration() != null) {
			removeDefinitionFromHeader(collector);
		}
		else {
			replaceDefinitionWithDeclaration(collector);
		}

		ASTRewrite implRewrite = collector.rewriterForTranslationUnit(impl_unit);
		if (includenode != null) {
			implRewrite.insertBefore(impl_unit, null, includenode, infoText);
		}
		
		IASTNode insertionParent = null;
		ICPPASTNamespaceDefinition parent = getParentNamespace();
		
		if (parent != null) {
			adaptQualifiedNameToNamespaceLevel(newDefinition, parent);
			insertionParent = searchNamespaceInImplementation(parent.getName());
			if (insertionParent == null) {
				insertionParent = createNamespace(parent);
				implRewrite = implRewrite.insertBefore(impl_unit.getTranslationUnit(), 
						null, insertionParent, infoText);
			}
		}
		else {
			insertionParent = impl_unit.getTranslationUnit();
		}
		
		newDefinition.setParent(insertionParent);
		
		IASTNode insertionPoint = findInsertionPoint(insertionParent, 
				context.getDeclarationUnit());
		ASTRewrite newRewriter = implRewrite.insertBefore(insertionParent, 
				insertionPoint, newDefinition, infoText);
		
		restoreBody(newDefinition, newRewriter);
		restoreLeadingComments(newDefinition, newRewriter);
	}

	private void newFileCheck() {
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

	private ICPPASTNamespaceDefinition getParentNamespace() {
		IASTNode toquery = context.getDeclaration();
		if (toquery == null) {
			toquery = context.getDefinition();
		}
		return ToggleNodeHelper.getAncestorOfType(toquery, ICPPASTNamespaceDefinition.class);
	}

	private IASTNode findInsertionPoint(IASTNode insertionParent,
			IASTTranslationUnit unit) {
		IASTFunctionDeclarator declarator = context.getDeclaration();
		if (unit == null) {
			unit = context.getDefinitionUnit();
		}
		if (declarator == null) {
			declarator = context.getDefinition().getDeclarator();
		}
		IASTNode insertion_point = InsertionPointFinder.findInsertionPoint(
				unit, insertionParent.getTranslationUnit(), declarator);
		return insertion_point;
	}

	private void restoreBody(ICPPASTFunctionDefinition newDefinition,
			ASTRewrite newRewriter) {
		String body = ToggleNodeHelper.getBody(context.getDefinition(), context.getDefinitionUnit());
		newRewriter.replace(newDefinition.getBody(), new ASTLiteralNode(body), infoText);
	}

	private void restoreLeadingComments(
			ICPPASTFunctionDefinition newDefinition, ASTRewrite newRewriter) {
		String definitionLeading = ToggleNodeHelper.getLeadingComments(
				context.getDefinition(), context.getDefinitionUnit());
		String newDeclSpec = newDefinition.getDeclSpecifier().toString();
		newRewriter.replace(newDefinition.getDeclSpecifier(),
				new ASTLiteralNode(definitionLeading + newDeclSpec), infoText);
	}

	private void replaceDefinitionWithDeclaration(
			ModificationCollector collector) {
		IASTSimpleDeclaration newdeclarator = 
			ToggleNodeHelper.createDeclarationFromDefinition(context.getDefinition());
		ASTRewrite rewrite = collector.rewriterForTranslationUnit(context.getDefinitionUnit());
		rewrite.replace(context.getDefinition(), newdeclarator, infoText);
	}

	private ICPPASTFunctionDefinition getNewDefinition() {
		ICPPASTFunctionDefinition newDefinition =
			ToggleNodeHelper.createFunctionSignatureWithEmptyBody(
					context.getDefinition().getDeclSpecifier().copy(), 
					context.getDefinition().getDeclarator().copy(), 
					context.getDefinition().copy());
		newDefinition.getDeclSpecifier().setInline(false);
		return newDefinition;
	}

	private void adaptQualifiedNameToNamespaceLevel(
			IASTFunctionDefinition new_definition, IASTNode parent) {
		if (parent instanceof ICPPASTNamespaceDefinition) {
			ICPPASTNamespaceDefinition ns = (ICPPASTNamespaceDefinition) parent;
			if (new_definition.getDeclarator().getName() instanceof ICPPASTQualifiedName) {
				ICPPASTQualifiedName qname = 
					(ICPPASTQualifiedName) new_definition.getDeclarator().getName();
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

	private CPPASTNamespaceDefinition createNamespace(
			ICPPASTNamespaceDefinition parent_namespace) {
		CPPASTNamespaceDefinition insertionParent = new CPPASTNamespaceDefinition(
				parent_namespace.getName().copy());
		insertionParent.setParent(impl_unit);
		return insertionParent;
	}

	private void removeDefinitionFromHeader(ModificationCollector collector) {
		ASTRewrite header_rewrite = collector.rewriterForTranslationUnit(
				context.getDefinitionUnit());
		header_rewrite.remove(ToggleNodeHelper.getParentRemovePoint(context.getDefinition()), infoText);
	}

	private IASTNode searchNamespaceInImplementation(final IASTName name) {
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
}
