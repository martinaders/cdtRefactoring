package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock;
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
			if (child instanceof ICPPASTParameterDeclaration) {
				ICPPASTParameterDeclaration parameter = (ICPPASTParameterDeclaration) child;
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

	static ICPPASTTemplateDeclaration getTemplateDeclaration(IASTNode node) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTTemplateDeclaration)
				return (ICPPASTTemplateDeclaration) node.copy();
		}
		return null;
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
	
}
