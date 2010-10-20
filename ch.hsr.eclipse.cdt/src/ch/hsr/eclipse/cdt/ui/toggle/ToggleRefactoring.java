package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionWithTryBlock;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.ASTHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleRefactoring extends CRefactoring {
	
	private IASTFunctionDefinition selectedDefinition;
	private CPPASTFunctionDeclarator selectedDeclaration;
	private TextSelection selection;
	private ASTRewrite rewriter;
	private TextEditGroup infoText;

	public ToggleRefactoring(IFile file, ISelection selection, ICProject proj) {
		super(file, selection, null, proj);
		this.selection = (TextSelection) selection;
		infoText = new TextEditGroup("Toggle function body placement");
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		super.checkInitialConditions(pm);
		if (initStatus.hasFatalError()) {
			return initStatus;
		}
		selectedDeclaration = ToggleSelectionHelper.getSelectedDeclaration(unit, selection);
		selectedDefinition  = ToggleSelectionHelper.getSelectedDefinition(unit, selection);

		if (selectedDeclaration == null || selectedDefinition == null) {
			initStatus.addFatalError("declaration AND definition needed. Cannot toggle. Stopping.");
		}
		return initStatus;
	}
	
	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return new EmptyRefactoringDescription();
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector modifications) throws CoreException {
		rewriter = modifications.rewriterForTranslationUnit(unit);
		try {
			lockIndex();
			try {
				collectModificationsWithCaseDistinguish();
			} finally {
				unlockIndex();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void collectModificationsWithCaseDistinguish() {
		if (isInClassSituation())
			handleInClassSituation();
		else
			handleInHeaderSituation();
	}

	private boolean isInClassSituation() {
		return selectedDefinition.getDeclarator() == selectedDeclaration;
	}
	
	private void handleInClassSituation() {
		System.out.println("We're in the in-class situation.");
		IASTSimpleDeclaration declaration = createDeclarationFromDefinition(selectedDefinition);
		rewriter.replace(selectedDefinition, declaration, infoText);
		rewriter.insertBefore(unit, null, getInHeaderDefinition(), infoText);
	}
	
	private IASTSimpleDeclaration createDeclarationFromDefinition(IASTFunctionDefinition memberdefinition) {
		IASTDeclarator declarator = memberdefinition.getDeclarator().copy();
		IASTDeclSpecifier specifier = memberdefinition.getDeclSpecifier().copy();
		IASTSimpleDeclaration result = new CPPASTSimpleDeclaration(specifier);
		result.addDeclarator(declarator);
		return result;
	}

	private void handleInHeaderSituation() {
		System.out.println("We're in the in-header situation.");
		rewriter.remove(getToBeRemovedDefinition(), infoText);
		rewriter.replace(selectedDeclaration.getParent(), getInClassDefinition(), infoText);
	}

	private IASTNode getToBeRemovedDefinition() {
		if (selectedDefinition.getParent() != null && selectedDefinition.getParent() instanceof ICPPASTTemplateDeclaration)
			return selectedDefinition.getParent();
		return selectedDefinition;
	}

	private IASTFunctionDefinition getInClassDefinition() {
		IASTDeclSpecifier newDeclSpec = selectedDefinition.getDeclSpecifier().copy();
		newDeclSpec.setInline(false);
		IASTFunctionDeclarator newDeclaration = selectedDeclaration.copy();
		IASTStatement newBody = selectedDefinition.getBody().copy();
		
		//totaly duplicated code
		IASTFunctionDefinition newfunc;
		if (selectedDefinition instanceof ICPPASTFunctionWithTryBlock) {
			newfunc = getCatchHandlers(newDeclSpec, newDeclaration, newBody);	
		} else {
			newfunc = new CPPASTFunctionDefinition(newDeclSpec, newDeclaration, newBody);
		}
		newfunc.setParent(getParentInsertionPoint(selectedDeclaration, unit));
		return newfunc;
	}

	private IASTNode getParentInsertionPoint(IASTNode node, IASTTranslationUnit alternative) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				return (ICPPASTCompositeTypeSpecifier) node;
			}
		}
		return unit;
	}

	private IASTNode getInHeaderDefinition() {
		IASTDeclSpecifier newdeclspec = selectedDefinition.getDeclSpecifier().copy();
		newdeclspec.setInline(true);
		IASTFunctionDeclarator funcdecl = selectedDeclaration.copy();
		funcdecl.setName(ToggleSelectionHelper.getQualifiedName(selectedDefinition));
		removeParameterInitializations(funcdecl);
		IASTStatement newbody = selectedDefinition.getBody().copy();
		
		IASTFunctionDefinition newfunc = null;
		if (selectedDefinition instanceof ICPPASTFunctionWithTryBlock) {
			newfunc = getCatchHandlers(newdeclspec, funcdecl, newbody);			
		} else {
			newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl, newbody);		
		}
		
		IASTNode node = getTemplateDeclaration();
		if (node != null && node instanceof ICPPASTTemplateDeclaration) {
			ICPPASTTemplateDeclaration templdecl = (ICPPASTTemplateDeclaration) node; 
			templdecl.setParent(unit);
			templdecl.setDeclaration(newfunc);
			return templdecl;
		} else {
			newfunc.setParent(unit);
			return newfunc;
		}
	}

	private IASTFunctionDefinition getCatchHandlers(
			IASTDeclSpecifier newdeclspec, IASTFunctionDeclarator funcdecl,
			IASTStatement newbody) {
		IASTFunctionDefinition newfunc;
		newfunc = new CPPASTFunctionWithTryBlock(newdeclspec, funcdecl, newbody);
		
		ICPPASTFunctionWithTryBlock oldfunc = (ICPPASTFunctionWithTryBlock) selectedDefinition;
		for(ICPPASTCatchHandler chandler: oldfunc.getCatchHandlers()) {
			((CPPASTFunctionWithTryBlock) newfunc).addCatchHandler(chandler.copy());
		}
		return newfunc;
	}

	private void removeParameterInitializations(IASTFunctionDeclarator funcdecl) {
		for (IASTNode child : funcdecl.getChildren()) {
			if (child instanceof ICPPASTParameterDeclaration) {
				ICPPASTParameterDeclaration parameter = (ICPPASTParameterDeclaration) child;
				parameter.getDeclarator().setInitializer(null);
			}
		}
	}

	private IASTNode getTemplateDeclaration() {
		IASTNode node = selectedDeclaration;
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTTemplateDeclaration)
				break;
		}
		return node.copy();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
	throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return super.checkFinalConditions(pm);
	}
}
