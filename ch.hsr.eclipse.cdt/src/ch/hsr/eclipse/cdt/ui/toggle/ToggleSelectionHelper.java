package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;
import java.util.Stack;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.index.IndexLocationFactory;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateId;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTypeId;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Helps finding a FunctionDefinition in the parent nodes of the current selection. 
 */
@SuppressWarnings("restriction")
class ToggleSelectionHelper extends SelectionHelper {		

	private static ICPPASTTemplateId getTemplateParameter(IASTNode node, IASTName name) {
		ICPPASTTemplateId templateid = new CPPASTTemplateId();
		templateid.setTemplateName(name.copy());
		
		for(IASTNode child : node.getChildren()) {
			if (child instanceof ICPPASTSimpleTypeTemplateParameter) {
				ICPPASTSimpleTypeTemplateParameter tempcild = (ICPPASTSimpleTypeTemplateParameter) child;

				CPPASTNamedTypeSpecifier namedTypeSpecifier = new CPPASTNamedTypeSpecifier();
				namedTypeSpecifier.setName(tempcild.getName().copy());
				
				CPPASTTypeId id = new CPPASTTypeId();
				id.setDeclSpecifier(namedTypeSpecifier);
				templateid.addTemplateArgument(id);
			}
		}
		return templateid;
	}
	
	public static boolean isInsideAClass(IASTFunctionDeclarator declarator,
			IASTFunctionDeclarator backup) {
		if (declarator.getName() instanceof ICPPASTQualifiedName)
			declarator = backup;
		IASTNode node = declarator;
		while (node != null) {
			if (node instanceof IASTCompositeTypeSpecifier)
				return true;
			node = node.getParent();
		}
		return false;
	}
	
	public static boolean isNamespacedOrTemplated(
			IASTFunctionDeclarator declarator,
			IASTFunctionDeclarator backup) {
		if (declarator.getName() instanceof ICPPASTQualifiedName)
			declarator = backup;
		IASTNode node = declarator;
		while (node != null) {
			if (node instanceof ICPPASTNamespaceDefinition || node instanceof ICPPASTTemplateDeclaration)
				return true;
			node = node.getParent();
		}
		return false;
	}
	
	public static ICPPASTQualifiedName getQualifiedName(IASTFunctionDeclarator declarator) {
		IASTNode node = declarator;
		Stack<IASTNode> nodes = new Stack<IASTNode>();
		IASTName lastname = declarator.getName();
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof IASTCompositeTypeSpecifier) {
				nodes.push(((IASTCompositeTypeSpecifier) node).copy());
				lastname = ((IASTCompositeTypeSpecifier) node).getName();
			}
			else if (node instanceof ICPPASTNamespaceDefinition) {
				nodes.push(((ICPPASTNamespaceDefinition) node).copy());
				lastname = ((ICPPASTNamespaceDefinition) node).getName();
			}
			else if (node instanceof ICPPASTTemplateDeclaration) {
				if (!nodes.isEmpty())
					nodes.pop();
				ICPPASTTemplateId templateid = getTemplateParameter(node, lastname);
				nodes.add(templateid);
			} 
		}
		
		CPPASTQualifiedName result = new CPPASTQualifiedName();
		IASTName name;
		while(!nodes.isEmpty()) {
			IASTNode nnode = nodes.pop();
			if (nnode instanceof IASTCompositeTypeSpecifier) {
				name = ((IASTCompositeTypeSpecifier) nnode).getName();
				result.addName(name);
			}
			else if (nnode instanceof ICPPASTNamespaceDefinition) {
				name = ((ICPPASTNamespaceDefinition) nnode).getName(); 
				result.addName(name);
			}
			else if (nnode instanceof ICPPASTTemplateId) {
				ICPPASTTemplateId id = (ICPPASTTemplateId) nnode;
				result.addName(id);
			}
		}
		result.addName(declarator.getName().copy());
		return result;
	}

	public static IASTTranslationUnit getSiblingFile(IFile file, IASTTranslationUnit asttu) throws CoreException {
		ICProject cProject = CoreModel.getDefault().create(file).getCProject();
		IIndex projectindex = CCorePlugin.getIndexManager().getIndex(cProject);
		
		IIndexFile thisfile = projectindex.getFile(asttu.getLinkage().getLinkageID(),IndexLocationFactory.getWorkspaceIFL(file));
		String filename = getFilenameWithoutExtension(file.getFullPath().toString());
		if (file.getFileExtension().equals("h")
				|| file.getFileExtension().equals("hpp")) {
			for (IIndexInclude include : projectindex.findIncludedBy(thisfile)) {
				if (getFilenameWithoutExtension(include.getIncludedBy().getLocation().getFullPath()).equals(filename)) {
					ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(include.getIncludedBy().getLocation().getURI(), cProject);
					return tu.getAST(projectindex, ITranslationUnit.AST_SKIP_ALL_HEADERS);
				}
			}
		} else if (file.getFileExtension().equals("cpp") || file.getFileExtension().equals("cxx")
				|| file.getFileExtension().equals("c")) {
			for (IIndexInclude include : projectindex.findIncludes(thisfile)) {
				if (getFilenameWithoutExtension(include.getFullName()).equals(
						filename)) {
					URI uri = include.getIncludesLocation().getURI();
					ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(uri, cProject);
					return tu.getAST(projectindex, ITranslationUnit.AST_SKIP_ALL_HEADERS);
					
				}
			}
		}
		return null;
	}
	
	public static String getFilenameWithoutExtension(String filename) {
		filename = filename.replaceAll("\\.(.)*$", "");
		filename = filename.replaceAll("(.)*\\/", "");
		return filename;
	}
}
