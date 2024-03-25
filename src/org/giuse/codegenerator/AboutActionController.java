package org.giuse.codegenerator;

import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import org.giuse.codegenerator.utils.AboutDialogHandler;

import static org.giuse.codegenerator.utils.GUI.viewManager;

public class AboutActionController implements VPActionController {
    public static final String TAG = "About";
    @Override
    public void performAction(VPAction vpAction) {
        viewManager.showDialog(new AboutDialogHandler());
    }

    @Override
    public void update(VPAction vpAction) {

    }
}
