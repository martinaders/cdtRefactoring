package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToClassStrategy extends
		ToggleRefactoringAbstractStrategy {

	private IASTTranslationUnit declaration_unit;
	private String path;
	private String filename;
	private String origin_filename;
	private ToggleRefactoringContext context;
	private String filename_without_extension;

	public ToggleFromImplementationToClassStrategy(
			ToggleRefactoringContext context) {
		super(context.getDeclaration(), context.getDefinition(), context.getDefinitionUnit());
		this.context = context;
		
		this.declaration_unit = context.getDeclarationUnit();
		if (this.declaration_unit == null) {
			path = context.getSelectionFile().getFullPath().toString();
			System.out.println("path: " + path);
			filename = ToggleSelectionHelper.getFilenameWithoutExtension(path);
			path = path.replaceAll("(\\w)*\\.(\\w)*", "");
			System.out.println("filename " + filename);
			this.filename_without_extension = filename;
			if (context.getSelectionFile().getFileExtension().equals("h")) {
				origin_filename = filename + ".h";
				filename += ".cpp";
			}
			if (context.getSelectionFile().getFileExtension().equals("cpp")) {
				origin_filename = filename + ".cpp";
				filename += ".h";
			}
			
			System.out.println("filename after all: " + path + filename);
		}
	}

	@Override
	public void run(ModificationCollector modifications) {
		ASTRewrite implast = modifications
		.rewriterForTranslationUnit(definition_unit);
		implast.remove(selectedDefinition, infoText);
		
		if (this.declaration_unit == null) {
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
		} else {
			ASTRewrite headerast = modifications
			.rewriterForTranslationUnit(declaration_unit);
			
			IASTFunctionDefinition function = new CPPASTFunctionDefinition();
			function.setBody(selectedDefinition.getBody().copy());
			
			//IASTFunctionDeclarator declarator = new CPPASTFunctionDeclarator(new CPPASTName(selectedDeclaration.getName().copy().toCharArray()));
			IASTFunctionDeclarator declarator = selectedDeclaration.copy();
			declarator.setParent(function);
			function.setDeclarator(declarator);
			System.out.println("name: " + selectedDeclaration.getName());
			IASTDeclSpecifier declspec = selectedDefinition.getDeclSpecifier().copy();
			function.setDeclSpecifier(declspec);
			IASTFunctionDefinition finalfunc = assembleFunctionDefinitionWithBody(declspec, function.getDeclarator());
			finalfunc.setParent(selectedDeclaration.getParent().getParent());
			
			headerast.insertBefore(selectedDeclaration.getParent().getParent(), null, finalfunc, infoText);
			headerast.remove(selectedDeclaration.getParent(), infoText);			
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
