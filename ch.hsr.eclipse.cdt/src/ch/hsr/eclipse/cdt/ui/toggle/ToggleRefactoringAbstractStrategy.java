package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionWithTryBlock;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.cdt.internal.ui.refactoring.CCompositeChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public abstract class ToggleRefactoringAbstractStrategy {

	protected IASTFunctionDeclarator selectedDeclaration;
	protected IASTFunctionDefinition selectedDefinition;
	protected IASTTranslationUnit definition_unit;
	protected TextEditGroup infoText;

	public ToggleRefactoringAbstractStrategy(
			IASTFunctionDeclarator selectedDeclaration2,
			IASTFunctionDefinition selectedDefinition, IASTTranslationUnit unit) {
		this.selectedDeclaration = selectedDeclaration2;
		this.selectedDefinition = selectedDefinition;
		this.definition_unit = unit;
		infoText = new TextEditGroup("Toggle function body placement");
	}

	public abstract void run(ModificationCollector modifications);

	protected IASTSimpleDeclaration createDeclarationFromDefinition(
			IASTFunctionDefinition memberdefinition) {
		IASTDeclarator declarator = memberdefinition.getDeclarator().copy();
		IASTDeclSpecifier specifier = memberdefinition.getDeclSpecifier()
				.copy();
		IASTSimpleDeclaration result = new CPPASTSimpleDeclaration(specifier);
		result.addDeclarator(declarator);
		return result;
	}

	protected IASTNode getQualifiedNameDefinition(boolean inline) {
		IASTDeclSpecifier newdeclspec = selectedDefinition.getDeclSpecifier()
				.copy();
		newdeclspec.setInline(inline);
		IASTFunctionDeclarator funcdecl = selectedDeclaration.copy();

		funcdecl.setName(ToggleSelectionHelper
				.getQualifiedName(selectedDefinition));
		ToggleNodeHelper.removeParameterInitializations(funcdecl);

		ICPPASTFunctionDefinition newfunc = assembleFunctionDefinitionWithBody(
				newdeclspec, funcdecl);

		ICPPASTTemplateDeclaration templdecl = ToggleNodeHelper
				.getTemplateDeclaration(selectedDeclaration);
		if (templdecl != null) {
			templdecl.setDeclaration(newfunc);
			templdecl.setParent(definition_unit);
			return templdecl;
		} else {
			newfunc.setParent(definition_unit);
			return newfunc;
		}
	}

	private ICPPASTFunctionDefinition assembleFunctionDefinitionWithBody(
			IASTDeclSpecifier newdeclspec, IASTFunctionDeclarator funcdecl) {
		IASTStatement newbody = selectedDefinition.getBody().copy();
		ICPPASTFunctionDefinition newfunc = null;
		if (ToggleNodeHelper.hasCatchHandlers(selectedDefinition)) {
			newfunc = new CPPASTFunctionWithTryBlock(newdeclspec, funcdecl,
					newbody);
			ToggleNodeHelper.copyCatchHandlers(selectedDefinition, newfunc);
		} else {
			newfunc = new CPPASTFunctionDefinition(newdeclspec, funcdecl,
					newbody);
		}

		if (ToggleNodeHelper.hasInitializerList(selectedDefinition)) {
			ToggleNodeHelper.copyInitializerList(newfunc, selectedDefinition);
		}
		return newfunc;
	}

	protected IASTFunctionDefinition getInClassDefinition(
			IASTFunctionDefinition definition,
			IASTFunctionDeclarator declaration, IASTTranslationUnit unit) {
		IASTDeclSpecifier newDeclSpec = definition.getDeclSpecifier().copy();
		newDeclSpec.setInline(false);
		IASTFunctionDeclarator newDeclaration = declaration.copy();

		ICPPASTFunctionDefinition newfunc = assembleFunctionDefinitionWithBody(
				newDeclSpec, newDeclaration);

		newfunc.setParent(getParentInsertionPoint(definition, unit));
		return newfunc;
	}

	private IASTNode getParentInsertionPoint(IASTNode node,
			IASTTranslationUnit alternative) {
		while (node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				return (ICPPASTCompositeTypeSpecifier) node;
			}
		}
		return definition_unit;
	}

	/**
	 * Removes annoying newlines after changes have been generated. Don't remove
	 * anything by default.
	 */
	protected void removeTrailingNewlines(CCompositeChange finalChange) {
	}
}
