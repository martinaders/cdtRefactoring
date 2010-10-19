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
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;

/**
 * Helps finding a FunctionDefinition in the parent nodes of the current selection. 
 */
@SuppressWarnings("restriction")
class ToggleSelectionHelper extends SelectionHelper {		
	
	public static CPPASTFunctionDeclarator getSelectedDeclaration(IASTTranslationUnit unit, TextSelection selection) {
		CPPASTFunctionDeclarator selectedDeclaration = getSelectedDeclarator(unit, selection);
		if (selectedDeclaration == null) {
			System.out.println("cannot determine selected function.");
			return null;
		}
		PlainDeclarationFinder visitor = new PlainDeclarationFinder(selectedDeclaration);
		unit.accept(visitor);
		return visitor.result;
	}

	private static CPPASTFunctionDeclarator getSelectedDeclarator(IASTTranslationUnit unit, TextSelection selection) {
		SelectedDeclaratorFinder visitor = new SelectedDeclaratorFinder(selection);
		unit.accept(visitor);
		return visitor.result;
	}

	public static IASTFunctionDefinition getSelectedDefinition(IASTTranslationUnit unit, TextSelection selection) {
		final CPPASTFunctionDeclarator selectedDeclaration = getSelectedDeclarator(unit, selection);
		if (selectedDeclaration == null) {
			System.out.println("cannot determine selected function.");
			return null;
		}
		final Container<IASTFunctionDefinition> result = new Container<IASTFunctionDefinition>();
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
					result.setObject((IASTFunctionDefinition) func);
				}
				return super.visit(node);
			}
		});
		return result.getObject();
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
		newdecl.setFullyQualified(true);
		return newdecl;
	}
	
	@Deprecated
	public static IASTFunctionDefinition getFirstSelectedFunctionDefinition(final Region region, final IASTTranslationUnit unit) {
		final Container<IASTFunctionDefinition> container = new Container<IASTFunctionDefinition>();
		
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}
			public int visit(IASTDeclarator declarator) {
				if (declarator instanceof CPPASTFunctionDeclarator) {
					if ((declarator.getParent() instanceof ICPPASTFunctionDefinition) && isSelectionOnExpression(region, declarator)) {
						container.setObject((IASTFunctionDefinition) declarator.getParent());
					}
				}
				return super.visit(declarator);
			}
		});
		return container.getObject();
	}

}
