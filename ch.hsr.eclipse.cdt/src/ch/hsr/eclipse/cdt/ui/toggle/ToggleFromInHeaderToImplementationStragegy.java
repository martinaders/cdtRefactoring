package ch.hsr.eclipse.cdt.ui.toggle;

import java.net.URI;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.index.IndexLocationFactory;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToImplementationStragegy extends
		ToggleRefactoringAbstractStrategy {

	private IFile file;
	private ICProject project;

	public ToggleFromInHeaderToImplementationStragegy(
			ICPPASTFunctionDeclarator selectedDeclaration,
			IASTFunctionDefinition selectedDefinition,
			IASTTranslationUnit unit, ICProject project, IFile file) {
		super(selectedDeclaration, selectedDefinition, unit);
		this.project = project;
		this.file = file;
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(unit);
		IASTTranslationUnit otherast = null;
		try {
			otherast = getTranslationUnitForFile(getSiblingFile());
			ASTRewrite otherrewrite = modifications
					.rewriterForTranslationUnit(otherast);
			IASTNode toremove = selectedDefinition;
			if (toremove.getParent() != null
					&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
				toremove = selectedDefinition.getParent();
			rewriter.remove(toremove, infoText);
			otherrewrite.insertBefore(otherast.getTranslationUnit(), null,
					getQualifiedNameDefinition(false), infoText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private URI getSiblingFile() throws CoreException {
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

	private String getFilenameWithoutExtension(String filename) {
		filename = filename.replaceAll("\\.(.)*$", "");
		filename = filename.replaceAll("(.)*\\/", "");
		System.out.println("filename: " + filename);
		return filename;
	}

	private IASTTranslationUnit getTranslationUnitForFile(URI fileUri)
			throws CModelException, CoreException {
		IASTTranslationUnit otherast;
		ICProject cProject = CoreModel.getDefault()
				.create(new Path(fileUri.getRawPath())).getCProject();
		ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(
				fileUri, cProject);
		IIndex index = CCorePlugin.getIndexManager().getIndex(cProject);
		otherast = tu.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
		return otherast;
	}

}
