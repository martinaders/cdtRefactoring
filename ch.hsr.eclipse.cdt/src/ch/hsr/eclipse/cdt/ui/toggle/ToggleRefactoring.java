package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.core.internal.resources.File;
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

//		determinePosition();
//		IASTFunctionDefinition newfunc = getReplacementFunction();
//		addFunctionReplaceModification(collector, newfunc);
	}

	private void determinePosition() {
		if (selectedDeclaration == null) {
			System.out.println("No function declaration selected. Cannot toggle. Stopping.");
			return;
		}
		System.out.println("The declarator found: " + selectedDefinition.getRawSignature());
		System.out.println("The declaration found: " + selectedDeclaration.getRawSignature());
		if (selectedDefinition.getDeclarator().equals(selectedDeclaration)) {
			System.out.println("The declarator is the same node as de definition");
		}
		if (isSelectionInHeaderFile())
			System.out.println("We're in a header file.");
		else
			System.out.println("We're in a source file.");
		
		if (selectedDefinition == null && selectedDeclaration != null) // 2nd part is obsolete
			System.out.println("We're in a pure deCLARAtion.");
		else
			System.out.println("We're in a deFINItion so we can access its body."); // stimmt nicht weils die klassendefinition findet

		if (selectedDefinition.getTranslationUnit().equals(selectedDeclaration.getTranslationUnit()))
			System.out.println("Declaration and Definition are placed into the same files.");
		
//		System.out.println("Declaration and Definition are placed in different files.");
//		System.out.println("Declaration and Definition are both placed in the header file.");
//		 System.out.println("and they're together.");
//		 System.out.println("and they're placed apart.");

//		System.out.println("The specifiers for this declaration: " + selectedDeclaration.getDeclSpecifier());
//		System.out.println("We're inside a class and the current scope is xy");
//		System.out.println("We're outside of any class. The function has scope xy. So look there for existing declarations.");
//		
//		System.out.println("We've selected a free function.");
//		 System.out.println("The free function shall be toggled to a separate file.");
//		 System.out.println("The free function shall be toggled back into the source file.");
	}

	private boolean isSelectionInHeaderFile() {
		String filename = unit.getContainingFilename();
		return filename.endsWith(".h") || filename.endsWith(".hpp");
	}

	private IASTFunctionDefinition getReplacementFunction() {
		IASTDeclSpecifier newdeclspec = selectedDefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = selectedDeclaration;

		//TODO: add the parameters
		
		IASTStatement newbody = selectedDefinition.getBody().copy();
		IASTFunctionDefinition newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl, newbody);
		newfunc.setParent(unit);
		return newfunc;
	}

	private void addFunctionReplaceModification(
			ModificationCollector collector, IASTFunctionDefinition newfunc) {
		ASTRewrite rewrite = collector.rewriterForTranslationUnit(unit);
		TextEditGroup edit = new TextEditGroup("Toggle");
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);
		rewrite.replace(selectedDefinition, declaration, edit);
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
