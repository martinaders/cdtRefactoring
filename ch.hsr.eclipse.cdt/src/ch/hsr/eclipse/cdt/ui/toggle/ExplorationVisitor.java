package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;

final class ExplorationVisitor extends ASTVisitor {

	ExplorationVisitor(boolean visitNodes) {
		super(visitNodes);
	}
	
	public int visit(IASTNode node) {
		return 0;
		
	}

	public int visit(IASTTranslationUnit tu) {
		System.out.println("*** " + tu.getClass() + " : \n" + tu.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTName name) {
		System.out.println("*** " + name.getClass() + " : \n" + name.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTDeclaration declaration) {
		System.out.println("*** " + declaration.getClass() + " : \n" + declaration.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTInitializer initializer) {
		System.out.println("*** " + initializer.getClass() + " : \n" + initializer.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTParameterDeclaration parameterDeclaration) {
		System.out.println("*** " + parameterDeclaration.getClass() + " : \n" + parameterDeclaration.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTDeclarator declarator) {
		System.out.println("*** " + declarator.getClass() + " : \n" + declarator.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTDeclSpecifier declSpec) {
		System.out.println("*** " + declSpec.getClass() + " : \n" + declSpec.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTArrayModifier arrayModifier) {
		System.out.println("*** " + arrayModifier.getClass() + " : \n" + arrayModifier.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTPointerOperator ptrOperator) {
		System.out.println("*** " + ptrOperator.getClass() + " : \n" + ptrOperator.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTExpression expression) {
		System.out.println("*** " + expression.getClass() + " : \n" + expression.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTStatement statement) {
		System.out.println("*** " + statement.getClass() + " : \n" + statement.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTTypeId typeId) {
		System.out.println("*** " + typeId.getClass() + " : \n" + typeId.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTEnumerator enumerator) {
		System.out.println("*** " + enumerator.getClass() + " : \n" + enumerator.getRawSignature());
		return PROCESS_CONTINUE;
	}

	public int visit(IASTProblem problem) {
		System.out.println("*** " + problem.getClass() + " : \n" + problem.getRawSignature());
		return PROCESS_CONTINUE;
	}
}