package ch.hsr.eclipse.cdt.ui.toggle;

/**
 * Thrown when the developer had no time to implement a special case the user
 * tried to refactor.
 * 
 */
public class NotSupportedException extends Throwable {

	private static final long serialVersionUID = -4359705945683270L;

	public NotSupportedException(String message) {
		super(message);
	}
}
