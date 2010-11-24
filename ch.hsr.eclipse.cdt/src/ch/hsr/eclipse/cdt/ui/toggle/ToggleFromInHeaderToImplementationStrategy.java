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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.internal.UIPlugin;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToImplementationStrategy implements ToggleRefactoringStrategy {

	private IASTTranslationUnit siblingtu;
	private ToggleRefactoringContext context;
	private TextEditGroup infoText;
	private boolean newfile;

	public ToggleFromInHeaderToImplementationStrategy(ToggleRefactoringContext context) throws CModelException, CoreException {
		this.infoText = new TextEditGroup("Toggle function body placement");
		this.context = context;
		this.siblingtu = context.getTUForSiblingFile();
		if (this.siblingtu == null) {
			Shell shell = UIPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getShell();
			newfile = MessageDialog.openQuestion(shell, "Create new implementation file?", "Create a new file named: " + getNewFileName() + " and move " + context.getDeclaration().getRawSignature() + " there?");
		}
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		ASTRewrite otherrewrite = modifications.rewriterForTranslationUnit(siblingtu);
		rewriter.remove(ToggleNodeHelper.getParentRemovePoint(context.getDefinition()), infoText);
		
		IASTFunctionDefinition newImpldef = copyDefinitionFromInHeader();
		if (this.siblingtu != null) {
			IASTNode parent_ns = getParentNamespace(context.getDeclaration());
			IASTNode parent = null;
			if (parent_ns instanceof ICPPASTNamespaceDefinition) {
				parent = searchForNamespace(((ICPPASTNamespaceDefinition) parent_ns).getName());
				if (parent == null) {//need to create namespace
					CPPASTNamespaceDefinition nsdef = new CPPASTNamespaceDefinition(((ICPPASTNamespaceDefinition) parent_ns).getName().copy());
					nsdef.setParent(siblingtu);
					ASTRewrite sub = otherrewrite.insertBefore(siblingtu.getTranslationUnit(), null, nsdef, infoText);
					parent = nsdef;
					newImpldef.setParent(nsdef);
					sub.insertBefore(parent, null, newImpldef, infoText);
					return;
				}
			}
			else {
				parent = siblingtu.getTranslationUnit();
			}
			InsertionPointFinder finder = new InsertionPointFinder(context.getDeclarationUnit(), parent.getTranslationUnit(), context.getDeclaration());
			otherrewrite.insertBefore(parent, finder.getPosition(), newImpldef, infoText);
			return;
		}
		if (newfile) {
			try {
				//set a parent or will not be able to print the signature
				newImpldef.setParent(context.getDeclarationUnit());
				CreateFileChange change = createNewImplementationFile(
						getNewFileContent(newImpldef.getRawSignature()), getNewFileName());
				modifications.addFileChange(change);			
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	private IASTNode searchForNamespace(final IASTName name) {
		final Container<IASTNode> result = new Container<IASTNode>();
		this.siblingtu.accept(
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
	
	private String getNewFileName() {
		return ToggleNodeHelper.getFilenameWithoutExtension(
				context.getSelectionFile().getFullPath().toString()) + ".cpp";
	}
	
	private IASTFunctionDefinition copyDefinitionFromInHeader() {
		IASTFunctionDefinition newImpldef = context.getDefinition().copy();
		newImpldef.getDeclSpecifier().setInline(false);
		return newImpldef;
	}

	private String getNewFileContent(String definition) {
		String originfile = ToggleNodeHelper.getFilenameWithoutExtension(
				context.getSelectionFile().getFullPath().toString()) + ".h";
		return "#include " + "\"" + originfile + "\"\n\n" + definition;
	}
	
	private CreateFileChange createNewImplementationFile(String content, String filename)
			throws CoreException {
		CreateFileChange change;
		change = new CreateFileChange(filename, new	Path(getPath()+filename), 
				content, context.getSelectionFile().getCharset());
		return change;
	}

	private String getPath() {
		String result = context.getSelectionFile().getFullPath().toString();
		return result.replaceAll("(\\w)*\\.(\\w)*", "");
	}
}
