package org.giuse.JavaGenerator;

import com.vp.plugin.ViewManager;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPContext;
import com.vp.plugin.action.VPContextActionController;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.model.*;
import org.giuse.JavaGenerator.generator.Generator;
import org.giuse.JavaGenerator.parser.Parser;
import java.awt.event.ActionEvent;

import static org.giuse.JavaGenerator.JavaGenerator.viewManager;

public class ActionController implements VPContextActionController {
    public ActionController(){

    }

    @Override
    public void performAction(VPAction vpAction, VPContext vpContext, ActionEvent actionEvent) {
        IModelElement modelElement = vpContext.getModelElement();

        if(modelElement instanceof IClass){
            Parser parser = Parser.getInstance(vpContext.getDiagram().getName());

            if(modelElement.hasStereotype("Interface"))
                parser.parseSingleInterface((IClass) modelElement);
            else
                parser.parseSingleClass((IClass) modelElement);

            Generator.generate(parser.getCodebase());
        } else if (modelElement instanceof IPackage) {
            Parser parser = Parser.getInstance(vpContext.getDiagram().getName());
            parser.parsePackage((IPackage) modelElement);
            Generator.generate(parser.getCodebase());
        } else{
            Parser parser = Parser.getInstance(vpContext.getDiagram().getName());
            parser.parseDiagram((IDiagramUIModel) vpContext.getDiagram());
            Generator.generate(parser.getCodebase());
        }
    }

    @Override
    public void update(VPAction vpAction, VPContext vpContext) {

    }
}
