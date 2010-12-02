package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.core.runtime.Path;

public class ToggleStrategyFactory {
	
	private ToggleRefactoringContext context;

	public ToggleStrategyFactory(ToggleRefactoringContext context) {
		this.context = context;
	}
	
	public ToggleRefactoringStrategy getAppropriateStategy() {
		if (context.getDefinition() == null)
			throw new NotSupportedException("cannot work without function defintion");
		if ((!context.getDefinitionUnit().isHeaderUnit())) {
			System.out.println("ToggleFromImplementationToClassStrategy");
			return new ToggleFromImplementationToClassStrategy(context);
		}
		if (isFreeFunction() && isAllInHeader()) {
			System.out.println("ToggleFreeFunctionFromInHeaderToImpl");
			return new ToggleFreeFunctionFromInHeaderToImpl(context);
		}
		if (isInClassSituation()) {
			System.out.println("ToggleFromClassToInHeaderStrategy");
			return new ToggleFromClassToInHeaderStrategy(context);
		}
		if (isTemplateSituation()) {
			System.out.println("ToggleFromInHeaderToClassStrategy");
			return new ToggleFromInHeaderToClassStrategy(context);
		}
		if (isinHeaderSituation()) {
			System.out.println("ToggleFromInHeaderToImplementationStrategy");
			return new ToggleFromInHeaderToImplementationStrategy(context);
		}
		throw new NotSupportedException("Unsupported situation for moving function body.");
	}
	
	private boolean isFreeFunction() {
		return !ToggleNodeHelper.isInsideAClass(context.getDefinition().getDeclarator(), context.getDeclaration());
	}

	private boolean isAllInHeader() {
		Path p = new Path(context.getDefinition().getContainingFilename());
		return p.getFileExtension().equals("h");
	}

	private boolean isinHeaderSituation() {
		boolean declarationAndDefinitionExist = context.getDefinition() != null && context.getDeclaration() != null;
		System.out.println(declarationAndDefinitionExist + ", " + isInHeaderFile() + ", " + isInSameFile());
		return declarationAndDefinitionExist && isInHeaderFile() && isInSameFile();
	}

	private boolean isInSameFile() {
		return context.getDefinition().getFileLocation().getFileName().equals(context.getDeclaration().getFileLocation().getFileName());
	}

	private boolean isInHeaderFile() {
		Path p = new Path(context.getDefinition().getContainingFilename());
		return p.getFileExtension().equals("h") || p.getFileExtension().equals("hpp");
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
