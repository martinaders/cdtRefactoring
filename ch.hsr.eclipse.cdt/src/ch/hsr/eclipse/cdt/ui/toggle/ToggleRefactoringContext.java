package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
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
import org.eclipse.cdt.internal.ui.refactoring.utils.NodeHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
	
	private IASTTranslationUnit localTranslation;
	
	public ToggleRefactoringContext() {
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

	IIndex index = null;
	public void findFileUnitTranslation(IFile file, RefactoringStatus initStatus) {
		origin_file = file;
		IPath filepath =  new Path(file.getLocationURI().getRawPath());
		ICProject cProject = CoreModel.getDefault().create(filepath).getCProject();
		try {
			ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(file.getLocationURI(), cProject);
			index = CCorePlugin.getIndexManager().getIndex(cProject);
			index.acquireReadLock();
			localTranslation = tu.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
		} catch (CModelException e) {
			initStatus.addFatalError("Cannot find translation unit for file");
		} catch (CoreException e) {
			initStatus.addFatalError("Cannot find ast translation for file");
		} catch (InterruptedException e) {
			initStatus.addFatalError("Cannot acquireReadlock in Index");
		} finally {
			index.releaseReadLock();
		}
		if (localTranslation == null) {
			initStatus.addFatalError("Could not get TranslationUnit for file");
		}
	}

	IASTName element_name;
	public void findASTNodeName(TextSelection selection,
			RefactoringStatus initStatus) {
		element_name = localTranslation.getNodeSelector(null).findName(selection.getOffset(), selection.getLength());
		if (element_name == null) {
			element_name = localTranslation.getNodeSelector(null).findFirstContainedName(selection.getOffset(), selection.getLength());
			IASTFunctionDefinition fundef = NodeHelper.findFunctionDefinitionInAncestors(element_name);
			element_name = fundef.getDeclarator().getName();
		}
		if (element_name == null) {
			initStatus.addFatalError("Problems determining the selected function, aborting. Choose another selection.");
		}
	}
	IIndexBinding binding = null;
	private IFile origin_file;
	
	public void findBinding(RefactoringStatus initStatus) {
		try {
			index.acquireReadLock();
			binding = index.findBinding(element_name);
			if (binding == null) {
				initStatus.addFatalError("Cannot find binding for name: " + element_name);
			}
		} catch (InterruptedException e) {
			initStatus.addFatalError("Cannot acquire Readlock for Index");
		} catch (CoreException e) {
			initStatus.addFatalError(e.getMessage());
		} finally {
			index.releaseReadLock();
		}
	}

	public void findDeclaration(RefactoringStatus initStatus) {
		try {
			IIndexName[] dname = index.findDeclarations(binding);
			for(IIndexName iname : dname) {
				localTranslation = loadTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(localTranslation, iname, index);
				if (astname != null) {
					declaration = findFunctionDeclarator(astname);
					declaration_unit = localTranslation;
					break;
				}
			}
		} catch (CoreException e) {
			initStatus.addFatalError(e.getMessage());
		}
	}
	
	public void findDefinition(RefactoringStatus initStatus) {
		try {
			IIndexName[] defnames = index.findDefinitions(binding);
			for(IIndexName iname : defnames) {
				localTranslation = loadTUForNameinFile(iname);
				IASTName astname = IndexToASTNameHelper.findMatchingASTName(localTranslation, iname, index);
				if (astname != null) {
					definition = findFunctionDefinition(astname);
					definition_unit = localTranslation;
					break;
				}
			}
		} catch (CoreException e) {
			initStatus.addFatalError(e.getMessage());
		}
	}

	public IFile getOriginFile() {
		return origin_file;
	}
	
	public IASTTranslationUnit loadTUForSiblingFile() throws CModelException, CoreException {
		return ToggleSelectionHelper.getLocalTranslationUnitForFile(ToggleSelectionHelper.getSiblingFile(origin_file));
	}

	private IASTTranslationUnit loadTUForNameinFile(
			IIndexName iname) throws CModelException, CoreException {
		if (!isSameFileAsInTU(iname)) {
			IASTTranslationUnit asttu;
			IPath path = new Path(iname.getFileLocation().getFileName());
			ICProject cProject = CoreModel.getDefault().create(path).getCProject();
			ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(iname.getFile().getLocation(), cProject);
			asttu = tu.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
			return asttu;
		}
		return localTranslation;
	}
	
	private boolean isSameFileAsInTU(IIndexName iname) {
		return iname.getFileLocation().getFileName().equals(localTranslation.getFileLocation().getFileName());
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


	private IASTFunctionDefinition findFunctionDefinition(IASTNode node) {
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTFunctionDefinition)
				return (IASTFunctionDefinition) node;
		}
		return null;
	}


}
