package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

@SuppressWarnings("restriction")
public class ToggleFromInHeaderToImplementationStrategy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit siblingfile_translation_unit;
	private String path;
	private String filename;
	private ToggleRefactoringContext context;
	private String origin_filename;

	public ToggleFromInHeaderToImplementationStrategy(ToggleRefactoringContext context) throws CModelException, CoreException {
		super(context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
		this.context = context;
		this.siblingfile_translation_unit = context.getTUForSiblingFile();
		if (this.siblingfile_translation_unit == null) {
			path = context.getSelectionFile().getFullPath().toString();
			filename = ToggleSelectionHelper.getFilenameWithoutExtension(path);
			path = path.replaceAll("(\\w)*\\.(\\w)*", "");
			if (context.getSelectionFile().getFileExtension().equals("h")) {
				origin_filename = filename + ".h";
				filename += ".cpp";
			}
			if (context.getSelectionFile().getFileExtension().equals("cpp")) {
				origin_filename = filename + ".cpp";
				filename += ".h";
			}
		}
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite rewriter = modifications.rewriterForTranslationUnit(definition_unit);
		IASTNode toremove = selectedDefinition;
		if (toremove.getParent() != null
				&& toremove.getParent() instanceof ICPPASTTemplateDeclaration)
			toremove = selectedDefinition.getParent();
		rewriter.remove(toremove, infoText);
		
		
		if (this.siblingfile_translation_unit == null) {
			IASTFunctionDefinition func = selectedDefinition.copy();
			IASTDeclSpecifier spec = new CPPASTSimpleDeclSpecifier();
			spec.setInline(false);
			func.setDeclSpecifier(spec);
			func.setParent(selectedDefinition.getParent());
			String declaration = func.getRawSignature();
			declaration = declaration.replaceAll("inline ", "");
			CreateFileChange change;
			try {
				change = new CreateFileChange(filename, new
				Path(path+filename), "#include " + "\"" + origin_filename + "\"\n\n" + declaration, context.getSelectionFile().getCharset());
				modifications.addFileChange(change);			
			} catch (CoreException e) {
				e.printStackTrace();
			}
		} else {
			ASTRewrite otherrewrite = modifications
			.rewriterForTranslationUnit(siblingfile_translation_unit);
			otherrewrite.insertBefore(siblingfile_translation_unit.getTranslationUnit(), null,
					getQualifiedNameDefinition(false), infoText);
		}
	}
	
	
}
