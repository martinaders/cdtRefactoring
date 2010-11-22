package ch.hsr.eclipse.cdt.ui.toggle;

import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.ui.refactoring.Container;

@SuppressWarnings("restriction")
public class InsertionPointFinder {

	private ArrayList<ICPPASTFunctionDeclarator> allafterdeclarations;
	private ArrayList<ICPPASTFunctionDefinition> alldefinitionsoutside;
	private ICPPASTFunctionDefinition position;
	
	public InsertionPointFinder(IASTTranslationUnit classunit, IASTTranslationUnit functiondefunit, IASTFunctionDeclarator funcdecl) {
		findAllDeclarationsAfterInClass(classunit, funcdecl);
		findAllDefinitionsoutSideClass(functiondefunit);
		findRightPlace();
	}
	
	public IASTDeclaration getPosition() {
		if (position.getParent() != null && position.getParent() instanceof ICPPASTTemplateDeclaration)
			return (ICPPASTTemplateDeclaration) position.getParent();
		return position;
	}
	
	private void findRightPlace() {
		for(ICPPASTFunctionDeclarator decl: allafterdeclarations) {
			String decl_name = decl.getName().toString();
			for(ICPPASTFunctionDefinition def: alldefinitionsoutside) {
				String def_name = null;
				if (def.getDeclarator().getName() instanceof ICPPASTQualifiedName) {
					ICPPASTQualifiedName qname = (ICPPASTQualifiedName) def.getDeclarator().getName();
					def_name = qname.getNames()[1].toString(); 
				}
				else if (def.getDeclarator().getName() instanceof CPPASTName) {
					def_name = def.getDeclarator().getName().toString();
				}

				if (decl_name.equals(def_name)) {
					position = def;
					return;
				}
			}
		}
	}

	private void findAllDeclarationsAfterInClass(IASTTranslationUnit classunit, IASTFunctionDeclarator funcdecl) {
		ICPPASTCompositeTypeSpecifier klass = getklass(classunit);
		allafterdeclarations = getDeclarationsInClass(klass, funcdecl);
	}
	
	/**
	 * @param unit, the translation unit where to find the definitions
	 */
	private void findAllDefinitionsoutSideClass(IASTTranslationUnit unit) {
		final ArrayList<ICPPASTFunctionDefinition> definitions = new ArrayList<ICPPASTFunctionDefinition>();
		unit.accept(
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

	private ArrayList<ICPPASTFunctionDeclarator> getDeclarationsInClass(ICPPASTCompositeTypeSpecifier klass, final IASTFunctionDeclarator selected) {
		final ArrayList<ICPPASTFunctionDeclarator> declarations = new ArrayList<ICPPASTFunctionDeclarator>();
		
		klass.accept(
				new CPPASTVisitor() {
				{
					shouldVisitDeclarators = true;
				}
	
				boolean got = false;
				@Override
				public int visit(IASTDeclarator declarator) {
					if (declarator instanceof ICPPASTFunctionDeclarator) {
						if (((ICPPASTFunctionDeclarator) declarator) == selected) {
							got = true;
						}
						if (got) {
							declarations.add((ICPPASTFunctionDeclarator) declarator);
						}
					}
					return super.visit(declarator);
				}
		});
		
		return declarations;
	}

	private ICPPASTCompositeTypeSpecifier getklass(IASTTranslationUnit unit) {
		final Container<ICPPASTCompositeTypeSpecifier> result = new Container<ICPPASTCompositeTypeSpecifier>();

		unit.accept(
			new CPPASTVisitor() {
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
}