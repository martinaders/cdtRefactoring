package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy implements
		ToggleRefactoringStrategy {

	private TextEditGroup infoText = new TextEditGroup(
			"Toggle function body placement");
	private ToggleRefactoringContext context;

	public ToggleFromInHeaderToClassStrategy(ToggleRefactoringContext context) {
		this.context = context;
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(context
				.getDefinitionUnit());
		IASTNode toremove = context.getDefinition();
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = context.getDefinition().getParent();

		rewriter.remove(toremove, infoText);
		// TODO: use deFINItion?
		rewriter.replace(context.getDeclaration().getParent(), ToggleNodeHelper
				.createInClassDefinition(context.getDeclaration(),
						context.getDefinition(), context.getDefinitionUnit()),
				infoText);

	}
}
