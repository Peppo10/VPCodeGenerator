package org.giuse.CodeGenerator;

import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPContext;
import com.vp.plugin.action.VPContextActionController;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.diagram.shape.IPackageUIModel;
import com.vp.plugin.model.*;
import org.giuse.CodeGenerator.generator.Generator;
import org.giuse.CodeGenerator.parser.Parser;
import org.giuse.CodeGenerator.utils.GUI;
import javax.swing.*;
import java.awt.event.ActionEvent;

import static org.giuse.CodeGenerator.utils.GUI.viewManager;

public class GenerateCodeContextActionController implements VPContextActionController {
    public static final String TAG = "GenerateJava";
    public GenerateCodeContextActionController(){

    }

    @Override
    public void performAction(VPAction vpAction, VPContext vpContext, ActionEvent actionEvent) {
        IDiagramElement diagramElement = vpContext.getDiagramElement();
        IModelElement modelElement = diagramElement != null ? diagramElement.getModelElement() : null;

        JFileChooser fileChooser = GUI.createGeneratorFileChooser(TAG);
        if (fileChooser.showOpenDialog(viewManager.getRootFrame()) == JFileChooser.APPROVE_OPTION) {

            String choosePath = fileChooser.getSelectedFile().getAbsolutePath();
            Parser parser = Parser.getInstance(vpContext, choosePath);

            if(modelElement instanceof IClass)
                parser.parseSingleClass((IClassUIModel) vpContext.getDiagramElement());
            else if (modelElement instanceof IPackage)
                parser.parseSinglePackage((IPackageUIModel) vpContext.getDiagramElement());
            else
                parser.parseDiagram(vpContext.getDiagram());

            Generator.generate(parser.getCodebase());
        }
    }

    @Override
    public void update(VPAction vpAction, VPContext vpContext) {

    }
}
