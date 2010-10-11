package ch.hsr.eclipse.cdt;

import org.eclipse.cdt.ui.refactoring.actions.Messages;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ch.hsr.eclipse.cdt"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

//    public static final String MENU_ID = "org.eclipse.cdt.ui.refactoring.menu"; //$NON-NLS-1$
//    public static final String GROUP_REORG = "reorgGroup"; //$NON-NLS-1$
//    private String fGroupName= IWorkbenchActionConstants.GROUP_REORGANIZE;
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

//		IMenuManager refactorSubmenu = new MenuManager(org.eclipse.cdt.ui.refactoring.actions.Messages.CRefactoringActionGroup_menu, MENU_ID);
//		IMenuManager menu = null;
//		menu.appendToGroup(fGroupName, refactorSubmenu);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
