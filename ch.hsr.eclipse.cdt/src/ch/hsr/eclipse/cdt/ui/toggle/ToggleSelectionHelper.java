package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.jface.text.TextSelection;

/**
 * Helps finding a FunctionDefinition in the parent nodes of the current selection. 
 */
class ToggleSelectionHelper extends SelectionHelper {		
	
	public static CPPASTFunctionDeclarator getSelectedDeclaration(final IASTTranslationUnit unit, final TextSelection selection) {
		final Container<CPPASTFunctionDeclarator> container = new Container<CPPASTFunctionDeclarator>();
		final Container<CPPASTFunctionDeclarator> selectedDeclaration = new Container<CPPASTFunctionDeclarator>();

		// Find the selected node (warning: DRY but existing function is not reliable!)
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}
			public int visit(IASTDeclarator node) {
				if (!(node instanceof CPPASTFunctionDeclarator))
					return super.visit(node);
				if (isSelectionOnExpression(getRegion(selection), node)) {
					selectedDeclaration.setObject((CPPASTFunctionDeclarator) node);
				}
				return super.visit(node);
			}
		});
		if (selectedDeclaration.getObject() == null) {
			System.out.println("cannot determine selected function.");
			return null;
		}

		// Now find the declarator that should be replaced by the refactoring
		final String selectedNodeName = new String(selectedDeclaration.getObject().getName().getSimpleID());
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}
			public int visit(IASTDeclarator node) {
				if (!(node instanceof CPPASTFunctionDeclarator))
					return super.visit(node);
				CPPASTFunctionDeclarator func = (CPPASTFunctionDeclarator) node;
				String currentNodeName = new String(func.getName().getSimpleID());
				// TODO: Name, Scope and Parameter names have to be the same
				// TODO: checking number of parameters as a small step towards correctness
				if (currentNodeName.equals(selectedNodeName) && func.getParameters().length == selectedDeclaration.getObject().getParameters().length) {
					// prioritize plain declarations over definitions
					if (container.getObject() == null || !(func.getParent() instanceof ICPPASTFunctionDefinition)) {
						System.out.println("Found matching declaration: " + func.getRawSignature());
						container.setObject((CPPASTFunctionDeclarator) func);
					}
					return PROCESS_CONTINUE;
				}
				return super.visit(node);
			}
		});
		return container.getObject();
	}

	public static IASTFunctionDefinition getSelectedDefinition(
			final IASTTranslationUnit unit, final TextSelection selection, final CPPASTFunctionDeclarator selectedDeclaration) {
		final Container<IASTFunctionDefinition> container = new Container<IASTFunctionDefinition>();
		final String selectedNodeName = new String(selectedDeclaration.getName().getSimpleID());
		
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarations = true;
			}
			public int visit(IASTDeclaration node) {
				if (!(node instanceof IASTFunctionDefinition))
					return super.visit(node);
				IASTFunctionDefinition func = (IASTFunctionDefinition) node;
				String currentNodeName = new String(func.getDeclarator().getName().getSimpleID());
				// TODO: add a more strict and complete equality check
				if (currentNodeName.equals(selectedNodeName)) {
					System.out.println("Found matching definition: " + func.getRawSignature());
					container.setObject((IASTFunctionDefinition) func);
				}
				return super.visit(node);
			}
		});
		return container.getObject();
	}

	public static ArrayList<IASTName> getAllQualifiedNames(IASTFunctionDefinition memberdefinition) {
		ArrayList<IASTName> names = new ArrayList<IASTName>();
		IASTNode node = memberdefinition; 
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				names.add(((ICPPASTCompositeTypeSpecifier) node).getName());
			}
		}
		Collections.reverse(names);
		return names;
	}

	public static ICPPASTQualifiedName getQualifiedName(IASTFunctionDefinition memberdefinition) {
		ICPPASTQualifiedName newdecl = new CPPASTQualifiedName();
		for (IASTName name : getAllQualifiedNames(memberdefinition)) {
			newdecl.addName(name.copy());
		}
		newdecl.addName(memberdefinition.getDeclarator().getName().copy());
		return newdecl;
	}

}