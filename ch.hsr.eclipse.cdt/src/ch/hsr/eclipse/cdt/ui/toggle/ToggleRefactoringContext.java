package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.ui.refactoring.utils.TranslationUnitHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.TextSelection;

import ch.hsr.ifs.redhead.helpers.IndexToASTNameHelper;

@SuppressWarnings("restriction")
public class ToggleRefactoringContext {

	private IASTFunctionDefinition targetDefinition;
	private IASTFunctionDeclarator targetDeclaration;
	private IASTTranslationUnit targetDefinitionUnit;
	private IASTTranslationUnit targetDeclarationUnit;
	private IIndex index;
	private IASTTranslationUnit selectionUnit;
	private IFile selectionFile;
	private IIndexBinding binding;
	private IASTName selectionName;

	public ToggleRefactoringContext(IIndex index, IFile file, TextSelection selection) throws NotSupportedException {
		this.index = index;
		this.selectionFile = file; 			System.out.print("Stage 1: ");
		findSelectionUnit();				System.out.print("complete\nStage 2: ");
		findSelectedFunctionDeclarator(selection); System.out.print("complete\nStage 3: ");
		findBinding();						System.out.print("complete\nStage 4: ");
		findDeclaration();					System.out.print("complete\nStage 5: ");
		findDefinition();					System.out.print("complete\n");
	}
	
	private void findSelectionUnit() throws NotSupportedException {
		try {
			selectionUnit = TranslationUnitHelper.loadTranslationUnit(
					selectionFile, true);
		} catch (Exception e) {
		}
		if (selectionUnit == null)
			throw new NotSupportedException(
					"not able to work without translation unit");
	}
	
	public void findSelectedFunctionDeclarator(TextSelection selection) throws NotSupportedException {
		selectionName = new DeclaratorFinder(selection, selectionUnit).getName();
	}
	
	public void findBinding() throws NotSupportedException {
		try {
			binding = index.findBinding(selectionName);
		} catch (CoreException e) {
		}
		if (binding == null)
			throw new NotSupportedException("not able to work without a binding");
	}
	
	// Declaration may be null afterwards, but thats ok.
	public void findDeclaration() throws NotSupportedException {
//		if (binding == null) {
//			declaration = declarationtryfallbackwithast(selectionName);
//			declaration_unit = null;
//			return;
//		}
		try {
			IIndexName[] decnames = index.findNames(binding, IIndex.FIND_DECLARATIONS);
			for (IIndexName iname : decnames) {
				selectionUnit = getTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						selectionUnit, iname, index);
				if (astname != null) {
					targetDeclaration = findFunctionDeclarator(astname);
					targetDeclarationUnit = selectionUnit;
					break;
				}
			}
		} catch (CoreException e) {
		}
		if (targetDeclaration == null)
			System.out.println("no declaration found -> create one if possible.");
	}
	
	public void findDefinition() throws NotSupportedException {
		// fallback
//		if (binding == null) {
//			definition = definitiontryfallbackwithast(selectionName);
//			definition_unit = selectionUnit;
//			return;
//		}
		try {
			IIndexName[] defnames = index.findNames(binding, IIndex.FIND_DEFINITIONS);
			for (IIndexName iname : defnames) {
				IASTTranslationUnit unit = getTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						unit, iname, index);
				if (astname != null) {
					targetDefinition = findFunctionDefinition(astname);
					targetDefinitionUnit = unit;
					break;
				}
			}
		} catch (CoreException e) {
		}
		if (targetDefinition == null)
			throw new NotSupportedException("cannot work without definition");
	}

	public IASTFunctionDeclarator getDeclaration() {
		return targetDeclaration;
	}

	public IASTFunctionDefinition getDefinition() {
		return targetDefinition;
	}

	public IASTTranslationUnit getDeclarationUnit() {
		return targetDeclarationUnit;
	}

	public IASTTranslationUnit getDefinitionUnit() {
		return targetDefinitionUnit;
	}

	public IFile getSelectionFile() {
		return selectionFile;
	}

	public IASTTranslationUnit getTUForSiblingFile() throws CModelException,
			CoreException {
		URI uri = ToggleSelectionHelper.getSiblingFile(selectionFile);
		if (uri == null)
			return null;
		return ToggleSelectionHelper.getLocalTranslationUnitForFile(uri);
	}

	private IASTTranslationUnit getTUForNameinFile(IIndexName iname)
			throws CModelException, CoreException {
		if (isSameFileAsInTU(iname))
			return selectionUnit;
		IASTTranslationUnit asttu = null;
		IPath path = new Path(iname.getFileLocation().getFileName());
		asttu = TranslationUnitHelper.loadTranslationUnit(path.toString(),
				true);
		return asttu;
	}

	private boolean isSameFileAsInTU(IIndexName iname) {
		return iname.getFileLocation().getFileName()
				.equals(selectionUnit.getFileLocation().getFileName());
	}

	private IASTFunctionDeclarator findFunctionDeclarator(IASTNode node) {
		if (node instanceof IASTSimpleDeclaration) {
			return (IASTFunctionDeclarator) ((IASTSimpleDeclaration) node)
					.getDeclarators()[0];
		}
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof IASTFunctionDeclarator)
				return (IASTFunctionDeclarator) node;
		}
		return null;
	}

	private IASTFunctionDefinition findFunctionDefinition(IASTNode node) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof IASTFunctionDefinition)
				return (IASTFunctionDefinition) node;
		}
		return null;
	}
}
