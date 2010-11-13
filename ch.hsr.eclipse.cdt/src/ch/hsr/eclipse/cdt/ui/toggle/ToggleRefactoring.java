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
		if (selection == null || file == null || project == null)
			initStatus.addFatalError("Invalid selection");
		if (!IDE.saveAllEditors(new IResource[] {ResourcesPlugin.getWorkspace().getRoot()}, false))
			initStatus.addFatalError("Cannot save files");
		this.selection = selection;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			IIndexManager im = CCorePlugin.getIndexManager();
			if (!im.isProjectIndexed(project))
				throw new NotSupportedException("not able to work without the indexer");
			lockIndex();
			context = new ToggleRefactoringContext(getIndex(), file, selection);
			strategy = new ToggleStrategyFactory(context).getAppropriateStategy(initStatus);
			return initStatus;
		} catch (InterruptedException e) {
			return initStatus;
		} catch (NotSupportedException e) {
			System.err.println("not implemented: " + e.getMessage());
			initStatus.addFatalError("fatal");
			return initStatus;
		} finally {
			unlockIndex();
		}
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
