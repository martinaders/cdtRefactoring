package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

import ch.hsr.eclipse.cdt.ui.NullRefactoring;

public class ToggleRefactoring extends CRefactoring {

	private class MemberVisitor extends ASTVisitor {
		{
			shouldVisitNames = true;
		}
		public IASTName memberName = null;

		@Override
		public int visit(IASTName name) {
			if (name.getRawSignature().equals("member"))
				memberName = name;
			return PROCESS_CONTINUE;
		}
	}

	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new CRefactoringDescription("id", "proj", "desc", "comment", 0, new HashMap<String, String>()) {
			@Override
			public Refactoring createRefactoring(RefactoringStatus status)
					throws CoreException {
				return new NullRefactoring(getFile(), getSelection(), getCProject());
			}
		};
	}

	// TODO: use lockIndex, as seen in HideMethodRefactoring
	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException,
			OperationCanceledException {
		TextEditGroup infoText = new TextEditGroup("Remove member");
		ASTRewrite rewriter = collector.rewriterForTranslationUnit(unit);
		rewriter.remove(getMemberDeclaration(), infoText);
	}

	private IASTNode getMemberDeclaration() {
		MemberVisitor visitor = new MemberVisitor();
		unit.accept(visitor);
		if (isDeclarationFound(visitor))
			return null;
		return visitor.memberName.getParent().getParent();
	}

	private boolean isDeclarationFound(MemberVisitor visitor) {
		return visitor.memberName == null || visitor.memberName.getParent() == null || !(visitor.memberName.getParent().getParent() instanceof CPPASTSimpleDeclaration);
	}

}
