package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.editors.text.TextEditor;

import ch.hsr.eclipse.cdt.Activator;

/**
 * Given a selection and a translation unit, this class finds a
 * ICPPASTFunctionDeclarator if possible. Nested local functions are skipped
 * during search.
 */
public class DeclaratorFinder {

	private IASTFunctionDeclarator foundDeclarator;

	public DeclaratorFinder(TextSelection selection, IASTTranslationUnit unit)
			throws NotSupportedException {
		foundDeclarator = findDeclaratorInSelection(selection, unit);

		if (foundDeclarator == null)
			throw new NotSupportedException("cannot work without declarator");

		if (isPartOfAStatement(foundDeclarator))
			throw new NotSupportedException(
					"Nested function declarations not supported");
	}

	public IASTName getName() {
		return foundDeclarator.getName();
	}

	private IASTFunctionDeclarator findDeclaratorInSelection(
			TextSelection selection, IASTTranslationUnit unit) {
		IASTFunctionDeclarator result = findAffectedNode(selection, unit);
		if (result == null) {
//			try {
//				ICProject[] projects= CoreModel.getDefault().getCModel().getCProjects();
//				IIndex i = CCorePlugin.getIndexManager().getIndex(project);
//				i.releaseReadLock();
//			TextEditor editor = (TextEditor) CUIPlugin.getActivePage().getActiveEditor();
//			IWorkingCopy wc = CUIPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editor.getEditorInput());
//			final ICProject project = wc.getCProject();
//			final IIndexManager im = CCorePlugin.getIndexManager();
//				Job job = new Job("update index") {
//
//					@Override
//					protected IStatus run(IProgressMonitor monitor) {
//						System.out.println("Manually reindexing project...");
//						try {
//							im.update(new ICElement[] {project}, IIndexManager.UPDATE_CHECK_CONTENTS_HASH);
//							System.out.println("update started");
//						} catch (CoreException e) {
//							e.printStackTrace();
//						}
//						return null;
//					}
//				};
//				job.setPriority(Job.LONG);
//				job.schedule();
				
//				System.out.println("before join");
//				im.joinIndexer(5000, new NullProgressMonitor());
//				System.out.println("... indexer joined");
//				while (!im.isIndexerIdle() || im.isIndexerSetupPostponed(project)) {
//					Thread.sleep(500);
//				}
//			} catch (CoreException e) {
//				System.out.println("deep s. exception");
//			}
//			catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			result = findAffectedNode(selection, unit);
		}
		
		return result;
	}

	private IASTFunctionDeclarator findAffectedNode(TextSelection selection,
			IASTTranslationUnit unit) {
		IASTNode firstNodeInsideSelection = unit.getNodeSelector(null)
				.findFirstContainedNode(selection.getOffset(),
						selection.getLength());
		IASTFunctionDeclarator result = findDeclaratorInAncestors(firstNodeInsideSelection);

		if (result == null) {
			firstNodeInsideSelection = unit.getNodeSelector(null)
					.findEnclosingNode(selection.getOffset(),
							selection.getLength());
			result = findDeclaratorInAncestors(firstNodeInsideSelection);
		}
		return result;
	}

	private IASTFunctionDeclarator findDeclaratorInAncestors(IASTNode node) {
		while (node != null) {
			IASTFunctionDeclarator result = extractDeclarator(node);
			if (node instanceof ICPPASTTemplateDeclaration)
				result = extractDeclaratorFromTemplate((ICPPASTTemplateDeclaration) node);
			if (result != null)
				return result;
			node = node.getParent();
		}
		return null;
	}

	private IASTFunctionDeclarator extractDeclaratorFromTemplate(
			ICPPASTTemplateDeclaration node) {
		for (IASTNode childnode : node.getChildren()) {
			IASTFunctionDeclarator result = extractDeclarator(childnode);
			if (result != null)
				return result;
		}
		return null;
	}

	private IASTFunctionDeclarator extractDeclarator(IASTNode node) {
		if (node instanceof IASTFunctionDeclarator)
			return (IASTFunctionDeclarator) node;
		if (node instanceof IASTFunctionDefinition)
			return ((IASTFunctionDefinition) node).getDeclarator();
		if (node instanceof IASTSimpleDeclaration) {
			IASTDeclarator[] declarators = ((IASTSimpleDeclaration) node)
					.getDeclarators();
			if (declarators.length == 1
					&& declarators[0] instanceof IASTFunctionDeclarator)
				return (IASTFunctionDeclarator) declarators[0];
		}
		return null;
	}

	private boolean isPartOfAStatement(IASTNode node) {
		while (node != null) {
			if (node instanceof IASTStatement)
				return true;
			node = node.getParent();
		}
		return false;
	}
}
