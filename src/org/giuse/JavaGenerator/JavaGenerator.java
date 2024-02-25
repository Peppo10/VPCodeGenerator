package org.giuse.JavaGenerator;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.VPPlugin;
import com.vp.plugin.VPPluginInfo;
import com.vp.plugin.ViewManager;

public class JavaGenerator implements VPPlugin {

	public static final String PLUGIN_NAME = "JavaGenerator";
	public static final ViewManager viewManager = ApplicationManager.instance().getViewManager();

	@Override
	public void loaded(VPPluginInfo vpi){
	}

	@Override
	public void unloaded() {

	}
}