package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.core.pdom.indexer.TranslationUnitCollector;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Responsible for starting the ToggleRefactoring when the user invokes the
 * refactoring.
 * 
 * Order of execution is: constructor, init, selectionChanged, run
 */
public class TogglingActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private TextSelection selection;
	private ICProject project;
	private IFile file;
	
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
		assert (window != null);
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		boolean isTextSelection = selection instanceof TextSelection;
		action.setEnabled(isTextSelection);
		if (!isTextSelection)
			return;
		this.selection = (TextSelection) selection;
	}

	@Override
	public void run(IAction action) {
		if (!initialize()) {
			MessageDialog.openInformation(window.getShell(), "Information",
					"Toggling function body is not available.");
			return;
		}
		runRefactoring(new ToggleRefactoring(file, selection, project));
	}
	
	private boolean initialize() {
		IWorkbenchPage activePage = window.getActivePage();
		if (activePage == null)
			return false;
		IEditorPart editor = activePage.getActiveEditor();
		if (editor == null || editor.getEditorInput() == null)
			return false;
		IWorkingCopy wc = CUIPlugin.getDefault().getWorkingCopyManager()
		.getWorkingCopy(editor.getEditorInput());
		if (wc == null)
			return false;
		project = wc.getCProject();
		file = (IFile) wc.getResource();
		return project != null && file != null;
	}

	@SuppressWarnings("restriction")
	private void runRefactoring(final ToggleRefactoring refactoring) {
			Job job = new Job("'move body' code automation") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());
						if (status.hasFatalError())
							return JobStatus.OK_STATUS;
						Change change = refactoring.createChange(new NullProgressMonitor());
						change.perform(new NullProgressMonitor());
						
						refactoring.openEditorIfNeeded();
					} catch (Exception e) {
						System.err.println("Failure during generation of changes." + e.getMessage() + e.getClass() + "\n");
						e.printStackTrace();
					}
					return JobStatus.OK_STATUS;
				}
			};
			job.setPriority(Job.SHORT);
			job.schedule();
	}

	@Override
	public void dispose() {
	}
}

//			IIndexManager im = CCorePlugin.getIndexManager();
//			ArrayList<ITranslationUnit> sources = new ArrayList<ITranslationUnit>();
//			ArrayList<ITranslationUnit> headers = new ArrayList<ITranslationUnit>();
//			TranslationUnitCollector visitor = new TranslationUnitCollector(sources, headers, new NullProgressMonitor());
//			project.accept(visitor);
//			System.out.println("files found: " + sources.size() + ", " + headers.size());
//			
//			im.update(new ICElement[] {sources.get(0), sources.get(1), headers.get(0)}, IIndexManager.UPDATE_CHECK_CONTENTS_HASH);