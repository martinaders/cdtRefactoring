package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.ui.refactoring.CCompositeChange;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.NodeHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import ch.hsr.ifs.redhead.helpers.IndexToASTNameHelper;

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
		if (unit == null) {
			initStatus.addFatalError("Could not get TranslationUnit for file");
			return initStatus;
		}

		IASTName name = unit.getNodeSelector(null).findName(selection.getOffset(), selection.getLength());
		if (name == null) {
			name = unit.getNodeSelector(null).findFirstContainedName(selection.getOffset(), selection.getLength());
			IASTFunctionDefinition fundef = NodeHelper.findFunctionDefinitionInAncestors(name);
			if (fundef == null)
				name = unit.getNodeSelector(null).findEnclosingName(selection.getOffset(), selection.getLength());
			else
				name = fundef.getDeclarator().getName();
		}
		if (name == null) {
			initStatus.addFatalError("Problems determining the selected function, aborting. Choose another selection.");
			return initStatus;
		}

		IIndexName[] decnames = null;
		IIndexName[] defnames = null;
		IASTTranslationUnit localtu = null;
		try {
			lockIndex();
			IIndex index = getIndex();
			IIndexBinding binding = getIndex().findBinding(name);
			decnames = index.findDeclarations(binding);
			defnames = index.findDefinitions(binding);
			
			for(IIndexName iname : decnames) {
				IASTName astname = null;
				if (!iname.getFileLocation().getFileName().equals(unit.getFileLocation().getFileName())) {
					try {
						localtu = ToggleSelectionHelper.getLocalTranslationUnitForFile(new URI(iname.getFileLocation().getFileName()));
						astname = IndexToASTNameHelper.findMatchingASTName(localtu, iname, index);
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				} else
					astname = IndexToASTNameHelper.findMatchingASTName(unit, iname, index);
				if (astname != null) {
					selectedDeclaration = findFunctionDeclarator(astname);
					break;
				}
			}
			
			for(IIndexName iname : defnames) {
				IASTName astname = null;
				if (!iname.getFileLocation().getFileName().equals(unit.getFileLocation().getFileName())) {
					try {
						localtu = ToggleSelectionHelper.getLocalTranslationUnitForFile(new URI(iname.getFileLocation().getFileName()));
						astname = IndexToASTNameHelper.findMatchingASTName(localtu, iname, index);
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				} else
					astname = IndexToASTNameHelper.findMatchingASTName(unit, iname, index);
				if (astname != null) {
					selectedDefinition = findFunctionDefinition(astname);
					break;
				}
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			unlockIndex();
		}

		if (selectedDeclaration == null || selectedDefinition == null) {
			initStatus
			.addFatalError("declaration AND definition needed. Cannot toggle.");
			return initStatus;
		}
		
		if (isInClassSituation()) {
			strategy = new ToggleFromClassToInHeaderStrategy(selectedDeclaration, selectedDefinition, unit, file);
		}
		else if (isTemplateSituation())
			strategy = new ToggleFromInHeaderToClassStrategy(selectedDeclaration, selectedDefinition, unit);
		else if (isinHeaderSituation()) {
			IASTTranslationUnit sibling_unit = ToggleSelectionHelper.getLocalTranslationUnitForFile(ToggleSelectionHelper.getSiblingFile(file, project));
			strategy = new ToggleFromInHeaderToImplementationStragegy(selectedDeclaration, selectedDefinition, unit, sibling_unit);
		} else if (isInImplementationSituation()) {			
			IASTTranslationUnit declaration_unit = null;
			IASTTranslationUnit definition_unit = null;
			if (getFileExtension(unit.getFileLocation().getFileName()).equals("h"))
				declaration_unit = unit;
			else if (getFileExtension(localtu.getFileLocation().getFileName()).equals("h"))
				declaration_unit = localtu;
			if (getFileExtension(unit.getFileLocation().getFileName()).equals("cpp"))
				definition_unit = unit;
			else if (getFileExtension(localtu.getFileLocation().getFileName()).equals("cpp"))
				definition_unit = localtu;
			
			strategy = new ToggleFromImplementationToClassStragegy(selectedDeclaration, selectedDefinition, definition_unit, declaration_unit);
		}
		return initStatus;
	}

	private IASTFunctionDefinition findFunctionDefinition(IASTNode node) {
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTFunctionDefinition)
				return (IASTFunctionDefinition) node;
		}
		return null;
	}

	private CPPASTFunctionDeclarator findFunctionDeclarator(IASTNode node) {
		if (node instanceof IASTSimpleDeclaration) {
			return (CPPASTFunctionDeclarator)((IASTSimpleDeclaration)node).getDeclarators()[0];
		}
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTFunctionDeclarator)
				return (CPPASTFunctionDeclarator) node;
		}
		return null;
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
		return selectedDefinition.getDeclarator() == selectedDeclaration && 
		selectedDeclaration.getFileLocation().getFileName().equals(selectedDefinition.getFileLocation().getFileName());
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
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		ModificationCollector collector = new ModificationCollector();
		collectModifications(pm, collector);
		CCompositeChange finalChange = collector.createFinalChange();
		strategy.removeTrailingNewlines(finalChange);
		finalChange.setDescription(new RefactoringChangeDescriptor(getRefactoringDescriptor()));
		return finalChange;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return super.checkFinalConditions(pm);
	}
}
