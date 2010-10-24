package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.jface.text.TextSelection;

@SuppressWarnings("restriction")
public class SelectedDeclaratorFinder extends CPPASTVisitor {
	public CPPASTFunctionDeclarator result = null;
	private TextSelection selection;
	{
		shouldVisitDeclarators = true;
	}

	public SelectedDeclaratorFinder(TextSelection selection) {
		this.selection = selection;
	}

	public int visit(IASTDeclarator node) {
		if (!(node instanceof CPPASTFunctionDeclarator))
			return super.visit(node);
		if (ToggleSelectionHelper.isSelectionOnExpression(ToggleSelectionHelper.getRegion(selection), node)) {
			result = (CPPASTFunctionDeclarator) node;
		}
		return super.visit(node);
	}
}