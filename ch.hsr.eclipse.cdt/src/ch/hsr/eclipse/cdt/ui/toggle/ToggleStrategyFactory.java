package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ToggleStrategyFactory {
	
	private ToggleRefactoringContext context;

	public ToggleStrategyFactory(ToggleRefactoringContext context) {
		this.context = context;
	}
	
	public ToggleRefactoringAbstractStrategy getAppropriateStategy(RefactoringStatus initStatus) {
		if (isInImplementationSituation()) 	
			return new ToggleFromImplementationToClassStrategy(context);
		else if (isFreeFunction() && isAllInHeader())
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
				ToggleFromInHeaderToImplementationStrategy stategy = new ToggleFromInHeaderToImplementationStrategy(context);
				return stategy;
			} catch (CModelException e) {
				initStatus.addFatalError(e.getMessage());
			} catch (CoreException e) {
				initStatus.addFatalError(e.getMessage());
			}
		} 
		return null;
	}
	
	private boolean isFreeFunction() {
		ICPPASTQualifiedName name = ToggleSelectionHelper.getQualifiedName(context.getDeclaration());
		int size = name.getNames().length;
		if (context.getDeclaration() != null && size == 1)
			return true;
		return false;
	}
	
	private boolean isAllInHeader() {
		return getFileExtension(context.getDeclaration().getFileLocation().getFileName()).equals("h") &&
		getFileExtension(context.getDefinition().getFileLocation().getFileName()).equals("h");
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
		IASTTranslationUnit unit = context.getDefinitionUnit();
		IASTFileLocation location = unit.getFileLocation();
		String filename = location.getFileName();
		String extension = getFileExtension(filename);
		
		if ((extension.equals("cpp") || extension.equals("c")))
			return true;
		return false;
	}
	
	private String getFileExtension(String fileName) {
		return fileName.replaceAll("(.)*\\.", "");
	}
}
