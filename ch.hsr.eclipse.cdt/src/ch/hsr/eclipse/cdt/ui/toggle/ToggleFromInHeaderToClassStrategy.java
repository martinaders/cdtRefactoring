package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToClassStrategy implements
		ToggleRefactoringStrategy {

	private TextEditGroup infoText;
	private ToggleRefactoringContext context;

	public ToggleFromInHeaderToClassStrategy(ToggleRefactoringContext context) {
		this.context = context;
		this.infoText =  new TextEditGroup("Toggle function body placement");
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(context
				.getDefinitionUnit());
		rewriter.remove(ToggleNodeHelper.getParentRemovePoint(context.getDefinition()), infoText);
		rewriter.replace(context.getDeclaration().getParent(),
				ToggleNodeHelper.createInClassDefinition( context.getDeclaration(), 
						context.getDefinition(), context.getDefinitionUnit()), infoText);

	}
}
