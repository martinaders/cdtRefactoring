package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {

	private IASTFunctionDefinition selectedMemberDefinition;

	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new EmptyRefactoringDescription();
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException {
		try {
			lockIndex();
			try {
				collectMoveChanges(collector);
			} finally {
				unlockIndex();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void collectMoveChanges(ModificationCollector collector) {
		selectedMemberDefinition = ToggleSelectionHelper.getFirstSelectedFunctionDefinition(region, unit);
		IASTFunctionDefinition newfunc = getReplacementFunction();
		addFunctionReplaceModification(collector, newfunc);
	}

	private IASTFunctionDefinition getReplacementFunction() {
		IASTDeclSpecifier newdeclspec = selectedMemberDefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = new CPPASTFunctionDeclarator(ToggleSelectionHelper.getQualifiedName(selectedMemberDefinition));

		//TODO: add the parameters
		
		IASTStatement newbody = selectedMemberDefinition.getBody().copy();
		IASTFunctionDefinition newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl, newbody);
		newfunc.setParent(unit);
		return newfunc;
	}

	private void addFunctionReplaceModification(
			ModificationCollector collector, IASTFunctionDefinition newfunc) {
		ASTRewrite rewrite = collector.rewriterForTranslationUnit(unit);
		TextEditGroup edit = new TextEditGroup("Toggle");
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedMemberDefinition);
		rewrite.replace(selectedMemberDefinition, declaration, edit);
		rewrite.insertBefore(unit, null, newfunc, edit);
	}

	private IASTSimpleDeclaration createDeclarationFromDefinition(
			IASTFunctionDefinition memberdefinition) {
		IASTDeclarator declarator = memberdefinition.getDeclarator().copy();
		IASTDeclSpecifier specifier = memberdefinition.getDeclSpecifier().copy();
		IASTSimpleDeclaration result = new CPPASTSimpleDeclaration(specifier);
		result.addDeclarator(declarator);
		return result;
	}

}
