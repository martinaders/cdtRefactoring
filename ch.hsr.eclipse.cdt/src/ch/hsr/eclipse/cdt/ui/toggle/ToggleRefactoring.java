package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CCompositeChange;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {

	private TextSelection selection;
	private ToggleRefactoringAbstractStrategy strategy;
	private ToggleRefactoringContext context;
	
	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
		if (selection == null || file == null || project == null) {
			initStatus.addFatalError("Preconditions for this refactoring not fulfilled, aborting.");
			return;
		}
		this.selection = (TextSelection) selection;
		try {
			ResourcesPlugin.getWorkspace().save(false, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			lockIndex();
			context = new ToggleRefactoringContext(getIndex());
			context.findFileUnitTranslation(file, initStatus);
			context.findASTNodeName(selection, initStatus);
			context.findBinding(initStatus);
			context.findDeclaration(initStatus);
			context.findDefinition(initStatus);
		} catch (InterruptedException e) {
		} finally {
			unlockIndex();
		}

		if (initStatus.hasFatalError())
			return initStatus;
		
		strategy = new ToggleStrategyFactory(context).getAppropriateStategy(initStatus);
		return initStatus;
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
		} catch (IllegalArgumentException e) {
			CUIPlugin.log("Failed to commit changes. Building project may help.", e);
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		ModificationCollector collector = new ModificationCollector();
		collectModifications(pm, collector);
		CCompositeChange finalChange = collector.createFinalChange();
		strategy.removeNewlines(finalChange);
		finalChange.setDescription(new RefactoringChangeDescriptor(getRefactoringDescriptor()));
		return finalChange;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return super.checkFinalConditions(pm);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new EmptyRefactoringDescription();
	}
}
