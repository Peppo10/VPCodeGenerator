package org.giuse.JavaGenerator;

import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPContext;
import com.vp.plugin.action.VPContextActionController;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.model.*;
import org.giuse.JavaGenerator.generator.Generator;
import org.giuse.JavaGenerator.parser.Parser;
import org.giuse.JavaGenerator.utils.Config;
import org.giuse.JavaGenerator.utils.GUI;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

import static org.giuse.JavaGenerator.utils.GUI.viewManager;

public class GenerateJavaActionController implements VPContextActionController {
    public static final String TAG = "GenerateJava";
    public GenerateJavaActionController(){

    }

    @Override
    public void performAction(VPAction vpAction, VPContext vpContext, ActionEvent actionEvent) {
        IModelElement modelElement = vpContext.getModelElement();

        JFileChooser fileChooser = GUI.createGeneratorFileChooser(TAG);
        if (fileChooser.showOpenDialog(viewManager.getRootFrame()) == JFileChooser.APPROVE_OPTION) {

            String choosePath;
            try {
                choosePath = fileChooser.getSelectedFile().getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(modelElement instanceof IClass){
                Parser parser = Parser.getInstance(vpContext.getDiagram().getName(), choosePath);

                if(modelElement.hasStereotype("Interface"))
                    parser.parseSingleInterface((IClass) modelElement);
                else
                    parser.parseSingleClass((IClass) modelElement);

                Generator.generate(parser.getCodebase());
            } else if (modelElement instanceof IPackage) {
                Parser parser = Parser.getInstance(vpContext.getDiagram().getName(), choosePath);
                parser.parsePackage((IPackage) modelElement);
                Generator.generate(parser.getCodebase());
            } else{
                Parser parser = Parser.getInstance(vpContext.getDiagram().getName(), choosePath);
                parser.parseDiagram(vpContext.getDiagram());
                Generator.generate(parser.getCodebase());
            }

            GUI.showInformationMessageDialog(viewManager.getRootFrame(), TAG, "Code Generated Successfully");
        }
    }

    @Override
    public void update(VPAction vpAction, VPContext vpContext) {

    }
}
