package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.jface.text.Region;

@SuppressWarnings("restriction")
public class ToggleSelectionHelper extends SelectionHelper {

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
