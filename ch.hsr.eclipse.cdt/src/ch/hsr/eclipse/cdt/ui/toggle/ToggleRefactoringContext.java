package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
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
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.utils.NodeHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.TranslationUnitHelper;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import ch.hsr.ifs.redhead.helpers.IndexToASTNameHelper;

@SuppressWarnings("restriction")
public class ToggleRefactoringContext {

	private IASTFunctionDefinition definition;
	private IASTFunctionDeclarator declaration;
	private IASTTranslationUnit definition_unit;
	private IASTTranslationUnit declaration_unit;
	IIndex index = null;

	private IASTTranslationUnit localTranslation;

	public ToggleRefactoringContext(IIndex index) {
		this.index = index;
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

	public void findFileUnitTranslation(IFile file, RefactoringStatus initStatus) {
		origin_file = file;
		try {
			localTranslation = TranslationUnitHelper.loadTranslationUnit(file,
					true);
		} catch (CModelException e) {
			initStatus.addFatalError("Cannot find translation unit for file");
		} catch (CoreException e) {
			initStatus.addFatalError("Cannot find ast translation for file");
		}
		if (localTranslation == null) {
			initStatus.addFatalError("Could not get TranslationUnit for file");
		}
	}

	IASTName element_name;

	public void findASTNodeName(TextSelection selection,
			RefactoringStatus initStatus) {

		IASTNode node = localTranslation.getNodeSelector(null)
				.findFirstContainedNode(selection.getOffset(),
						selection.getLength());
		IASTFunctionDefinition fundef = NodeHelper
				.findFunctionDefinitionInAncestors(node);
		if (fundef != null) {
			element_name = fundef.getDeclarator().getName();
			return;
		}

		node = localTranslation.getNodeSelector(null).findEnclosingNode(
				selection.getOffset(), selection.getLength());
		fundef = NodeHelper.findFunctionDefinitionInAncestors(node);
		if (fundef != null) {
			element_name = fundef.getDeclarator().getName();
			return;
		}

		node = localTranslation.getNodeSelector(null).findEnclosingName(
				selection.getOffset(), selection.getLength());
		IASTFunctionDeclarator fundec = findFunctionDeclarationInAncestors(node);
		if (fundec != null) {
			element_name = fundec.getName();
			return;
		}

		if (element_name == null) {
			initStatus
					.addFatalError("Problems determining the selected function, aborting. Choose another selection.");
		}
	}

	private IASTFunctionDeclarator findFunctionDeclarationInAncestors(
			IASTNode node) {
		while (node != null) {
			if (node instanceof IASTFunctionDeclarator) {
				return (IASTFunctionDeclarator) node;
			}
			node = node.getParent();
		}
		return null;
	}

	IIndexBinding binding = null;
	private IFile origin_file;

	public void findBinding(RefactoringStatus initStatus) {
		try {
			binding = index.findBinding(element_name);
			if (binding == null) {
				System.out.println("binding is null");
			}
		} catch (CoreException e) {
			initStatus.addFatalError(e.getMessage());
		} finally {
			index.releaseReadLock();
		}
	}

	private IASTFunctionDefinition definitiontryfallbackwithast(final IASTName element_name2) {
		final Container<IASTFunctionDefinition> container = new Container<IASTFunctionDefinition>();
		localTranslation.accept(new CPPASTVisitor(true) {
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
		localTranslation.accept(new CPPASTVisitor(true) {
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
	
	public void findDeclaration(RefactoringStatus initStatus) {
		if (binding == null) {
			declaration = declarationtryfallbackwithast(element_name);
			declaration_unit = null;
			return;
		}
		try {
			IIndexName[] decnames = index.findDeclarations(binding);
			for (IIndexName iname : decnames) {
				localTranslation = loadTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						localTranslation, iname, index);
				if (astname != null) {
					declaration = findFunctionDeclarator(astname);
					declaration_unit = localTranslation;
					break;
				}
			}
			if (declaration == null)
				initStatus
						.addFatalError("Could not determine selection. Select a function name.");
		} catch (CoreException e) {
			initStatus.addFatalError(e.getMessage());
		}
	}

	public void findDefinition(RefactoringStatus initStatus) {
		// fallback
		if (binding == null) {
			definition = definitiontryfallbackwithast(element_name);
			definition_unit = localTranslation;
			return;
		}
		try {
			IIndexName[] defnames = index.findDefinitions(binding);
			for (IIndexName iname : defnames) {
				localTranslation = loadTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(
						localTranslation, iname, index);
				if (astname != null) {
					definition = findFunctionDefinition(astname);
					definition_unit = localTranslation;
					break;
				}
			}
			if (definition == null)
				initStatus
						.addFatalError("Could not determine selection. Select a function name.");
		} catch (CoreException e) {
			initStatus.addFatalError(e.getMessage());
		}
	}

	public IFile getOriginFile() {
		return origin_file;
	}

	public IASTTranslationUnit loadTUForSiblingFile() throws CModelException,
			CoreException {
		URI uri = ToggleSelectionHelper.getSiblingFile(origin_file);
		if (uri == null)
			return null;
		return ToggleSelectionHelper.getLocalTranslationUnitForFile(uri);
	}

	private IASTTranslationUnit loadTUForNameinFile(IIndexName iname)
			throws CModelException, CoreException {
		if (!isSameFileAsInTU(iname)) {
			IASTTranslationUnit asttu = null;
			IPath path = new Path(iname.getFileLocation().getFileName());
			try {
				index.acquireReadLock();
				asttu = TranslationUnitHelper.loadTranslationUnit(
						path.toString(), true);
			} catch (InterruptedException e) {
				CUIPlugin.log("Interruption during index locking.", e);
			} finally {
				index.releaseReadLock();
			}
			return asttu;
		}
		return localTranslation;
	}

	private boolean isSameFileAsInTU(IIndexName iname) {
		return iname.getFileLocation().getFileName()
				.equals(localTranslation.getFileLocation().getFileName());
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
		return origin_file;
	}
}
