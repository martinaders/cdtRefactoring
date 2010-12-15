/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 * 		Martin Schwab & Thomas Kallenberg - initial API and implementation 
 ******************************************************************************/
package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
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
	private IToggleRefactoringStrategy strategy;
	private ToggleRefactoringContext context;
	private IIndex fIndex;
	
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
			pm.subTask("waiting for indexer");
			prepareIndexer(pm);
			pm.subTask("analyzing user text selection");
			context = new ToggleRefactoringContext(fIndex, file, selection);
			strategy = new ToggleStrategyFactory(context).getAppropriateStategy();
		} catch (InterruptedException e) {
		} catch (NotSupportedException e) {
			System.err.println("not implemented: " + e.getMessage());
			initStatus.addFatalError("fatal");
		} finally {
			fIndex.releaseReadLock();
		}

		return initStatus;
	}

	private void prepareIndexer(IProgressMonitor pm) throws CoreException, InterruptedException  {
		IIndexManager im = CCorePlugin.getIndexManager();
		while (!im.isProjectIndexed(project)) {
			im.joinIndexer(500, pm);
			if (pm.isCanceled())
				throw new NotSupportedException("not able to work without the indexer");
		}
		if (!im.isProjectIndexed(project))
			throw new NotSupportedException("not able to work without the indexer");
		IndexerPreferences.set(project.getProject(), IndexerPreferences.KEY_INDEX_UNUSED_HEADERS_WITH_DEFAULT_LANG, Boolean.TRUE.toString());
		fIndex = CCorePlugin.getIndexManager().getIndex(project);
		fIndex.acquireReadLock();
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector modifications) throws CoreException {
		pm.subTask("calculating required code modifications");
		strategy.run(modifications);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new EmptyRefactoringDescription();
	}
}
