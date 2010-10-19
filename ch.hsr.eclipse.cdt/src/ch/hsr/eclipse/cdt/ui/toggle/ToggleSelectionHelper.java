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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.jface.text.TextSelection;

/**
 * Helps finding a FunctionDefinition in the parent nodes of the current selection. 
 */
class ToggleSelectionHelper extends SelectionHelper {		

	public static IASTFunctionDefinition getSelectedDefinition(
			final IASTTranslationUnit unit, final TextSelection selection) {
		final Container<IASTFunctionDefinition> container = new Container<IASTFunctionDefinition>();
		
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarations = true;
			}
			public int visit(IASTDeclaration node) {
				if (!(node instanceof IASTFunctionDefinition))
					return super.visit(node);
				IASTFunctionDefinition func = (IASTFunctionDefinition) node;
				String currentNodeName = new String(func.getDeclarator().getName().getSimpleID());
				if (currentNodeName.equals(selection.getText())) {
					// TODO: In addition, check if at same offset as selection
					System.out.println("Found matching definition: " + func.getRawSignature());
					container.setObject((IASTFunctionDefinition) func);
				}
				return super.visit(node);
			}
		});
		return container.getObject();
	}
	
	public static CPPASTFunctionDeclarator getSelectedDeclaration(final IASTTranslationUnit unit, final TextSelection selection) {
		final Container<CPPASTFunctionDeclarator> container = new Container<CPPASTFunctionDeclarator>();
		
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}
			public int visit(IASTDeclarator node) {
				if (!(node instanceof CPPASTFunctionDeclarator))
					return super.visit(node);
				CPPASTFunctionDeclarator func = (CPPASTFunctionDeclarator) node;
				String currentNodeName = new String(func.getName().getSimpleID());
				if (currentNodeName.equals(selection.getText())) {
					// TODO: In addition, check if at same offset as selection
					System.out.println("Found matching declaration: " + func.getRawSignature());
					container.setObject((CPPASTFunctionDeclarator) func);
					return PROCESS_ABORT;
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
			else if (node instanceof ICPPASTNamespaceDefinition) {
				names.add(((ICPPASTNamespaceDefinition) node).getName());
			}
			else if (node instanceof ICPPASTTemplateDeclaration) {
				for(IASTNode child : node.getChildren()) {
					if (child instanceof ICPPASTSimpleTypeTemplateParameter) {
						ICPPASTSimpleTypeTemplateParameter tempcild = (ICPPASTSimpleTypeTemplateParameter) child;
						names.add(tempcild.getName());
						//TODO: fix me: not insert template as name but as template parameter
						//NOT: A::T::B but A<T>::B
					}
				}
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