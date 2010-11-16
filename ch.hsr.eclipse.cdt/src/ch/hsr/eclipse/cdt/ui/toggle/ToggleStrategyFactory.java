package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
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
		if (isFreeFunction() && isAllInHeader()) {
			try {
				System.out.println("ToggleFreeFunctionFromInHeaderToImpl");
				return new ToggleFreeFunctionFromInHeaderToImpl(context);
			} catch (Exception e) {
				throw new NotSupportedException("move FreeFunctionFromInHeaderToImplementation was not possible");
			}
		}
		if (isInClassSituation()) {
			System.out.println("ToggleFromClassToInHeaderStrategy");
			System.out.println(context.getDefinition() + ", " + context.getDeclaration());
			if (isWrappedInsideAClass(context.getDefinition()) && isWrappedInsideAClass(context.getDeclaration()))
				throw new NotSupportedException("behavior when def + decl both inside a class is undefined");
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
		return !ToggleSelectionHelper.isInsideAClass(context.getDefinition().getDeclarator(), context.getDeclaration());
	}

	private boolean isAllInHeader() {
		return getFileExtension(context.getDefinition().getFileLocation().getFileName()).equals("h");
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

	private boolean isWrappedInsideAClass(IASTNode definition) {
		IASTNode node = definition;
		while (node != null) {
			if (node instanceof ICPPASTCompositeTypeSpecifier)
				return true;
			node = node.getParent();
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

	private boolean isInImplementationSituation() throws NotSupportedException {
		IASTTranslationUnit unit = context.getDefinitionUnit();
		IASTFileLocation location = unit.getFileLocation();
		String filename = location.getFileName();
		String extension = getFileExtension(filename);
		
		if ((extension.equals("cpp") || extension.equals("c"))) {
			if (context.getDeclarationUnit() == null) {
				throw new NotSupportedException("Not supported if no declaration is found");
			}
			return true;
		}
		return false;
	}
	
	private String getFileExtension(String fileName) {
		return fileName.replaceAll("(.)*\\.", "");
	}
}
