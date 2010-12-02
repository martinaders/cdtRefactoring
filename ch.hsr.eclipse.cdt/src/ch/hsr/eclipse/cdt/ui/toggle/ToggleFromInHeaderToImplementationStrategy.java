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
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamespaceDefinition;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTLiteralNode;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.UIPlugin;

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
			if (askUserForFileCreation(context)) {
				createNewImplementationFile();
				impl_unit = loadTranslationUnit();
				includenode = new ASTLiteralNode(getIncludeStatement());
			}
			else {
				throw new NotSupportedException("Cannot create new Implementation File");
			}
		}
	}

	private IASTTranslationUnit loadTranslationUnit() {
		String filename = context.getDeclaration().getContainingFilename();
		filename = filename.replaceAll("\\w*.h$", "");
		filename = filename + ToggleNodeHelper.getFilenameWithoutExtension(getNewFileName()) + ".cpp";
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(filename));
		IASTTranslationUnit result = null;
		try {
			result = CoreModelUtil.findTranslationUnitForLocation(file.getFullPath(), null).getAST();
		} catch (CModelException e) {
		} catch (CoreException e) {
		}
		if (result == null)
			throw new NotSupportedException("Cannot find translation unit for sibling file");
		return result;
	}

	private boolean askUserForFileCreation(final ToggleRefactoringContext context) {
		final Container<Boolean> answer = new Container<Boolean>();
		Runnable r = new Runnable() {
			@Override
			public void run() {
				Shell shell = UIPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getShell();
				boolean createnew = MessageDialog.openQuestion(shell, "New Implementation file?", 
						"Create a new file named: " + getNewFileName() + " and move " + context.getDeclaration().getRawSignature() + "?");
				answer.setObject(createnew);
			}
		};
		PlatformUI.getWorkbench().getDisplay().syncExec(r);
		return answer.getObject();
	}

	@Override
	public void run(ModificationCollector collector) {
		IASTFunctionDefinition new_definition = copyDefinitionFromInHeader();
		removeDefinitionFromHeader(collector);

		IASTNode insertion_parent = null;
		ASTRewrite impl_rewrite = collector.rewriterForTranslationUnit(impl_unit);
		if (includenode != null) {
			impl_rewrite.insertBefore(impl_unit, null, includenode, infoText);
		}
		IASTNode parent = getParentNamespace(context.getDeclaration());
		
		if (parent instanceof ICPPASTNamespaceDefinition) {
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

	private String getIncludeStatement() {
		return "#include \"" + ToggleNodeHelper.getFilenameWithoutExtension(getNewFileName()) + ".h\"";
	}

	private void addToImplementationFile(IASTFunctionDefinition new_definition,
			IASTNode insertion_parent, ASTRewrite impl_rewrite) {
		new_definition.setParent(insertion_parent);
		IASTNode insertion_point = InsertionPointFinder.findInsertionPoint(
				context.getDeclarationUnit(), insertion_parent.getTranslationUnit(), 
				context.getDeclaration());
		impl_rewrite.insertBefore(insertion_parent, insertion_point, new_definition, infoText);
	}

	private CPPASTNamespaceDefinition createNamespace(
			ICPPASTNamespaceDefinition parent_namespace) {
		return new CPPASTNamespaceDefinition(parent_namespace.getName().copy());
	}

	private void removeDefinitionFromHeader(ModificationCollector collector) {
		ASTRewrite header_rewrite = collector.rewriterForTranslationUnit(
				context.getDefinitionUnit());
		header_rewrite.remove(ToggleNodeHelper.getParentRemovePoint(
				context.getDefinition()), infoText);
	}
	
	private void createNewImplementationFile() {
		CreateFileChange change;
		String filename = getNewFileName();
		try {
			change = new CreateFileChange(filename, new	Path(getPath()+filename), 
					"", context.getSelectionFile().getCharset());
			change.perform(new NullProgressMonitor());
		} catch (CoreException e) {
			throw new NotSupportedException("Cannot create new filechange");
		}
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
	
	private String getNewFileName() {
		return ToggleNodeHelper.getFilenameWithoutExtension(
				context.getSelectionFile().getFullPath().toString()) + ".cpp";
	}
	
	private IASTFunctionDefinition copyDefinitionFromInHeader() {
		IASTFunctionDefinition newImpldef = context.getDefinition().copy();
		newImpldef.getDeclSpecifier().setInline(false);
		return newImpldef;
	}

	private String getPath() {
		String result = context.getSelectionFile().getFullPath().toOSString();
		return result.replaceAll("(\\w)*\\.(\\w)*", "");
	}
}
