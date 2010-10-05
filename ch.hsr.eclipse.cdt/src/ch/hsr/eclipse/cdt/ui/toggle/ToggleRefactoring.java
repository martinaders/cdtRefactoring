package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescription;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditVisitor;
import org.eclipse.ui.internal.UIPlugin;
import org.eclipse.cdt.ui.CUIPlugin;

import ch.hsr.eclipse.cdt.ui.NullRefactoring;

public class ToggleRefactoring extends CRefactoring {

	private class MemberVisitor extends ASTVisitor {
		{
			shouldVisitNames = true;
		}
		public IASTName member = null;

		@Override
		public int visit(IASTName name) {
			System.out.println("Visit a name: " + name.getRawSignature());
			if (name.getRawSignature().equals("member"))
				member = name;
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

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException,
			OperationCanceledException {
		MemberVisitor visitor = new MemberVisitor();
		unit.accept(visitor);
		if (visitor.member == null)
			return;
		
		TextEditGroup infoText = new TextEditGroup("Remove " + visitor.member.getRawSignature());
		ASTRewrite rewriter = collector.rewriterForTranslationUnit(unit);
		rewriter.remove(visitor.member, infoText);
	}

}
