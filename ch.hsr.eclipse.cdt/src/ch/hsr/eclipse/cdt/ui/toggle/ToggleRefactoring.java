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
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {

	private IASTFunctionDefinition selectedDefinition;
	private CPPASTFunctionDeclarator selectedDeclaration;
	private final TextSelection selection;

	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
		this.selection = (TextSelection) selection;
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
		selectedDeclaration = ToggleSelectionHelper.getSelectedDeclaration( unit, selection);
		selectedDefinition  = ToggleSelectionHelper.getSelectedDefinition(unit, selection);
		determinePosition();
		IASTFunctionDefinition newfunc = getReplacementDefinition();
		addFunctionReplaceModification(collector, newfunc);
	}

	private void determinePosition() {
		if (selectedDeclaration == null || selectedDefinition == null) {
			System.out.println("declaration AND definition needed. Cannot toggle. Stopping.");
			return;
		}
	}

	private IASTFunctionDefinition getReplacementDefinition() {
		IASTDeclSpecifier newdeclspec = selectedDefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = selectedDeclaration.copy();

		//TODO: add the parameters
		
		IASTStatement newbody = selectedDefinition.getBody().copy();
		IASTFunctionDefinition newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl, newbody);
		newfunc.setParent(unit);
		return newfunc;
	}

	private void addFunctionReplaceModification(
			ModificationCollector collector, IASTFunctionDefinition definition) {
		ASTRewrite rewrite = collector.rewriterForTranslationUnit(unit);
		TextEditGroup infoText = new TextEditGroup("Toggle");
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);
		rewrite.replace(selectedDefinition, declaration, infoText);
		rewrite.insertBefore(unit, null, definition, infoText);
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
