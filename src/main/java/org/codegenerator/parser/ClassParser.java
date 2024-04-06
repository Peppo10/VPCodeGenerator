package org.codegenerator.parser;

import com.vp.plugin.diagram.ICompartmentColorModel;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.model.*;
import org.codegenerator.logger.Logger;
import org.codegenerator.parser.models.*;
import org.codegenerator.parser.models.Class;
import org.codegenerator.parser.models.Enum;
import org.codegenerator.utils.CGContext;
import org.codegenerator.utils.FormatUtils;
import org.codegenerator.utils.GUI;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.codegenerator.parser.Parser.*;

public class ClassParser{
    private enum ClassType{CLASS,INTERFACE,ENUM}
    private final TemplateParser templateParser = new TemplateParser();
    private final AttributeParser attributeParser;
    private final FunctionParser functionParser;
    private final CGContext context;

    public ClassParser(CGContext context) {
        this.context = context;
        this.functionParser = new FunctionParser(context);
        this.attributeParser = new AttributeParser(context);
    }

    public Struct parseClass(IClassUIModel iClassUIModel, String parentPath){
        IClass iClass = (IClass) iClassUIModel.getModelElement();

        //endRelationship.getMasterView().getDiagramUIModel().getName()

        ClassType classType;
        AtomicBoolean hasExtend = new AtomicBoolean(false);
        Struct.Builder builder;

        String optionalPackagePath = parentPath;

        if(parentPath == null)
            optionalPackagePath = getElementPath(iClassUIModel, context.getCodebase());

        if(iClass.hasStereotype("Interface")){
            builder = new Interface.Builder(optionalPackagePath + File.separator + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.INTERFACE;
        }
        else if(iClass.hasStereotype("Enum")){
            builder = new Enum.Builder(optionalPackagePath + File.separator + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.ENUM;
        }
        else{
            builder = new Class.Builder(optionalPackagePath + File.separator + iClass.getName() + ".java", iClass.getVisibility(), iClass.getName());
            classType = ClassType.CLASS;
        }


        builder.setPackage(getClassPackage(iClass, context));

        if((classType == ClassType.CLASS) && iClass.isAbstract())
            ((Class.Builder)builder).isAbstract();

        IAttribute[] attributes = iClass.toAttributeArray();
        IOperation[] operations = iClass.toOperationArray();
        IRelationshipEnd[] relationshipsFrom = iClass.toFromRelationshipEndArray();
        IRelationshipEnd[] relationshipsTo = iClass.toToRelationshipEndArray();
        ISimpleRelationship[] simpleRelationshipsFrom = iClass.toFromRelationshipArray();
        ITemplateParameter[] templateParameters = iClass.toTemplateParameterArray();
        IClass[] innerClasses = iClass.toClassArray();
        Color defaultColor = iClassUIModel.getFillColor().getColor1();

        //attributes
        for (IAttribute attribute : attributes)
            if(!handleAttributeParsing(attribute, iClassUIModel, builder, classType, defaultColor)){
                context.setErrorFlag(true);
                return null;
            }

        //functions
        for (IOperation operation : operations)
            if(!handleFunctionParsing(operation, iClassUIModel, builder, classType, defaultColor)){
                context.setErrorFlag(true);
                return null;
            }

        //template
        if((templateParameters != null) && (classType != ClassType.ENUM)) {
            Template template = templateParser.parseTemplate(templateParameters, iClass);
            builder.setTemplate(template);

            //TODO builder.addImport(getClassImport(iClass, template.getTypeName()));
        }

        if(parentPath != null){
            //inner classes
            for(IClass innerClass: innerClasses){
                IClassUIModel uiModel = (IClassUIModel) getUIModelFromElement(innerClass, context);

                if(uiModel != null){
                    IDiagramElement parent = uiModel.getParent();

                    if
                    (
                            ((parent != null) && (parent.getModelElement().getAddress().startsWith(context.getPath())))
                                    ||
                                    ((parent == null) && (context.getPath().isEmpty()))
                    )
                    {
                        Struct parsedInnerClass = parseClass(uiModel, getElementPath(uiModel, context.getCodebase()));

                        if(parsedInnerClass == null)
                            context.setErrorFlag(true);
                        else
                            builder.addInnerClass(parsedInnerClass);

                        Logger.queueInfoMessage(iClass.getName() + " contains declaration of " + innerClass.getName());
                    }
                }
            }

            //attributes from relations
            for(IRelationshipEnd relationship: relationshipsFrom)
                if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_FROM_TO, context)){
                    context.setErrorFlag(true);
                    return null;
                }

            for(IRelationshipEnd relationship: relationshipsTo)
                if(!handleEndRelationshipParsing(relationship,iClassUIModel,builder,IAssociation.DIRECTION_TO_FROM, context)){
                    context.setErrorFlag(true);
                    return null;
                }

            //inheritance
            for(ISimpleRelationship relationship: simpleRelationshipsFrom){
                switch ( handleSimpleRelationshipParsing(relationship,iClass,builder,classType,iClassUIModel,defaultColor,hasExtend, context)){
                    case 1:
                        context.setErrorFlag(true);
                        return null;
                    case 2:
                        continue;
                    default:
                }
            }
        }

        return builder.build();
    }

    private Integer handleSimpleRelationshipParsing(ISimpleRelationship relationship, IClass iClass, Struct.Builder builder, ClassType classType, IClassUIModel iClassUIModel, Color defaultColor, AtomicBoolean hasExtend, CGContext context){
        if (relationship instanceof IGeneralization) {
            IModelElement to = relationship.getTo();

            if (!(to instanceof IClass) || !(to.getAddress().startsWith(context.getPath())))
                return 2; //should step on next iteration

            builder.addImport(getClassImport(iClass, to.getName(), context));

            if(to.hasStereotype("Interface")){
                Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + " implements " + to.getName());

                if(classType == ClassType.INTERFACE)
                    ((Interface.Builder) builder).addExtends(to.getName());
                else if (classType == ClassType.CLASS)
                    ((Class.Builder) builder).addImplements(to.getName());
                else
                    ((Enum.Builder) builder).addImplements(to.getName());

                for(IOperation function: ((IClass) to).toOperationArray()){
                    Function parsedFunction = functionParser.parseFunction(function, iClass, false);

                    if(parsedFunction != null){
                        parsedFunction.setOverride(true);

                        builder.addFunction(parsedFunction);
                    }
                    else{
                        ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(function,true);
                        attributeColor.setBackground(GUI.defaultError);

                        for(IParameter parameter :function.toParameterArray())
                            parameter.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));

                        function.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
                        return 1;
                    }
                }
            }
            else{
                if(classType == ClassType.CLASS){
                    if((!hasExtend.get())){
                        Logger.queueInfoMessage("Parsing " + iClass.getName() + "-> " + " extends " + to.getName());

                        Class extended = new Class.Builder("",null, to.getName()).build();

                        extended.setAttributes(parseExtendAttributes((IClass) to));

                        ((Class.Builder) builder).setExtends(extended);
                        hasExtend.set(true);
                    }
                    else {
                        GUI.showErrorParsingMessage(iClassUIModel,relationship, iClass.getName() + " cannot extends multiple classes");

                        return 1;
                    }
                }
                else{
                    GUI.showErrorParsingMessage(iClassUIModel,relationship, iClass.getName() + " cannot extends classes");

                    return 1;
                }
            }
        }

        return 0;
    }

    private Boolean handleFunctionParsing(IOperation operation, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor){
        IClass iClass = (IClass) iClassUIModel.getModelElement();
        Function parsedFunction = functionParser.parseFunction(operation, iClass, true);

        if(parsedFunction != null){
            if((classType == ClassType.INTERFACE))
                parsedFunction.setVirtual(true);

            builder.addFunction(parsedFunction);
        }
        else{
            ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(operation,true);
            attributeColor.setBackground(GUI.defaultError);

            for(IParameter parameter :operation.toParameterArray())
                parameter.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));

            operation.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
            return false;
        }

        return true;
    }

    private Boolean handleEndRelationshipParsing(IRelationshipEnd relationship, IClassUIModel iClassUIModel, Struct.Builder builder, int direction, CGContext context){
        IEndRelationship endRelationship = relationship.getEndRelationship();

        if(endRelationship instanceof IAssociation){
            if(getToModelElement(relationship, direction).getAddress().startsWith(context.getPath())){
                IClass iClass = (IClass) iClassUIModel.getModelElement();
                Attribute parsedAssociation = attributeParser.parseAssociation(relationship, iClass, direction);

                if (parsedAssociation != null)
                    builder.addAttribute(parsedAssociation);
                else{
                    iClassUIModel.setForeground(GUI.defaultError);
                    endRelationship.addPropertyChangeListener(evt -> iClassUIModel.setForeground(new Color(0,0,0)));
                    return false;
                }
            }
        }

        return true;
    }

    private Boolean handleAttributeParsing(IAttribute attribute, IClassUIModel iClassUIModel, Struct.Builder builder, ClassType classType, Color defaultColor){
        IClass iClass = (IClass) iClassUIModel.getModelElement();

        Attribute parsedAttribute = attributeParser.parseAttribute(attribute, iClass, true);

        if(parsedAttribute != null){
            if(classType != ClassType.ENUM)
                builder.addAttribute(parsedAttribute);
            else{
                if(FormatUtils.isCapsLock(parsedAttribute.getName()))
                    ((Enum.Builder)builder).addPairs(parsedAttribute.getName(), parsedAttribute.getInitializer());
                else
                    builder.addAttribute(parsedAttribute);
            }
        }
        else{
            ICompartmentColorModel attributeColor = iClassUIModel.getCompartmentColorModel(attribute,true);
            attributeColor.setBackground(GUI.defaultError);
            attribute.addPropertyChangeListener(evt -> attributeColor.setBackground(defaultColor));
            return false;
        }

        return true;
    }

    private ArrayList<Attribute> parseExtendAttributes(IClass aClass){
        ArrayList<Attribute> attributesList = new ArrayList<>();
        IAttribute[] attributes = aClass.toAttributeArray();

        for(IAttribute attribute: attributes)
            attributesList.add(attributeParser.parseAttribute(attribute, aClass, false));

        return attributesList;
    }
}