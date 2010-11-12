package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.util.EditorUtility;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

/**
 * Determines whether a valid function was selected by the user to be able to
 * run the appropriate strategy for moving the function body to another
 * position.
 */
@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {

	private TextSelection selection;
	private ToggleRefactoringAbstractStrategy strategy;
	private ToggleRefactoringContext context;
	
	public ToggleRefactoring(IFile file, TextSelection selection, ICProject proj) {
		super(file, selection, null, proj);
		if (selection == null || file == null || project == null) {
			initStatus.addFatalError("Preconditions for this refactoring not fulfilled, aborting.");
			return;
		}
		this.selection = selection;
		IDE.saveAllEditors(new IResource[] {ResourcesPlugin.getWorkspace().getRoot()}, false);
	}

	/**
	 * indexer not available? use TU instead. (make an adapter?)
	 * 
	 *  search first contained node
	 *  	look at its type:
	 *  	1) ICPPASTTemplateDeclaration
	 *  	2) ICPPASTFunctionDefinition
	 *  	3) IASTSimpleDeclaration+child(ICPPASTFunctionDeclarator)
	 *    -> result found but IASTDeclarationStatement as parent -> drop reference and continue search.
	 *  if null do search the first enclosing node the same way
	 *  
	 *  => Abort if nothing found.
	 *  
	 *  extract a binding that can be searched by index.findDeclarations + index.findDefinitions (directly? Name-node?)
	 *  
	 * Selection is inside			other.cpp		Class.cpp		Class.h		sys_library.h	N/A
	 * Declaration found			other.cpp		Class.cpp		Class.h		sys_library.h	N/A
	 * Definition found				other.cpp		Class.cpp		Class.h		sys_library.h	N/A
	 * Occurrences					def_only		decl_only		def_decl	multi_declaration	multi_definition
	 * 
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			//waitForIndexer();
			if (initStatus.hasFatalError())
				return initStatus;
			lockIndex();
			initContext();
		} catch (InterruptedException e) {
		} finally {
			unlockIndex();
		}

		if (initStatus.hasFatalError())
			return initStatus;
		
		strategy = new ToggleStrategyFactory(context).getAppropriateStategy(initStatus);
		return initStatus;
	}

	private void initContext() {
		context = new ToggleRefactoringContext(getIndex());
		context.findFileUnitTranslation(file, initStatus);
		if (initStatus.hasFatalError())
			return;
		context.findASTNodeName(selection, initStatus);
		if (initStatus.hasFatalError())
			return;
		context.findBinding(initStatus);
		if (initStatus.hasFatalError())
			return;
		context.findDeclaration(initStatus);
		if (initStatus.hasFatalError())
			return;
		context.findDefinition(initStatus);
	}

	private void waitForIndexer() {
		final IIndexManager im = CCorePlugin.getIndexManager();
		System.out.println("before Join");
		im.joinIndexer(1, new NullProgressMonitor());
		if (!im.isProjectIndexed(project))
			initStatus.addFatalError("Indexer not yet finished. Be patient and then try again.");
		System.out.println("after Join");
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector modifications) throws CoreException {
		strategy.run(modifications);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new EmptyRefactoringDescription();
	}

	/**
	 * Some strategies create a new file which should be shown to the user. The
	 * TogglingActionDelegate invokes this method after having applied the
	 * collected changes and the new file has been created.
	 */
	public void openEditorIfNeeded() {
		if (strategy.shouldOpenFile == null)
			return;
		try {
			CEditor editor = (CEditor) EditorUtility.openInEditor(strategy.shouldOpenFile, null);
			editor.setSelection(strategy.sourceRangeToBeShown, true);
		} catch (PartInitException e) {
		}
	}
}
