package org.codegenerator;

import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import org.codegenerator.utils.AboutDialogHandler;

import static org.codegenerator.utils.GUI.viewManager;

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
