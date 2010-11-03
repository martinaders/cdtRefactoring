package ch.hsr.ifs.redhead.helpers;

import org.eclipse.cdt.core.dom.IName;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.core.runtime.CoreException;

public class IndexToASTNameHelper {

	public static IASTName findMatchingASTName(IASTTranslationUnit tu,
			IName name, IIndex index) throws CoreException {
		if (name instanceof IASTName) {
			return (IASTName) name;
		} else if (!(name instanceof IIndexName)) {
			return null;
		}

		IndexNameToAstNameMatcher visitor = new IndexNameToAstNameMatcher(tu,
				(IIndexName) name, index);
		tu.accept(visitor);
		return visitor.getMatch();
	}

	static boolean shouldConsiderName(IASTName candidate) {
		return !isQualifiedName(candidate)
				&& isLastNameInQualifiedName(candidate)
				&& !isUnnamedName(candidate);
	}

	private static boolean isLastNameInQualifiedName(IASTName name) {
		if (name.getParent() instanceof ICPPASTQualifiedName) {
			ICPPASTQualifiedName qName = (ICPPASTQualifiedName) name
					.getParent();
			return name.equals(qName.getLastName());
		}
		return true;
	}

	private static boolean isUnnamedName(IASTName name) {
		return name.getFileLocation() == null && "".equals(name.toString());
	}

	private static boolean isQualifiedName(IASTName name) {
		return name instanceof ICPPASTQualifiedName;
	}
}
