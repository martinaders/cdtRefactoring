package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
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
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

/**
 * Helps finding a FunctionDefinition in the parent nodes of the current selection. 
 */
@SuppressWarnings("restriction")
class ToggleSelectionHelper extends SelectionHelper {		

	public static ArrayList<IASTName> getAllQualifiedNames(IASTNode node) {
		ArrayList<IASTName> names = new ArrayList<IASTName>();
		
		boolean isInsideAClass = false;
		while(node.getParent() != null) {
			node = node.getParent();
			if (node instanceof ICPPASTCompositeTypeSpecifier) {
				isInsideAClass = true;
				names.add(((ICPPASTCompositeTypeSpecifier) node).getName());
			}
			else if (node instanceof ICPPASTNamespaceDefinition) {
				names.add(((ICPPASTNamespaceDefinition) node).getName());
			}
			else if (node instanceof ICPPASTTemplateDeclaration) {
				for(IASTNode child : node.getChildren()) {
					if (child instanceof ICPPASTSimpleTypeTemplateParameter) {
						if (isInsideAClass) {
							IASTName name = names.remove(names.size()-1);
							IASTName toadd = new CPPASTName((name + "<" + getTemplateParameterName(child) + ">").toCharArray());
							names.add(toadd);
						} else {
							names.add(getTemplateParameterName(child));
						}
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

	public static URI getSiblingFile(IFile file) throws CoreException {
		ICProject cProject = CoreModel.getDefault().create(file).getCProject();
		IIndex projectindex = CCorePlugin.getIndexManager().getIndex(cProject);
		ITranslationUnit tu = CoreModelUtil.findTranslationUnit(file);
		IASTTranslationUnit asttu = tu.getAST(projectindex, ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT);
		IIndexFile thisfile = projectindex.getFile(asttu.getLinkage().getLinkageID(),
				IndexLocationFactory.getWorkspaceIFL(file));
		String filename = getFilenameWithoutExtension(file.getFullPath()
				.toString());
		if (file.getFileExtension().equals("h")) {
			for (IIndexInclude include : projectindex.findIncludedBy(thisfile)) {
				if (getFilenameWithoutExtension(include.getIncludedBy().getLocation().getFullPath()).equals(filename)) {
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
	
	public static String getFilenameWithoutExtension(String filename) {
		filename = filename.replaceAll("\\.(.)*$", "");
		filename = filename.replaceAll("(.)*\\/", "");
		return filename;
	}

	public static IASTTranslationUnit getLocalTranslationUnitForFile(URI fileUri)
			throws CModelException, CoreException {
		Path p = new Path(fileUri.getRawPath());
		ICElement e = CoreModel.getDefault().create(p);
		ICProject cProject = e.getCProject();
		ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(fileUri, cProject);
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
		return tu.getAST(index, ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT | ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
	}
}
