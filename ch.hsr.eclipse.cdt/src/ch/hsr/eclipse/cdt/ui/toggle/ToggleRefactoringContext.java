package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.utils.TranslationUnitHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.TextSelection;

import ch.hsr.ifs.redhead.helpers.IndexToASTNameHelper;

@SuppressWarnings("restriction")
public class ToggleRefactoringContext {

	private IASTFunctionDefinition definition;
	private IASTFunctionDeclarator declaration;
	private IASTTranslationUnit definition_unit;
	private IASTTranslationUnit declaration_unit;
	private IIndex index = null;
	private IASTTranslationUnit selectionUnit;
	private IFile selectionFile;

	public ToggleRefactoringContext(IIndex index, IFile file, TextSelection selection) throws NotSupportedException {
		this.index = index;
		this.selectionFile = file;
		findFileUnitTranslation();
		findAffectedFunctionDeclarator(selection);
		findBinding();
		findDeclaration();
		findDefinition();
	}

	public IASTFunctionDeclarator getDeclaration() {
		return declaration;
	}

	public IASTFunctionDefinition getDefinition() {
		return definition;
	}

	public IASTTranslationUnit getDeclarationUnit() {
		return declaration_unit;
	}

	public IASTTranslationUnit getDefinitionUnit() {
		return definition_unit;
	}

	public void findFileUnitTranslation()
			throws NotSupportedException {
		try {
			selectionUnit = TranslationUnitHelper.loadTranslationUnit(selectionFile, true);
		} catch (Exception e) {
		}
		if (selectionUnit == null)
			throw new NotSupportedException("not able to work without translation unit");
	}

	public void findAffectedFunctionDeclarator(TextSelection selection) throws NotSupportedException {
		element_name = new DeclaratorFinder(selection, selectionUnit).getName();
	}

	IIndexBinding binding = null;
	private IASTName element_name;

	public void findBinding() throws NotSupportedException {
		try {
			binding = index.findBinding(element_name);
		} catch (CoreException e) {
		}
		if (binding == null)
			throw new NotSupportedException("not able to work without a binding");
	}

	private IASTFunctionDefinition definitiontryfallbackwithast(final IASTName element_name2) {
		final Container<IASTFunctionDefinition> container = new Container<IASTFunctionDefinition>();
		selectionUnit.accept(new CPPASTVisitor(true) {
			{
				shouldVisitDeclarations = true;
			}

			@Override
			public int visit(IASTDeclaration declaration) {
				if (declaration instanceof ICPPASTFunctionDefinition) {
					CPPASTFunctionDefinition func = (CPPASTFunctionDefinition) declaration;
					IASTName name = func.getDeclarator().getName();
					if (name.equals(element_name2)) {
						System.out.println("got it");
						container.setObject(func);
						return PROCESS_ABORT;
					}
				}
				return super.visit(declaration);
			}
		});
		return container.getObject();
	}

	private IASTFunctionDeclarator declarationtryfallbackwithast(final IASTName element_name2) {
		final Container<IASTFunctionDeclarator> container = new Container<IASTFunctionDeclarator>();
		selectionUnit.accept(new CPPASTVisitor(true) {
			{
				shouldVisitDeclarations = true;
			}

			@Override
			public int visit(IASTDeclaration declaration) {
				if (declaration instanceof IASTFunctionDeclarator) {
					CPPASTFunctionDeclarator decl = (CPPASTFunctionDeclarator) declaration;
					IASTName name = decl.getName();
					
					if (name.equals(element_name2)) {
						System.out.println("got it");
						container.setObject(decl);
						return PROCESS_ABORT;
					}
				}
				return super.visit(declaration);
			}
		});
		return container.getObject();
	}
	
	public void findDeclaration() throws NotSupportedException {
		if (binding == null) {
			declaration = declarationtryfallbackwithast(element_name);
			declaration_unit = null;
			return;
		}
		try {
			IIndexName[] decnames = index.findDeclarations(binding);
			for (IIndexName iname : decnames) {
				selectionUnit = loadTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						selectionUnit, iname, index);
				if (astname != null) {
					declaration = findFunctionDeclarator(astname);
					declaration_unit = selectionUnit;
					break;
				}
			}
		} catch (CoreException e) {
		}
		if (declaration == null)
			throw new NotSupportedException(
					"cannot work without a declaration");
	}

	public void findDefinition() throws NotSupportedException {
		// fallback
		if (binding == null) {
			definition = definitiontryfallbackwithast(element_name);
			definition_unit = selectionUnit;
			return;
		}
		try {
			IIndexName[] defnames = index.findDefinitions(binding);
			for (IIndexName iname : defnames) {
				selectionUnit = loadTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						selectionUnit, iname, index);
				if (astname != null) {
					definition = findFunctionDefinition(astname);
					definition_unit = selectionUnit;
					break;
				}
			}
		} catch (CoreException e) {
		}
		if (definition == null)
			throw new NotSupportedException("cannot work without definition");
	}

	public IFile getOriginFile() {
		return selectionFile;
	}

	public IASTTranslationUnit loadTUForSiblingFile() throws CModelException,
			CoreException {
		URI uri = ToggleSelectionHelper.getSiblingFile(selectionFile);
		if (uri == null)
			return null;
		return ToggleSelectionHelper.getLocalTranslationUnitForFile(uri);
	}

	private IASTTranslationUnit loadTUForNameinFile(IIndexName iname)
			throws CModelException, CoreException {
		if (!isSameFileAsInTU(iname)) {
			IASTTranslationUnit asttu = null;
			IPath path = new Path(iname.getFileLocation().getFileName());
			asttu = TranslationUnitHelper.loadTranslationUnit(path.toString(),
					true);
			return asttu;
		}
		return selectionUnit;
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

	public IFile getFile() {
		return selectionFile;
	}

}
