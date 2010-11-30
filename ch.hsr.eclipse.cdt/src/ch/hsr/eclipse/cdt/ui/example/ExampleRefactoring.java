package ch.hsr.eclipse.cdt.ui.example;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ExampleRefactoring extends CRefactoring {

	public ExampleRefactoring(IFile file, ISelection selection,
			ICElement element, ICProject proj) {
		super(file, selection, element, proj);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException,
			OperationCanceledException {
		ASTRewrite rewriter = collector.rewriterForTranslationUnit(unit);
		TextEditGroup editGroup = new TextEditGroup("");
		IASTNode remove = unit.getDeclarations()[1];
		rewriter.remove(remove, editGroup);
		IASTNode replace = unit.getDeclarations()[0];
//		rewriter.replace(replace , editGroup);
	}
}
