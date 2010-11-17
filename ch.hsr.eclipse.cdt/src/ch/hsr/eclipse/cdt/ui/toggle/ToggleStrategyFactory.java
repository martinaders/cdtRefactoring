package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.core.runtime.Path;

public class ToggleStrategyFactory {
	
	private ToggleRefactoringContext context;

	public ToggleStrategyFactory(ToggleRefactoringContext context) {
		this.context = context;
	}
	
	public ToggleRefactoringStrategy getAppropriateStategy() throws NotSupportedException {
		assert(context.getDefinition() != null);
		if (isInImplementationSituation()) {
			System.out.println("ToggleFromImplementationToClassStrategy");
			if (context.getDeclarationUnit() == null
					|| context.getDeclarationUnit() == context
							.getDefinitionUnit())
				throw new NotSupportedException("Definition+declaration both in a cpp file -> not clear where to move the function body.");
			return new ToggleFromImplementationToClassStrategy(context);
		}
		if (isFreeFunction() && isAllInHeader()) {
			if (isScopedFreeFunction())
				throw new NotSupportedException("namespaced+templated free functions not supported yet");
			try {
				if (context.getTUForSiblingFile() == null)
					throw new NotSupportedException("Cannot decide where to put the code");
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
		throw new NotSupportedException("Unsupported situation for moving function body.");
	}
	
	private boolean isScopedFreeFunction() {
		return isNamespacedOrTemplated(context.getDefinition().getDeclarator(), context.getDeclaration());
	}

	private boolean isNamespacedOrTemplated(IASTFunctionDeclarator declarator, IASTFunctionDeclarator backup) {
		if (declarator.getName() instanceof ICPPASTQualifiedName)
			declarator = backup;
		IASTNode node = declarator;
		while (node != null) {
			if (node instanceof ICPPASTNamespaceDefinition
					|| node instanceof ICPPASTTemplateDeclaration)
				return true;
			node = node.getParent();
		}
		return false;
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
		System.out.println(p.getFileExtension());
		return p.getFileExtension().equals("h") || p.getFileExtension().equals("hpp");
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
