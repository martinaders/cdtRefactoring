package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy extends
		ToggleRefactoringAbstractStrategy {

	public ToggleFromInHeaderToClassStrategy(
			ICPPASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition, IASTTranslationUnit unit) {
		super(selectedDeclaration, selectedDefinition, unit);
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(unit);
		IASTNode toremove = selectedDefinition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = selectedDefinition.getParent();

		rewriter.remove(toremove, infoText);
		rewriter.replace(selectedDeclaration.getParent(),
				getInClassDefinition(selectedDefinition, selectedDeclaration),
				infoText);
	}

}
