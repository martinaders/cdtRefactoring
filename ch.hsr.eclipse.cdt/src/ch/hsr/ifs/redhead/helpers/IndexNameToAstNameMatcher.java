package ch.hsr.ifs.redhead.helpers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.parser.util.CharArrayUtils;
import org.eclipse.core.runtime.CoreException;

public class IndexNameToAstNameMatcher extends ASTVisitor {
	private IASTName result;
	private IBinding bindingToFind;
	private char[] charNameToFind;
	private IASTFileLocation locationToFind;

	public IndexNameToAstNameMatcher(IASTTranslationUnit tu,
			IIndexName indexName, IIndex index) throws CoreException {
		super(true);
		locationToFind = indexName.getFileLocation();
		bindingToFind = index.findBinding(indexName);
		charNameToFind = bindingToFind.getNameCharArray();
		shouldVisitImplicitNames = true;
		shouldVisitImplicitNameAlternates = true;
	}

	@Override
	public int visit(IASTName candidate) {
		if (!IndexToASTNameHelper.shouldConsiderName(candidate)) {
			return PROCESS_CONTINUE;
		}
		if (isEquivalent(candidate)) {
			result = candidate;
			return PROCESS_ABORT;
		}
		return PROCESS_CONTINUE;
	}

	private boolean isEquivalent(IASTName candidate) {
		if (!matchesIndexName(candidate))
			return false;
//		IBinding binding = candidate.resolveBinding();
//		IIndexBinding indexbinding = index.adaptBinding(binding);
//		boolean eq = bindingToFind.equals(indexbinding);
//		if (!(eq))
//			return false;
		return true;
	}

	private boolean matchesIndexName(IASTName candidate) {
		IASTFileLocation candidateLocation = candidate.getFileLocation();
		if (!(locationToFind.getNodeOffset() == candidateLocation.getNodeOffset()))
			return false;
		if (!(locationToFind.getNodeLength() == candidateLocation.getNodeLength()))
			return false;
		if (!locationToFind.getFileName().equals(candidateLocation.getFileName()))
			return false;
		if (!CharArrayUtils.equals(candidate.getLookupKey(),charNameToFind))
			return false;
		return true;
	}

	public IASTName getMatch() {
		return result;
	}
}

class BindingToAstNameMatcher extends ASTVisitor {

	private List<IASTName> results = new ArrayList<IASTName>();
	private IBinding bindingToFind;
	private char[] toFindName;
	private IIndex index;

	public BindingToAstNameMatcher(IBinding binding, IIndex index) {
		super(true);
		bindingToFind = index.adaptBinding(binding);
		this.index = index;
		toFindName = binding.getNameCharArray();
		shouldVisitImplicitNames = true;
		shouldVisitImplicitNameAlternates = true;
	}

	@Override
	public int visit(IASTName candidate) {
		if (!IndexToASTNameHelper.shouldConsiderName(candidate)) {
			return PROCESS_CONTINUE;
		}
		if (isEquivalent(candidate)) {
			results.add(candidate);
		}
		return PROCESS_CONTINUE;
	}

	private boolean isEquivalent(IASTName candidate) {
		return CharArrayUtils.equals(candidate.getLookupKey(), toFindName)
				&& bindingToFind.equals(index.adaptBinding(candidate
						.resolveBinding()));
	}

	public List<IASTName> getMatches() {
		return results;
	}

}
