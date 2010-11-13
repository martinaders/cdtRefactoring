package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.jface.text.TextSelection;

/**
 * Given a selection and a translation unit, this class finds a
 * ICPPASTFunctionDeclarator if possible. Nested local functions are skipped
 * during search.
 */
public class DeclaratorFinder {

	private IASTFunctionDeclarator foundDeclarator;

	public DeclaratorFinder(TextSelection selection, IASTTranslationUnit unit)
			throws NotSupportedException {
		foundDeclarator = findDeclaratorInSelection(selection, unit);

		if (foundDeclarator == null)
			throw new NotSupportedException("cannot work without declarator");

		if (isPartOfAStatement(foundDeclarator))
			throw new NotSupportedException(
					"Nested function declarations not supported");
	}

	public IASTName getName() {
		return foundDeclarator.getName();
	}

	private IASTFunctionDeclarator findDeclaratorInSelection(
			TextSelection selection, IASTTranslationUnit unit) {
		IASTNode firstNodeInsideSelection = unit.getNodeSelector(null)
				.findFirstContainedNode(selection.getOffset(),
						selection.getLength());
		IASTFunctionDeclarator result = findDeclaratorInAncestors(firstNodeInsideSelection);

		if (result == null) {
			firstNodeInsideSelection = unit.getNodeSelector(null)
					.findEnclosingName(selection.getOffset(),
							selection.getLength());
			result = findDeclaratorInAncestors(firstNodeInsideSelection);
		}
		return result;
	}

	private IASTFunctionDeclarator findDeclaratorInAncestors(IASTNode node) {
		while (node != null) {
			IASTFunctionDeclarator result = extractDeclarator(node);
			if (node instanceof ICPPASTTemplateDeclaration)
				result = extractDeclaratorFromTemplate((ICPPASTTemplateDeclaration) node);
			if (result != null)
				return result;
			node = node.getParent();
		}
		return null;
	}

	private IASTFunctionDeclarator extractDeclaratorFromTemplate(
			ICPPASTTemplateDeclaration node) {
		IASTFunctionDeclarator result;
		for (IASTNode n : node.getChildren()) {
			result = extractDeclarator(n);
			if (result != null)
				return result;
		}
		return null;
	}

	private IASTFunctionDeclarator extractDeclarator(IASTNode node) {
		if (node instanceof IASTFunctionDeclarator)
			return (IASTFunctionDeclarator) node;
		if (node instanceof IASTFunctionDefinition)
			return ((IASTFunctionDefinition) node).getDeclarator();
		if (node instanceof IASTSimpleDeclaration) {
			IASTDeclarator[] declarators = ((IASTSimpleDeclaration) node)
					.getDeclarators();
			if (declarators.length == 1
					&& declarators[0] instanceof IASTFunctionDeclarator)
				return (IASTFunctionDeclarator) node;
		}
		return null;
	}

	private boolean isPartOfAStatement(IASTNode node) {
		while (node != null) {
			if (node instanceof IASTStatement)
				return true;
			node = node.getParent();
		}
		return false;
	}
}
