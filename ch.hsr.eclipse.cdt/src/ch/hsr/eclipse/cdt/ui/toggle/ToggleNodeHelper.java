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

import java.net.URI;
import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.index.IndexLocationFactory;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateId;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTypeId;
import org.eclipse.cdt.internal.ui.refactoring.utils.NodeHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

@SuppressWarnings("restriction")
public class ToggleNodeHelper extends NodeHelper {

	static boolean hasCatchHandlers(IASTNode node) {
		return node instanceof ICPPASTFunctionWithTryBlock;
	}

	static void copyCatchHandlers(IASTFunctionDefinition oldfunc, ICPPASTFunctionDefinition newfunc) {
		for (ICPPASTCatchHandler chandler : ((ICPPASTFunctionWithTryBlock) oldfunc).getCatchHandlers()) {
			((CPPASTFunctionWithTryBlock) newfunc).addCatchHandler(chandler
					.copy());
		}
	}

	static void copyInitializerList(ICPPASTFunctionDefinition newfunc, IASTFunctionDefinition oldfunc) {
		for (ICPPASTConstructorChainInitializer singlelist : getAllInitializerList(oldfunc)) {
			singlelist.setParent(newfunc);
			newfunc.addMemberInitializer(singlelist);
		}
	}

	static void removeParameterInitializations(IASTFunctionDeclarator funcdecl) {
		for (IASTNode child : funcdecl.getChildren()) {
			if (child instanceof IASTParameterDeclaration) {
				IASTParameterDeclaration parameter = (IASTParameterDeclaration) child;
				parameter.getDeclarator().setInitializer(null);
			}
		}
	}

	static boolean hasInitializerList(
			IASTFunctionDefinition definition) {
		for (IASTNode node : definition.getChildren()) {
			if (node instanceof ICPPASTConstructorChainInitializer)
				return true;
		}
		return false;
	}

	static ArrayList<ICPPASTConstructorChainInitializer> getAllInitializerList(
			IASTFunctionDefinition definition) {
		ArrayList<ICPPASTConstructorChainInitializer> result = 
			new ArrayList<ICPPASTConstructorChainInitializer>();
	
		for (IASTNode node : definition.getChildren()) {
			if (node instanceof ICPPASTConstructorChainInitializer)
				result.add(((ICPPASTConstructorChainInitializer) node).copy());
		}
		return result;
	}

	static IASTSimpleDeclaration createDeclarationFromDefinition(
			IASTFunctionDefinition sourcedefinition) {
		IASTDeclarator declarator = sourcedefinition.getDeclarator().copy();
		IASTDeclSpecifier specifier = sourcedefinition.getDeclSpecifier()
				.copy();
		IASTSimpleDeclaration result = new CPPASTSimpleDeclaration(specifier);
		result.addDeclarator(declarator);
		return result;
	}

	static ICPPASTFunctionDefinition assembleFunctionDefinitionWithBody(
			IASTDeclSpecifier newdeclspec, IASTFunctionDeclarator funcdecl, IASTFunctionDefinition def) {
		IASTStatement newbody = def.getBody().copy();
		ICPPASTFunctionDefinition newfunc = null;
		if (hasCatchHandlers(def)) {
			newfunc = new CPPASTFunctionWithTryBlock(newdeclspec, funcdecl,newbody);
			copyCatchHandlers(def, newfunc);
		} else {
			newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl,newbody);
		}
	
		if (hasInitializerList(def)) {
			copyInitializerList(newfunc, def);
		}
		return newfunc;
	}

	static IASTNode getQualifiedNameDefinition(IASTFunctionDefinition def, 
			IASTTranslationUnit definition_unit, IASTNode namespace) {
		
		ICPPASTDeclSpecifier newdeclspec = (ICPPASTDeclSpecifier) def.getDeclSpecifier().copy();
		newdeclspec.setVirtual(false);
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = def.getDeclarator().copy();
	
		funcdecl.setName(ToggleNodeHelper.getQualifiedName(def.getDeclarator(), namespace));
		
		removeParameterInitializations(funcdecl);
		ICPPASTFunctionDefinition newfunc = assembleFunctionDefinitionWithBody(newdeclspec, funcdecl, def);
	
		ICPPASTTemplateDeclaration templdecl = getTemplateDeclaration(def);
		if (templdecl != null) {
			templdecl.setDeclaration(newfunc);
			templdecl.setParent(definition_unit);
			return templdecl;
		} else {
			newfunc.setParent(definition_unit);
			return newfunc;
		}
	}

	static ICPPASTTemplateDeclaration getTemplateDeclaration(IASTNode node) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTTemplateDeclaration)
				return (ICPPASTTemplateDeclaration) node.copy();
		}
		return null;
	}

	static IASTNode getParentInsertionPoint(IASTNode node,
			IASTTranslationUnit alternative) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				return (ICPPASTCompositeTypeSpecifier) node;
			}
		}
		return alternative;
	}

	static IASTFunctionDefinition createInClassDefinition(
			IASTFunctionDeclarator dec, 
			IASTFunctionDefinition def, 
			IASTTranslationUnit insertionunit) {
		IASTFunctionDeclarator declarator = dec.copy();
		ICPPASTDeclSpecifier declspec = (ICPPASTDeclSpecifier) def.getDeclSpecifier().copy();
		declspec.setInline(false);
		if (ToggleNodeHelper.isVirtual(dec))
			declspec.setVirtual(true);
		
		IASTFunctionDefinition newdefinition = assembleFunctionDefinitionWithBody(declspec, declarator, def);
		newdefinition.setParent(getParentInsertionPoint(def, insertionunit));
		return newdefinition;
	}

	/**
	 * Does not work for *every* function declarator; its parent needs to be a
	 * simple declaration.
	 */
	static boolean isVirtual(IASTFunctionDeclarator fdec) {
		if (fdec.getParent() instanceof IASTSimpleDeclaration) {
			IASTSimpleDeclaration dec = (IASTSimpleDeclaration) fdec.getParent();
			return ((ICPPASTDeclSpecifier) dec.getDeclSpecifier()).isVirtual();
		}
		return false;
	}

	static IASTNode getParentRemovePoint(IASTFunctionDefinition definition) {
		IASTNode toremove = definition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = definition.getParent();
		return toremove;
	}

	/**
	 * @param declarator the declarator from which the full qualified namespace should be fetched
	 * @param limiter set a limiter in the class hierarchy where the lookup will stop
	 * @return
	 */
	static ICPPASTQualifiedName getQualifiedName(IASTFunctionDeclarator declarator, IASTNode limiter) {
		Stack<IASTNode> nodes = getQualifiedNames(declarator, limiter, declarator);
		CPPASTQualifiedName result = reAssembleQualifiedName(nodes);
		result.addName(declarator.getName().copy());
		return result;
	}

	private static CPPASTQualifiedName reAssembleQualifiedName(
			Stack<IASTNode> nodes) {
		CPPASTQualifiedName result = new CPPASTQualifiedName();
		while(!nodes.isEmpty()) {
			IASTNode nnode = nodes.pop();
			if (nnode instanceof IASTCompositeTypeSpecifier) {
				result.addName(((IASTCompositeTypeSpecifier) nnode).getName());
			}
			else if (nnode instanceof ICPPASTNamespaceDefinition) {
				result.addName(((ICPPASTNamespaceDefinition) nnode).getName());
			}
			else if (nnode instanceof ICPPASTTemplateId) {
				result.addName((ICPPASTTemplateId) nnode);
			}
		}
		return result;
	}

	private static Stack<IASTNode> getQualifiedNames(
			IASTFunctionDeclarator declarator, IASTNode limiter, IASTNode node) {
		IASTName lastname = declarator.getName();
		Stack<IASTNode> nodes = new Stack<IASTNode>();
		while(node.getParent() != null && node.getParent() != limiter) {
			node = node.getParent();
			if (node instanceof IASTCompositeTypeSpecifier) {
				nodes.push(((IASTCompositeTypeSpecifier) node).copy());
				lastname = ((IASTCompositeTypeSpecifier) node).getName();
			}
			else if (node instanceof ICPPASTNamespaceDefinition) {
				nodes.push(((ICPPASTNamespaceDefinition) node).copy());
				lastname = ((ICPPASTNamespaceDefinition) node).getName();
			}
			else if (node instanceof ICPPASTTemplateDeclaration) {
				if (!nodes.isEmpty())
					nodes.pop();
				ICPPASTTemplateId templateid = ToggleNodeHelper.getTemplateParameter(node, lastname);
				nodes.add(templateid);
			} 
		}
		return nodes;
	}

	static ICPPASTTemplateId getTemplateParameter(IASTNode node, IASTName name) {
		ICPPASTTemplateId templateid = new CPPASTTemplateId();
		templateid.setTemplateName(name.copy());
		
		for(IASTNode child : node.getChildren()) {
			if (child instanceof ICPPASTSimpleTypeTemplateParameter) {
				ICPPASTSimpleTypeTemplateParameter tempcild = (ICPPASTSimpleTypeTemplateParameter) child;
	
				CPPASTNamedTypeSpecifier namedTypeSpecifier = new CPPASTNamedTypeSpecifier();
				namedTypeSpecifier.setName(tempcild.getName().copy());
				
				CPPASTTypeId id = new CPPASTTypeId();
				id.setDeclSpecifier(namedTypeSpecifier);
				templateid.addTemplateArgument(id);
			}
		}
		return templateid;
	}

	static IASTTranslationUnit getSiblingFile(IFile file, IASTTranslationUnit asttu) throws CoreException {
		ICProject cProject = CoreModel.getDefault().create(file).getCProject();
		IIndex projectindex = CCorePlugin.getIndexManager().getIndex(cProject);
		
		IIndexFile thisfile = projectindex.getFile(asttu.getLinkage().getLinkageID(),IndexLocationFactory.getWorkspaceIFL(file));
		String filename = ToggleNodeHelper.getFilenameWithoutExtension(file.getFullPath().toString());
		IIndexInclude[] include;
		boolean headerflag = false;
		if (asttu.isHeaderUnit()) {
			include = projectindex.findIncludedBy(thisfile);
			headerflag = true;
		} else {
			include = projectindex.findIncludes(thisfile);
		}
		
		URI uri = getTranslationUnitOfSiblingFile(include, filename, headerflag);
		return getTranslationUnitFromFile(cProject, projectindex, uri);
	}
	
	
	private static URI getTranslationUnitOfSiblingFile(IIndexInclude[] includes, 
			String filename, boolean isheader) {
		URI uri = null;
		for(IIndexInclude include: includes) {
			try {
				if (isheader) {
					uri = include.getIncludedBy().getLocation().getURI();
				} else {
					uri = include.getIncludesLocation().getURI();
				}
				if (ToggleNodeHelper.getFilenameWithoutExtension(uri.getRawPath()).equals(filename)) {
					break;
				}
			} catch (CoreException e) {
				throw new NotSupportedException("Could not get sibling translation unit");
			}
		}
		return uri;
	}

	private static IASTTranslationUnit getTranslationUnitFromFile(
			ICProject cProject, IIndex projectindex, URI uri)
			throws CModelException, CoreException {
		ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(uri, cProject);
		return tu.getAST(projectindex, ITranslationUnit.AST_SKIP_ALL_HEADERS);
	}

	static String getFilenameWithoutExtension(String filename) {
		filename = filename.replaceAll("\\.(.)*$", "");
		filename = filename.replaceAll("(.)*\\/", "");
		return filename;
	}

	static boolean isInsideAClass(IASTFunctionDeclarator declarator, IASTFunctionDeclarator backup) {
		if (declarator.getName() instanceof ICPPASTQualifiedName)
			declarator = backup;
		IASTNode node = declarator;
		while(node != null) {
			if (node instanceof IASTCompositeTypeSpecifier)
				return true;
			node = node.getParent();
		}
		return false;
	}

	public static boolean isWrappedInsideAClass(IASTNode definition) {
		IASTNode node = definition;
		while (node != null) {
			if (node instanceof ICPPASTCompositeTypeSpecifier)
				return true;
			node = node.getParent();
		}
		return false;
	}
}