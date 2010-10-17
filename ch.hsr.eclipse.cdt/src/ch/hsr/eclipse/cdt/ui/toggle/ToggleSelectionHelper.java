package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;
import java.util.Collections;

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

/**
 * Helps finding a FunctionDefinition in the parent nodes of the current selection. 
 */
class ToggleSelectionHelper extends SelectionHelper {		
	public static IASTFunctionDefinition getFirstSelectedFunctionDefinition(final Region region, final IASTTranslationUnit unit) {
		final Container<IASTFunctionDefinition> container = new Container<IASTFunctionDefinition>();
		
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}
			public int visit(IASTDeclarator declarator) {
				if (declarator instanceof CPPASTFunctionDeclarator) {
					if (declarator.getParent() instanceof ICPPASTFunctionDefinition && isSelectionOnExpression(region, unit)) {
						container.setObject((IASTFunctionDefinition) declarator.getParent());
					}
				}
				return super.visit(declarator);
			}
		});
		return container.getObject();
	}

	static ArrayList<IASTName> getAllQualifiedNames(IASTFunctionDefinition memberdefinition) {
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

	static ICPPASTQualifiedName getQualifiedName(IASTFunctionDefinition memberdefinition) {
		ICPPASTQualifiedName newdecl = new CPPASTQualifiedName();
		for (IASTName name : getAllQualifiedNames(memberdefinition)) {
			newdecl.addName(name.copy());
		}
		newdecl.addName(memberdefinition.getDeclarator().getName().copy());
		return newdecl;
	}
}