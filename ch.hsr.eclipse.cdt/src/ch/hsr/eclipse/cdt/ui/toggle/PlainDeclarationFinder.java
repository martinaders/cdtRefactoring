package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;

@SuppressWarnings("restriction")
class PlainDeclarationFinder extends CPPASTVisitor {
	private CPPASTFunctionDeclarator similar;
	private String similarName;
	public CPPASTFunctionDeclarator result;
	{
		shouldVisitDeclarators = true;
	}

	PlainDeclarationFinder(CPPASTFunctionDeclarator similar) {
		this.similar = similar;
		this.similarName = new String(similar.getName().getSimpleID());
	}

	public int visit(IASTDeclarator node) {
		if (!(node instanceof CPPASTFunctionDeclarator))
			return super.visit(node);
		CPPASTFunctionDeclarator func = (CPPASTFunctionDeclarator) node;
		String currentNodeName = new String(func.getName().getSimpleID());
		// TODO: Name, Scope and Parameter names have to be the same
		// TODO: checking number of parameters as a small step towards correctness
		if (currentNodeName.equals(similarName) && func.getParameters().length == similar.getParameters().length) {
			// prioritize plain declarations over definitions
			if (result == null || !(func.getParent() instanceof ICPPASTFunctionDefinition)) {
				System.out.println("Found matching declaration: " + func.getRawSignature());
				result = func;
			}
			return PROCESS_CONTINUE;
		}
		return super.visit(node);
	}
}