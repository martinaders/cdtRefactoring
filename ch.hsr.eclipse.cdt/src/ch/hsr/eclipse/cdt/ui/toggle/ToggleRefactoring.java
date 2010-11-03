package ch.hsr.eclipse.cdt.ui.toggle;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTImplicitName;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTImplicitName;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.NodeHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.TranslationUnitHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.internal.ui.refactoring.util.Strings;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;

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
			name = fundef.getDeclarator().getName();
		}
		if (name == null) {
			initStatus.addFatalError("Problems determining the selected function, aborting. Choose another selection.");
			return initStatus;
		}
		System.out.println(name.getClass() + ", " + name.getRawSignature());
		
		System.out.println("name was selected: " + name);
		try {
			lockIndex();
			IIndexBinding binding = getIndex().findBinding(name);
			for(IIndexName iname : getIndex().findDeclarations(binding)) {
				System.out.println("iname-dec : " + iname.getNodeOffset() + " " + iname.getFile());
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(unit, iname, getIndex());
				if (astname != null) {
					selectedDeclaration = findFunctionDeclarator(astname);
					System.out.println(" -> " + selectedDeclaration);
					break;
				}
			}
			for(IIndexName iname : getIndex().findDefinitions(binding)) {
				System.out.println("iname-def : " + iname.getNodeOffset() + " " + iname.getFile());
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(unit, iname, getIndex());
				if (astname != null) {
					selectedDefinition = findFunctionDefinition(astname);
					System.out.println(" -> " + selectedDefinition);
					break;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			unlockIndex();
		}

		if (selectedDeclaration == null || selectedDefinition == null) {
			System.err.println("fuck");
			initStatus
			.addFatalError("declaration AND definition needed. Cannot toggle.");
			return initStatus;
		}
		
		if (isInClassSituation()) {
			System.out.println("inclass");
			strategy = new ToggleFromClassToInHeaderStrategy(selectedDeclaration, selectedDefinition, unit);
		}
		else if (isTemplateSituation())
			strategy = new ToggleFromInHeaderToClassStrategy(selectedDeclaration, selectedDefinition, unit);
		else if (isinHeaderSituation()) {
			IASTTranslationUnit sibling_unit = ToggleSelectionHelper.getLocalTranslationUnitForFile(ToggleSelectionHelper.getSiblingFile(file, project));
			strategy = new ToggleFromInHeaderToImplementationStragegy(selectedDeclaration, selectedDefinition, unit, sibling_unit);
		} else if (isInImplementationSituation()) {
			System.out.println("this is 4th style");
			//strategy = new ToggleFromImplementationToClassStragegy(selectedDeclaration, selectedDefinition, unit, project, file);
		}
		return initStatus;
	}

	private IASTFunctionDefinition findFunctionDefinition(IASTNode node) {
		System.out.print("Analyzing parents: ");
		while(node.getParent() != null) {
			node = node.getParent();
			System.out.print(node.getClass() + ", ");
			if (node instanceof ICPPASTFunctionDefinition)
				return (IASTFunctionDefinition) node;
		}
		return null;
	}

	private CPPASTFunctionDeclarator findFunctionDeclarator(IASTNode node) {
		System.out.print("Analyzing parents: " + node.getClass() + ", "); //IASTCPPTypeSpecifier
		if (node instanceof IASTSimpleDeclaration) {
			System.out.println("\nHell yeah :-)");
			return (CPPASTFunctionDeclarator)((IASTSimpleDeclaration)node).getDeclarators()[0];
		}
		while(node.getParent() != null) {
			node = node.getParent();
			System.out.print(node.getClass() + ", ");
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
		System.out.println(selectedDeclaration.getFileLocation().getFileName());
		System.out.println(selectedDefinition.getFileLocation().getFileName());
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
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return super.checkFinalConditions(pm);
	}
}
