package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.refactoring.CTextFileChange;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.text.edits.InsertEdit;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToClassStrategy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit declaration_unit;
	private String path;
	private String filename;
	private ToggleRefactoringContext context;
	private String filename_without_extension;

	public ToggleFromImplementationToClassStrategy(
			ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
		this.context = context;
		
		this.declaration_unit = context.getDeclarationUnit();
		if (this.declaration_unit == null)
			createNewFilename(context);
	}

	private void createNewFilename(ToggleRefactoringContext context) {
		path = context.getSelectionFile().getFullPath().toString();
		System.out.println("path: " + path);
		filename = ToggleSelectionHelper.getFilenameWithoutExtension(path);
		path = path.replaceAll("(\\w)*\\.(\\w)*", "");
		System.out.println("filename " + filename);
		this.filename_without_extension = filename;
		if (context.getSelectionFile().getFileExtension().equals("h")) {
			filename += ".cpp";
		}
		if (context.getSelectionFile().getFileExtension().equals("cpp")) {
			filename += ".h";
		}
		System.out.println("filename after all: " + path + filename);
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite implast = modifications.rewriterForTranslationUnit(definition_unit);
		implast.remove(selectedDefinition, infoText);
		
		if (this.declaration_unit == null) {
			writeNewFile(modifications);
		} else {
			ASTRewrite headerast = modifications
			.rewriterForTranslationUnit(declaration_unit);
			
			IASTFunctionDefinition function = new CPPASTFunctionDefinition();
			function.setBody(selectedDefinition.getBody().copy());
			
			IASTFunctionDeclarator declarator = selectedDeclaration.copy();
			declarator.setParent(function);
			function.setDeclarator(declarator);
			System.out.println("name: " + selectedDeclaration.getName());
			ICPPASTSimpleDeclSpecifier declspec = (ICPPASTSimpleDeclSpecifier) selectedDefinition.getDeclSpecifier().copy();
			IASTSimpleDeclaration dec = (IASTSimpleDeclaration) selectedDeclaration.getParent();
			ICPPASTSimpleDeclSpecifier olddeclspec = (ICPPASTSimpleDeclSpecifier) dec.getDeclSpecifier();
			if (olddeclspec.isVirtual())
				declspec.setVirtual(true);
			//how to get to the declaration specifier of the function declarator?
			function.setDeclSpecifier(declspec);
			IASTFunctionDefinition finalfunc = assembleFunctionDefinitionWithBody(declspec, function.getDeclarator());
			finalfunc.setParent(selectedDeclaration.getParent().getParent());
			
			headerast.replace(selectedDeclaration.getParent(), finalfunc, infoText);
//			headerast.remove(selectedDeclaration.getParent(), infoText);			
//			headerast.insertBefore(selectedDeclaration.getParent().getParent(), null, finalfunc, infoText);
		}
	}

	private void writeNewFile(ModificationCollector modifications) {
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
			Path(path+filename), getIncludeGuardStatementAsString() + "\n" + getClassStart(func.getDeclarator().getRawSignature()) + "\n\t" + getPureDeclaration(declaration) + "\n" + "};" + "\n\n" + GetIncludeGuardEndStatementAsString(), context.getSelectionFile().getCharset());
			modifications.addFileChange(change);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		//insertIncludeStatement();			
	}

	private void insertIncludeStatement() {
		Path p = new Path(definition_unit.getFileLocation().getFileName());
		ICElement e = CoreModel.getDefault().create(p);
		ICProject cProject = e.getCProject();
		ITranslationUnit tu;
		try {
			tu = CoreModelUtil.findTranslationUnitForLocation(p, cProject);
			InsertEdit edit = new InsertEdit(0, "#include \"" + filename + "\"");
			CTextFileChange insertchange = new CTextFileChange("achange", tu);
			insertchange.setEdit(edit);
			insertchange.perform(new NullProgressMonitor());
		} catch (CModelException e1) {
			e1.printStackTrace();
		} catch (CoreException e2) {
			e2.printStackTrace();
		}
	}
	
	private String getPureDeclaration(String declaration) {
		String result = declaration.replaceAll("(\\w)*::", "");
		System.out.println("pure declaration: " + result);
		return result;
	}

	private String getClassStart(String definition) {
		String tmp = definition.replaceAll("::.*", "");
		tmp += " {";
		String result = "class " + tmp;
		System.out.println("class: " + result);
		return result;
	}

	private String getIncludeGuardStatementAsString() {
		String result = "";
		result += "#ifndef " + filename_without_extension.toUpperCase() + "_" + "H" + "_" + "\n";
		result += "#define " + filename_without_extension.toUpperCase() + "_" + "H" + "_" + "\n";
		return result;
	}
	
	private String GetIncludeGuardEndStatementAsString() {
		String result = "";
		result += "#endif " + "/* " + filename_without_extension.toUpperCase() + "_" + "H" + "_" + "\n";
		return result;
	}
	
}
