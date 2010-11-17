package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.pdom.indexer.IndexerPreferences;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.ide.IDE;

/**
 * Determines whether a valid function was selected by the user to be able to
 * run the appropriate strategy for moving the function body to another
 * position.
 */
@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {

	private TextSelection selection;
	private ToggleRefactoringStrategy strategy;
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
			IndexerPreferences.set(project.getProject(), IndexerPreferences.KEY_INDEX_UNUSED_HEADERS_WITH_DEFAULT_LANG, Boolean.TRUE.toString());
			context = new ToggleRefactoringContext(getIndex(), file, selection);
			strategy = new ToggleStrategyFactory(context).getAppropriateStategy();
		} catch (InterruptedException e) {
		} catch (NotSupportedException e) {
			System.err.println("not implemented: " + e.getMessage());
			initStatus.addFatalError("fatal");
		} finally {
			unlockIndex();
		}

		return initStatus;
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
}
