package org.giuse.CodeGenerator;

import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import org.giuse.CodeGenerator.utils.GenerateDialogHandler;
import static org.giuse.CodeGenerator.utils.GUI.viewManager;

public class GenerateCodeActionController implements VPActionController {
    public static final String TAG = "Generate";
    @Override
    public void performAction(VPAction vpAction) {
        viewManager.showDialog(new GenerateDialogHandler());
    }

    @Override
    public void update(VPAction vpAction) {

    }
}
