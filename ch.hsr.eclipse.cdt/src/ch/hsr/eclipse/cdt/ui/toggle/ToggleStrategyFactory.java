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
		assert(context.getDefinition() != null);
		if (isInImplementationSituation()) {
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
		System.out.println(declarationAndDefinitionExist + ", " + isInHeaderFile() + ", " + isInSamFile());
		return declarationAndDefinitionExist && isInHeaderFile() && isInSamFile();
	}

	private boolean isInSamFile() {
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

	private boolean isInImplementationSituation() {
		Path p = new Path(context.getDefinition().getContainingFilename());
		if ((p.getFileExtension().equals("cpp") || p.getFileExtension().equals("c") || p.getFileExtension().equals("cxx"))) {
			if (context.getDeclarationUnit() == null) {
				throw new NotSupportedException("Not supported if no declaration is found");
			}
			return true;
		}
		return false;
	}
}
