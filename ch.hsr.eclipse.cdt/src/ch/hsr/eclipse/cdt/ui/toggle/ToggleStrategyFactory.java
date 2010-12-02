package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;

public class ToggleStrategyFactory {
	
	private ToggleRefactoringContext context;

	public ToggleStrategyFactory(ToggleRefactoringContext context) {
		this.context = context;
	}
	
	public ToggleRefactoringStrategy getAppropriateStategy() {
		if (context.getDefinition() == null)
			throw new NotSupportedException("cannot work without function defintion");
		if (!context.getDefinitionUnit().isHeaderUnit())
			return new ToggleFromImplementationToClassStrategy(context);
		if (isFreeFunction() && context.getDefinitionUnit().isHeaderUnit())
			return new ToggleFreeFunctionFromInHeaderToImpl(context);
		if (isInClassSituation())
			return new ToggleFromClassToInHeaderStrategy(context);
		if (isTemplateSituation())
			return new ToggleFromInHeaderToClassStrategy(context);
		if (isinHeaderSituation())
			return new ToggleFromInHeaderToImplementationStrategy(context);
		throw new NotSupportedException("Unsupported situation for moving function body.");
	}
	
	private boolean isFreeFunction() {
		return !ToggleNodeHelper.isInsideAClass(context.getDefinition().getDeclarator(), context.getDeclaration());
	}

	private boolean isinHeaderSituation() {
		boolean declarationAndDefinitionExist = context.getDefinition() != null && context.getDeclaration() != null;
		return declarationAndDefinitionExist && context.getDefinitionUnit().isHeaderUnit() && isInSameFile();
	}

	private boolean isInSameFile() {
		return context.getDefinition().getFileLocation().getFileName().equals(context.getDeclaration().getFileLocation().getFileName());
	}

	private boolean isInClassSituation() {
		return ToggleNodeHelper.isWrappedInsideAClass(context.getDefinition()) && context.getDeclaration() == null;
	}

	private boolean isTemplateSituation() {
		IASTNode node = context.getDefinition();
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTTemplateDeclaration)
				return true;
		}
		return false;
	}
}
