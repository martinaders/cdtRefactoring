package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.index.IndexLocationFactory;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;

/**
 * Helps finding a FunctionDefinition in the parent nodes of the current selection. 
 */
@SuppressWarnings("restriction")
class ToggleSelectionHelper extends SelectionHelper {		
	
	public static CPPASTFunctionDeclarator getSelectedDeclaration(IASTTranslationUnit unit, TextSelection selection) {
		CPPASTFunctionDeclarator selectedDeclaration = getSelectedDeclarator(unit, selection);
		if (selectedDeclaration == null) {
			return null;
		}
		PlainDeclarationFinder visitor = new PlainDeclarationFinder(selectedDeclaration);
		unit.accept(visitor);
		return visitor.result;
	}

	private static CPPASTFunctionDeclarator getSelectedDeclarator(IASTTranslationUnit unit, TextSelection selection) {
		SelectedDeclaratorFinder visitor = new SelectedDeclaratorFinder(selection);
		unit.accept(visitor);
		return visitor.result;
	}

	public static IASTFunctionDefinition getSelectedDefinition(IASTTranslationUnit unit, TextSelection selection) {
		final CPPASTFunctionDeclarator selectedDeclaration = getSelectedDeclarator(unit, selection);
		if (selectedDeclaration == null) {
			return null;
		}
		final Container<IASTFunctionDefinition> result = new Container<IASTFunctionDefinition>();
		
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarations = true;
			}
			public int visit(IASTDeclaration node) {
				if (!(node instanceof IASTFunctionDefinition))
					return super.visit(node);
				IASTFunctionDefinition func = (IASTFunctionDefinition) node;

				if (!(func.getDeclarator() instanceof CPPASTFunctionDeclarator))
					return super.visit(node);
				CPPASTFunctionDeclarator declarator = (CPPASTFunctionDeclarator) func.getDeclarator();
				if (declarator.getFunctionScope().equals(selectedDeclaration.getFunctionScope())) {
					result.setObject((IASTFunctionDefinition) func);
				}
				return super.visit(node);
			}
		});
		return result.getObject();
	}

	public static ArrayList<IASTName> getAllQualifiedNames(IASTNode node) {
		ArrayList<IASTName> names = new ArrayList<IASTName>();
		
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				names.add(((ICPPASTCompositeTypeSpecifier) node).getName());
			}
			else if (node instanceof ICPPASTNamespaceDefinition) {
				names.add(((ICPPASTNamespaceDefinition) node).getName());
			}
			else if (node instanceof ICPPASTTemplateDeclaration) {
				for(IASTNode child : node.getChildren()) {
					if (child instanceof ICPPASTSimpleTypeTemplateParameter) {
						IASTName name = names.remove(names.size()-1);
						IASTName toadd = new CPPASTName((name + "<" + getTemplateParameterName(child) + ">").toCharArray());
						names.add(toadd);
					}
				}
			}
		}
		Collections.reverse(names);
		return names;
	}

	private static IASTName getTemplateParameterName(IASTNode child) {
		ICPPASTSimpleTypeTemplateParameter tempcild = (ICPPASTSimpleTypeTemplateParameter) child;
		IASTNamedTypeSpecifier t = new CPPASTNamedTypeSpecifier(tempcild.getName().copy());
		IASTName templname = t.getName();
		return templname;
	}

	public static ICPPASTQualifiedName getQualifiedName(IASTFunctionDefinition memberdefinition) {
		ICPPASTQualifiedName newdecl = new CPPASTQualifiedName();
		for (IASTName name : getAllQualifiedNames(memberdefinition)) {
			newdecl.addName(name.copy());
		}
		newdecl.addName(memberdefinition.getDeclarator().getName().copy());
		newdecl.setFullyQualified(true);
		return newdecl;
	}
	
	@Deprecated
	public static IASTFunctionDefinition getFirstSelectedFunctionDefinition(final Region region, final IASTTranslationUnit unit) {
		final Container<IASTFunctionDefinition> container = new Container<IASTFunctionDefinition>();
		
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitDeclarators = true;
			}
			public int visit(IASTDeclarator declarator) {
				if (declarator instanceof CPPASTFunctionDeclarator) {
					if ((declarator.getParent() instanceof ICPPASTFunctionDefinition) && isSelectionOnExpression(region, declarator)) {
						container.setObject((IASTFunctionDefinition) declarator.getParent());
					}
				}
				return super.visit(declarator);
			}
		});
		return container.getObject();
	}

	public static URI getSiblingFile( IFile file, ICProject project) throws CoreException {
		IIndex projectindex = CCorePlugin.getIndexManager().getIndex(project);
		IIndexFile thisfile = projectindex.getFile(ILinkage.CPP_LINKAGE_ID,
				IndexLocationFactory.getWorkspaceIFL(file));
		String filename = getFilenameWithoutExtension(file.getFullPath()
				.toString());
		if (file.getFileExtension().equals("h")) {
			for (IIndexInclude include : projectindex.findIncludedBy(thisfile)) {
				if (getFilenameWithoutExtension(
						include.getIncludedBy().getLocation().getFullPath())
						.equals(filename)) {
					return include.getIncludedBy().getLocation().getURI();
				}

			}
		} else if (file.getFileExtension().equals("cpp")
				|| file.getFileExtension().equals("c")) {
			for (IIndexInclude include : projectindex.findIncludes(thisfile)) {
				if (getFilenameWithoutExtension(include.getFullName()).equals(
						filename)) {
					return include.getIncludesLocation().getURI();
				}
			}
		}
		return null;
	}
	
	private static String getFilenameWithoutExtension(String filename) {
		filename = filename.replaceAll("\\.(.)*$", "");
		filename = filename.replaceAll("(.)*\\/", "");
		return filename;
	}

	public static IASTTranslationUnit getLocalTranslationUnitForFile(URI fileUri)
			throws CModelException, CoreException {
		ICProject cProject = CoreModel.getDefault()
				.create(new Path(fileUri.getRawPath())).getCProject();
		ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(
				fileUri, cProject);
		IIndex index = CCorePlugin.getIndexManager().getIndex(cProject);
		return tu.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
	}

	public static IASTTranslationUnit getGlobalTranslationUnitForFile(
			URI fileUri) throws CoreException {
		ICProject cProject = CoreModel.getDefault()
				.create(new Path(fileUri.getRawPath())).getCProject();
		ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(
				fileUri, cProject);
		IIndex index = CCorePlugin.getIndexManager().getIndex(cProject);
		return tu.getAST(index, ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT);
	}
}
