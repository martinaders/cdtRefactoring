package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.ui.refactoring.Container;

public class InsertionPointFinder {

	private ToggleRefactoringContext context;
	private ArrayList<ICPPASTFunctionDeclarator> allafterdeclarations;
	private ArrayList<ICPPASTFunctionDefinition> alldefinitionsoutside;
	private ICPPASTFunctionDefinition position;
	

	public InsertionPointFinder(ToggleRefactoringContext context) {
		this.context = context;
		findAllDeclarationsAfterInClass();
		findAllDefinitionsoutSideClass();
		findRightPlace();
	}
	
	private void findRightPlace() {
		for(ICPPASTFunctionDeclarator decl: allafterdeclarations) {
			for(ICPPASTFunctionDefinition def: alldefinitionsoutside) {
				ICPPASTQualifiedName qname = (ICPPASTQualifiedName) def.getDeclarator().getName();
				if (decl.getName().toString().equals(qname.getNames()[1].toString())) {
						position = def;
						return;
				}
			}
		}
	}

	private void findAllDefinitionsoutSideClass() {
		final ArrayList<ICPPASTFunctionDefinition> definitions = new ArrayList<ICPPASTFunctionDefinition>();
		context.getDefinitionUnit().accept(
			new CPPASTVisitor() {
				{
					shouldVisitDeclarations = true;
				}
	
				@Override
				public int visit(IASTDeclaration declaration) {
					if (declaration instanceof ICPPASTFunctionDefinition) {
						if (declaration.getParent() != null && declaration.getParent() instanceof ICPPASTCompositeTypeSpecifier)
							return PROCESS_CONTINUE;
						definitions.add((ICPPASTFunctionDefinition) declaration);
					}
					return super.visit(declaration);
				}
			});
		alldefinitionsoutside = definitions;
	}

	private void findAllDeclarationsAfterInClass() {
		ICPPASTCompositeTypeSpecifier klass = getklass();
		allafterdeclarations = getDeclarationsInClass(klass);
	}

	private ArrayList<ICPPASTFunctionDeclarator> getDeclarationsInClass(
			ICPPASTCompositeTypeSpecifier klass) {
		final ICPPASTFunctionDeclarator selected = (ICPPASTFunctionDeclarator) context.getDefinition().getDeclarator();
		final ArrayList<ICPPASTFunctionDeclarator> declarations = new ArrayList<ICPPASTFunctionDeclarator>();
		klass.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}

			boolean got = false;
			@Override
			public int visit(IASTDeclarator declarator) {
				if (declarator instanceof ICPPASTFunctionDeclarator) {
					if (((ICPPASTFunctionDeclarator) declarator) == selected) {
						System.out.println("catch");
						got = true;
					}
					if (got) {
						System.out.println("declarator: " + declarator.getParent().getRawSignature());
						declarations.add((ICPPASTFunctionDeclarator) declarator);
					}
				}
				return super.visit(declarator);
			}
		});
		return declarations;
	}

	private ICPPASTCompositeTypeSpecifier getklass() {
		final Container<ICPPASTCompositeTypeSpecifier> result = new Container<ICPPASTCompositeTypeSpecifier>();

		context.getDefinitionUnit().accept(new CPPASTVisitor() {
			{
				shouldVisitDeclSpecifiers = true;
			}

			@Override
			public int visit(IASTDeclSpecifier declSpec) {
				if (declSpec instanceof ICPPASTCompositeTypeSpecifier) {
					result.setObject((ICPPASTCompositeTypeSpecifier) declSpec);
					return PROCESS_ABORT;
				}
				return super.visit(declSpec);
			}
		});
		return result.getObject();
	}

	public IASTNode getInsertionPoint() {
		return new CPPASTName();
	}

	public ICPPASTFunctionDefinition getPosition() {
		return position;
	}
}
