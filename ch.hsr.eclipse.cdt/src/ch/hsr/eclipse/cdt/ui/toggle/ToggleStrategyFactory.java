package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;

public class ToggleStrategyFactory {
	
	private ToggleRefactoringContext context;

	public ToggleStrategyFactory(ToggleRefactoringContext context) {
		this.context = context;
	}
	
	public ToggleRefactoringAbstractStrategy getAppropriateStategy() throws NotSupportedException {
		assert(context.getDefinition() != null);
		if (isInImplementationSituation()) {
			System.out.println("ToggleFromImplementationToClassStrategy");
			return new ToggleFromImplementationToClassStrategy(context);
		}
		if (isFreeFunction() && 
				getFileExtension(context.getDeclaration().getFileLocation().getFileName()).equals("h") &&
				getFileExtension(context.getDefinition().getFileLocation().getFileName()).equals("h")) {
			try {
				System.out.println("ToggleFreeFunctionFromInHeaderToImpl");
				return new ToggleFreeFunctionFromInHeaderToImpl(context);
			} catch (Exception e) {
				throw new NotSupportedException("move FreeFunctionFromInHeaderToImplementation was not possible");
			}
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
			try {
				System.out.println("ToggleFromInHeaderToImplementationStrategy");
				return new ToggleFromInHeaderToImplementationStrategy(context);
			} catch (Exception e) {
				throw new NotSupportedException("move FromInHeaderToImplementation was not possible.");
			}
		} 
		return null;
	}
	
	private boolean isFreeFunction() {
		if (context.getDeclaration() != null && ToggleSelectionHelper.getAllQualifiedNames(context.getDeclaration()).isEmpty())
			return true;
		return false;
	}

	private boolean isinHeaderSituation() {
		boolean declarationAndDefinitionExist = context.getDefinition() != null && context.getDeclaration() != null;
		return declarationAndDefinitionExist && isInHeaderFile() && isInSamFile();
	}

	private boolean isInSamFile() {
		return context.getDefinition().getFileLocation().getFileName().equals(context.getDefinition().getFileLocation().getFileName());
	}

	private boolean isInHeaderFile() {
		return context.getDefinition().getFileLocation().getFileName().endsWith(".h") || context.getDefinition().getFileLocation().getFileName().endsWith(".hpp");
	}

	// special: Don't support decl AND def inside the class definition
	private boolean isInClassSituation() {
		return isWrappedInsideAClass(context.getDefinition()) && context.getDeclaration() == null;
	}

	// structs?
	private boolean isWrappedInsideAClass(IASTFunctionDefinition definition) {
		IASTNode node = context.getDefinition();
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier)
				return true;
		}
		return false;
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
		//String extension1 = getFileExtension(context.getDeclarationUnit().getFileLocation().getFileName());
		String extension2 = getFileExtension(context.getDefinitionUnit().getFileLocation().getFileName());
		if ((extension2.equals("cpp") || extension2.equals("c")))
			return true;
		return false;
	}
	
	private String getFileExtension(String fileName) {
		return fileName.replaceAll("(.)*\\.", "");
	}
}
