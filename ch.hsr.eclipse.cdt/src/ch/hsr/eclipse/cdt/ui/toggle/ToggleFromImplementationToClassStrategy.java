package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToClassStrategy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit declaration_unit;

	public ToggleFromImplementationToClassStrategy(
			IASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition,
			IASTTranslationUnit definition_unit, IASTTranslationUnit declaration_unit) {
		super(selectedDeclaration, selectedDefinition, definition_unit);
		this.declaration_unit = declaration_unit;
	}

	public ToggleFromImplementationToClassStrategy(
			ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
		this.declaration_unit = context.getDeclarationUnit();
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite implast = modifications
				.rewriterForTranslationUnit(definition_unit);
		ASTRewrite headerast = modifications
				.rewriterForTranslationUnit(declaration_unit);

		implast.remove(selectedDefinition, infoText);
		IASTFunctionDefinition function = new CPPASTFunctionDefinition();
		function.setBody(selectedDefinition.getBody().copy());
		
		IASTFunctionDeclarator declarator = new CPPASTFunctionDeclarator(new CPPASTName(selectedDeclaration.getName().copy().toCharArray()));
		declarator.setParent(function);
		function.setDeclarator(declarator);
		function.setDeclSpecifier(selectedDefinition.getDeclSpecifier().copy());
		function.setParent(selectedDeclaration.getParent().getParent());
		
		headerast.insertBefore(selectedDeclaration.getParent().getParent(), null, function, infoText);
		headerast.remove(selectedDeclaration.getParent(), infoText);
	}

}
