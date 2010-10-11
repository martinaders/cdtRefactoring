package ch.hsr.eclipse.cdt;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "ch.hsr.eclipse.cdt"; //$NON-NLS-1$
	private static Activator plugin;
	
	public Activator() {
		System.out.println("Toggle-Plugin created");
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		System.out.println("Toggle-Plugin started");
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

}
