package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ToggleStrategyFactory {
	
	private ToggleRefactoringContext context;

	public ToggleStrategyFactory(ToggleRefactoringContext context) {
		this.context = context;
	}
	
	public ToggleRefactoringAbstractStrategy getAppropriatedStategy(RefactoringStatus initStatus) {
		if (isFreeFunction() && 
				getFileExtension(context.getDeclaration().getFileLocation().getFileName()).equals("h") &&
				getFileExtension(context.getDefinition().getFileLocation().getFileName()).equals("h"))
			try {
				return new ToggleFreeFunctionFromInHeaderToImpl(context);
			} catch (Exception e1) {
				initStatus.addFatalError("could not create strategy from header to implementation");
			} 
		else if (isInClassSituation())
			return new ToggleFromClassToInHeaderStrategy(context);
		else if (isTemplateSituation())
			return new ToggleFromInHeaderToClassStrategy(context);
		else if (isinHeaderSituation()) {
			try {
				return new ToggleFromInHeaderToImplementationStragegy(context);
			} catch (CModelException e) {
				initStatus.addFatalError(e.getMessage());
			} catch (CoreException e) {
				initStatus.addFatalError(e.getMessage());
			}
		} else if (isInImplementationSituation()) 	
			return new ToggleFromImplementationToClassStragegy(context);
		return null;
	}
	
	private boolean isFreeFunction() {
		if (ToggleSelectionHelper.getAllQualifiedNames(context.getDeclaration()).isEmpty())
			return true;
		return false;
	}

	private boolean isinHeaderSituation() {
		return context.getDeclaration().getFileLocation().getFileName().equals(context.getDefinition().getFileLocation().getFileName());
	}
	
	private boolean isInClassSituation() {
		boolean samefile = context.getDeclaration().getFileLocation().getFileName().equals(context.getDefinition().getFileLocation().getFileName());
		boolean samenode = context.getDefinition().getDeclarator() == context.getDeclaration();
		return samefile && samenode;
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
		String extension1 = getFileExtension(context.getDeclarationUnit().getFileLocation().getFileName());
		String extension2 = getFileExtension(context.getDefinitionUnit().getFileLocation().getFileName());
		if (extension1.equals("h") && (extension2.equals("cpp") || extension2.equals("c")))
			return true;
		return false;
	}
	
	private String getFileExtension(String fileName) {
		return fileName.replaceAll("(.)*\\.", "");
	}
}
