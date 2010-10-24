package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;

@SuppressWarnings("restriction")
class PlainDeclarationFinder extends CPPASTVisitor {
	public CPPASTFunctionDeclarator result;
	private CPPASTFunctionDeclarator similar;
	{
		shouldVisitDeclarators = true;
	}

	PlainDeclarationFinder(CPPASTFunctionDeclarator similar) {
		this.similar = similar;
	}

	public int visit(IASTDeclarator node) {
		if (!(node instanceof CPPASTFunctionDeclarator))
			return super.visit(node);
		CPPASTFunctionDeclarator declarator = (CPPASTFunctionDeclarator) node;
		if (isInSameScope(declarator, similar)) {
			// prioritize plain declarations over definitions
			if (result == null || !(declarator.getParent() instanceof CPPASTFunctionDefinition)) {
				System.out.println("Found matching declaration: "+ declarator.getRawSignature());
				result = declarator;
			}
		}
		return super.visit(node);
	}

	private boolean isInSameScope(ICPPASTFunctionDeclarator declarator, ICPPASTFunctionDeclarator other) {
		ArrayList<String> q1 = new ArrayList<String>();
		ArrayList<String> q2 = new ArrayList<String>();

		q1 = extractNames(declarator);
		q2 = extractNames(other);

		return compareScope(q1, q2);
	}

	private boolean compareScope(ArrayList<String> qualiA, ArrayList<String> qualiB) {
		return qualiA.equals(qualiB);
	}

	private ArrayList<String> extractNames(ICPPASTFunctionDeclarator declarator) {
		ArrayList<String> q1 = new ArrayList<String>();
		IASTName qualiA = declarator.getName();
		if (qualiA instanceof ICPPASTQualifiedName) {
			ICPPASTQualifiedName temp = (ICPPASTQualifiedName)qualiA;
			for (IASTName name : temp.getNames()) {
				q1.add(name.toString());
			}
		}
		
		IASTNode parent = declarator.getParent();
		ArrayList<String> arr = new ArrayList<String>();
		while (parent != null) {
			if (parent instanceof ICPPASTCompositeTypeSpecifier)
				arr.add(((ICPPASTCompositeTypeSpecifier)parent).getName().toString());
//			if (parent instanceof ICPPASTNamespaceDefinition)
//				arr.add(((ICPPASTNamespaceDefinition)parent).getName().toString());
			parent = parent.getParent();
		}

		Collections.reverse(arr);
		for (String n : arr) {
			q1.add(n);
		}
		
		if (!(qualiA instanceof ICPPASTQualifiedName)) {
			q1.add(qualiA.toString());
		}
		return q1;
	}
}