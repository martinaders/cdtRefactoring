/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 * 		Martin Schwab & Thomas Kallenberg - initial API and implementation 
 ******************************************************************************/
package ch.hsr.eclipse.cdt.ui.toggle;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.ui.refactoring.CreateFileChange;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.refactoring.CTextFileChange;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings("restriction")
public class ToggleFromImplementationToClassStrategy implements ToggleRefactoringStrategy {

	private ToggleRefactoringContext context;
	private String filename_without_extension;
	protected TextEditGroup infoText;

	public ToggleFromImplementationToClassStrategy(
			ToggleRefactoringContext context) {
		this.context = context;
		this.infoText = new TextEditGroup("Toggle function body placement");
	}


	@Override
	public void run(ModificationCollector modifications) {
		if (context.getDeclarationUnit() != null) {
			ASTRewrite headerast = modifications.rewriterForTranslationUnit(context.getDeclarationUnit());
			IASTNode oldDeclaration = context.getDeclaration().getParent();
			IASTFunctionDefinition newDefinition = ToggleNodeHelper.createInClassDefinition(context.getDeclaration(), context.getDefinition(), context.getDeclarationUnit());
			newDefinition.setParent(oldDeclaration.getParent());
			headerast.addRecursiveCommentMapping(context.getDefinition(), newDefinition);
			headerast.addCommentMapping(oldDeclaration, newDefinition);
			headerast.replace(oldDeclaration, newDefinition, infoText);
			//headerast.remove(selectedDeclaration.getParent(), infoText);			
			//headerast.insertBefore(selectedDeclaration.getParent().getParent(), null, finalfunc, infoText);
		} else {
			IASTTranslationUnit other_unit = null;
			try {
				other_unit = ToggleNodeHelper.getSiblingFile(context.getSelectionFile(), context.getDefinitionUnit());
				IASTFunctionDefinition function = context.getDefinition().copy();
				ASTRewrite classast = modifications.rewriterForTranslationUnit(context.getDeclarationUnit());
				classast.insertBefore(other_unit, null, function, infoText);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (context.getDeclarationUnit() == null)
				writeNewFile(modifications);
		}
		ASTRewrite implast = modifications.rewriterForTranslationUnit(context.getDefinitionUnit());
		implast.remove(context.getDefinition(), infoText);
	}

	private void writeNewFile(ModificationCollector modifications) {
		IASTFunctionDefinition func = context.getDefinition().copy();
		IASTDeclSpecifier spec = new CPPASTSimpleDeclSpecifier();
		spec.setInline(false);
		func.setDeclSpecifier(spec);
		func.setParent(context.getDefinition().getParent());
		String declaration = func.getRawSignature();
		declaration = declaration.replaceAll("inline ", "");
		CreateFileChange change;
		String path = context.getSelectionFile().getFullPath().toString();
		String filename = generateNewFilename(path);
		try {
			change = new CreateFileChange(filename, new
			Path(path+filename), getIncludeGuardStatementAsString() + "\n" + getClassStart(func.getDeclarator().getRawSignature()) + "\n\t" + getPureDeclaration(declaration) + "\n" + "};" + "\n\n" + getIncludeGuardEndStatementAsString(), context.getSelectionFile().getCharset());
			modifications.addFileChange(change);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		//insertIncludeStatement(filename);			
	}

	private String generateNewFilename(String path) {
		String filename = ToggleNodeHelper.getFilenameWithoutExtension(path);
		path = path.replaceAll("(\\w)*\\.(\\w)*", "");
		this.filename_without_extension = filename;
		if (context.getSelectionFile().getFileExtension().equals("h")) {
			return filename += ".cpp";
		}
		if (context.getSelectionFile().getFileExtension().equals("cpp")) {
			return filename += ".h";
		}
		return null;
	}

	private void insertIncludeStatement(String filename) {
		Path p = new Path(context.getDefinitionUnit().getFileLocation().getFileName());
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
		return result;
	}

	private String getClassStart(String definition) {
		String tmp = definition.replaceAll("::.*", "");
		tmp += " {";
		String result = "class " + tmp;
		return result;
	}

	private String getIncludeGuardStatementAsString() {
		String result = "";
		result += "#ifndef " + filename_without_extension.toUpperCase() + "_" + "H" + "_" + "\n";
		result += "#define " + filename_without_extension.toUpperCase() + "_" + "H" + "_" + "\n";
		return result;
	}
	
	private String getIncludeGuardEndStatementAsString() {
		String result = "";
		result += "#endif " + "/* " + filename_without_extension.toUpperCase() + "_" + "H" + "_" + "*/" + "\n";
		return result;
	}
}
