package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {

	private IASTFunctionDefinition selectedDefinition;
	private CPPASTFunctionDeclarator selectedDeclaration;
	private TextSelection selection;
	private ToggleRefactoringAbstractStrategy strategy;
	
	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
		this.selection = (TextSelection) selection;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		unit = ToggleSelectionHelper.getLocalTranslationUnitForFile(file.getLocationURI());
		
		selectedDeclaration = ToggleSelectionHelper.getSelectedDeclaration(unit, selection);
		selectedDefinition = ToggleSelectionHelper.getSelectedDefinition(unit, selection);
		
		if (isInClassSituation())
			strategy = new ToggleFromClassToInHeaderStrategy(selectedDeclaration, selectedDefinition, unit);
		else if (isTemplateSituation())
			strategy = new ToggleFromInHeaderToClassStrategy(selectedDeclaration, selectedDefinition, unit);
		else if (isinHeaderSituation()) {
			IASTTranslationUnit sibling_unit = ToggleSelectionHelper.getLocalTranslationUnitForFile(ToggleSelectionHelper.getSiblingFile(file, project));
			strategy = new ToggleFromInHeaderToImplementationStragegy(selectedDeclaration, selectedDefinition, unit, sibling_unit);
		}
//		else if (isInImplementationSituation())
//			strategy = new ToggleFromImplementationToClassStragegy(selectedDeclaration, selectedDefinition, unit, project, file);
		
		if (selectedDeclaration == null || selectedDefinition == null) {
			initStatus
					.addFatalError("declaration AND definition needed. Cannot toggle.");
		}
		return initStatus;
	}

	private boolean isInImplementationSituation() {
		String extension1 = getFileExtension(selectedDeclaration.getFileLocation().getFileName());
		String extension2 = getFileExtension(selectedDefinition.getFileLocation().getFileName());
		if (extension1.equals("h") && extension2.equals("cpp"))
			return true;
		return false;
	}

	private String getFileExtension(String fileName) {
		return fileName.replaceAll("(.)*\\.", "");
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new EmptyRefactoringDescription();
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector modifications) throws CoreException {
		try {
			lockIndex();
			try {
				strategy.run(modifications);
			} finally {
				unlockIndex();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	private boolean isinHeaderSituation() {
		return selectedDeclaration.getFileLocation().getFileName().equals(selectedDefinition.getFileLocation().getFileName());
	}
	
	private boolean isInClassSituation() {
		return selectedDefinition.getDeclarator() == selectedDeclaration;
	}

	private boolean isTemplateSituation() {
		IASTNode node = selectedDefinition;
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTTemplateDeclaration)
				return true;
		}
		return false;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return super.checkFinalConditions(pm);
	}
}
