package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.utils.NodeHelper;

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
			newfunc = new CPPASTFunctionWithTryBlock(newdeclspec, funcdecl,
					newbody);
			copyCatchHandlers(def, newfunc);
		} else {
			newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl,
					newbody);
		}
	
		if (hasInitializerList(def)) {
			copyInitializerList(newfunc, def);
		}
		return newfunc;
	}

	static IASTNode getQualifiedNameDefinition(
			boolean inline, 
			IASTFunctionDefinition def, 
			IASTFunctionDeclarator dec, 
			IASTTranslationUnit definition_unit) {
		
		ICPPASTDeclSpecifier newdeclspec = (ICPPASTDeclSpecifier) def.getDeclSpecifier().copy();
		newdeclspec.setVirtual(false);
		newdeclspec.setInline(inline);
		// was: declaration
		IASTFunctionDeclarator funcdecl = def.getDeclarator().copy();
	
		// TODO: rethink correctness of this statement
		if (dec != null)
			funcdecl.setName(ToggleSelectionHelper.getQualifiedName(dec));
		else
			funcdecl.setName(ToggleSelectionHelper.getQualifiedName(def.getDeclarator()));
		
		removeParameterInitializations(funcdecl);
		ICPPASTFunctionDefinition newfunc = assembleFunctionDefinitionWithBody(newdeclspec, funcdecl, def);
	
		// was: declaration
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
	
	static boolean isVirtual(IASTFunctionDeclarator fdec) {
		IASTSimpleDeclaration dec = (IASTSimpleDeclaration) fdec.getParent();
		ICPPASTDeclSpecifier olddeclspec = (ICPPASTDeclSpecifier) dec.getDeclSpecifier();
		return olddeclspec.isVirtual();
	}

	static IASTNode getParentRemovePoint(IASTFunctionDefinition definition) {
		IASTNode toremove = definition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = definition.getParent();
		return toremove;
	}
}