package org.codegenerator.parser;
import com.vp.plugin.action.VPContext;
import com.vp.plugin.diagram.*;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.diagram.shape.IPackageUIModel;
import com.vp.plugin.model.*;
import org.codegenerator.logger.Logger;
import org.codegenerator.parser.models.*;
import org.codegenerator.parser.models.Package;
import org.codegenerator.utils.CGContext;
import org.codegenerator.utils.FormatUtils;
import org.codegenerator.utils.GUI;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import static org.codegenerator.utils.GUI.viewManager;

public class Parser {
    public static final String TAG = "Parser";
    private static Parser INSTANCE;
    public static String DEFAULT_PATH;
    CGContext context;
    ClassParser classParser;

    public static Parser getInstance(VPContext context, String choosePath){
        //TODO Config.UpdateProperty("actions.generate_code.default_path",choosePath);
        String name = context.getDiagram().getName();

        DEFAULT_PATH = choosePath;

        if(INSTANCE == null)
            INSTANCE = new Parser();

        INSTANCE.context = new CGContext(context,
                new Codebase(name,new ArrayList<>(), new ArrayList<>(), DEFAULT_PATH + File.separator + name),
                null,
                false,
                null);

        INSTANCE.classParser = new ClassParser(INSTANCE.context);
        
        return INSTANCE;
    }

    public Codebase getCodebase(){
        return this.context.getCodebase();
    }

    private Package parsePackage(IPackageUIModel iPackage, String parentPath){
        Package aPackage = new Package(iPackage.getModelElement().getName(), new ArrayList<>(), parentPath + File.separator + iPackage.getModelElement().getName());

        for(IShapeUIModel shapeUIModel: iPackage.toChildArray()){
            IModelElement iModelElement = shapeUIModel.getModelElement();

            if(iModelElement.getParent() instanceof IClass)
                continue;

            if(iModelElement instanceof IClass)
                aPackage.addFile(classParser.parseClass((IClassUIModel) shapeUIModel, aPackage.getPathname()));
            else if (iModelElement instanceof IPackage)
                context.getCodebase().addPackage(parsePackage((IPackageUIModel) shapeUIModel, aPackage.getPathname()));
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    private Package parseDefaultPackage(IPackage iPackage){
        this.context.setDefaultPackage(iPackage.getName());

        Package aPackage = new Package(iPackage.getName(), new ArrayList<>(), context.getCodebase().getPathname() + File.separator + iPackage.getName());

        for(IModelElement modelElement: iPackage.toChildArray()){
            if(modelElement instanceof IClass)
                aPackage.addFile(classParser.parseClass((IClassUIModel) getUIModelFromElement(modelElement, context), aPackage.getPathname()));
            else if (modelElement instanceof IPackage)
                context.getCodebase().addPackage(parsePackage((IPackageUIModel) getUIModelFromElement(modelElement, context), aPackage.getPathname()));
        }

        return aPackage.getFiles().contains(null) ? null : aPackage;
    }

    public void parseDiagram(IDiagramUIModel iDiagramUIModel){
        context.setPath("");
        int handled = 0;

        for(IShapeUIModel shapeUIModel: iDiagramUIModel.toShapeUIModelArray()){
            IModelElement modelElement = shapeUIModel.getModelElement();
            IModelElement parent = modelElement.getParent();

            if((parent instanceof IModel && (shapeUIModel.getParent() == null)) && (handled++ == 1))
                GUI.showWarningMessageDialog(viewManager.getRootFrame(), TAG, "Default package is not defined. Can cause errors with import");


            if(modelElement instanceof IPackage){
                if(parent instanceof IModel && (shapeUIModel.getParent() == null))
                    context.getCodebase().addPackage(parsePackage((IPackageUIModel) shapeUIModel, context.getCodebase().getPathname()));
                else if((parent instanceof IPackage) && (shapeUIModel.getParent() == null)) {
                    context.getCodebase().addPackage(parseDefaultPackage((IPackage) parent));
                    break;
                }
            }
            else if(modelElement instanceof IClass){
                if(parent instanceof IModel && (shapeUIModel.getParent() == null))
                    context.getCodebase().addFile(classParser.parseClass((IClassUIModel) shapeUIModel, context.getCodebase().getPathname()));
                else if((parent instanceof IPackage) && (shapeUIModel.getParent() == null)) {
                    context.getCodebase().addPackage(parseDefaultPackage((IPackage) parent));
                    break;
                }
            }
        }

        Logger.consumeQueue();
    }

    public void parseSingleClass(IClassUIModel iClass){
        context.setPath(iClass.getModelElement().getAddress());
        File aClass = classParser.parseClass(iClass, context.getCodebase().getPathname());

        context.getCodebase().addFile(aClass);

        Logger.consumeQueue();
    }
    public void parseSinglePackage(IPackageUIModel iPackage){
        context.setPath(iPackage.getModelElement().getAddress());
        Package aPackage = parsePackage(iPackage, context.getCodebase().getPathname());

        context.getCodebase().addPackage(aPackage);

        Logger.consumeQueue();
    }


    //utils
    private static Package parsePackageBottomUp(IPackageUIModel iPackage, Codebase codebase){
        IDiagramElement parent = iPackage.getParent();
        IModelElement modelParent = parent != null ? parent.getModelElement() : null;
        Package parentPackage = null;

        if(modelParent instanceof IPackage)
            parentPackage = parsePackageBottomUp((IPackageUIModel) parent, codebase);

        String optionalPackagePath = parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();

        return new Package(iPackage.getModelElement().getName(), new ArrayList<>(), optionalPackagePath + File.separator + iPackage.getModelElement().getName());
    }
    public static IModelElement getToModelElement(IRelationshipEnd relationshipEnd, int direction){
        if(direction == IAssociation.DIRECTION_FROM_TO){
            return relationshipEnd.getEndRelationship().getTo();
        }
        else if(direction == IAssociation.DIRECTION_TO_FROM){
            return relationshipEnd.getEndRelationship().getFrom();
        }
        else{
            throw new InvalidParameterException("direction parameter must be in range [0,1]");
        }
    }
    public static IModelElement getFromModelElement(IRelationshipEnd relationshipEnd, int direction){
        if(direction == IAssociation.DIRECTION_FROM_TO){
            return relationshipEnd.getEndRelationship().getFrom();
        }
        else if(direction == IAssociation.DIRECTION_TO_FROM){
            return relationshipEnd.getEndRelationship().getTo();
        }
        else{
            throw new InvalidParameterException("direction parameter must be in range [0,1]");
        }
    }
    public static String getClassImport(IClass importingClass, String classNameToImport, CGContext context){
        String javaLibraryImport = FormatUtils.listTypeJava.get(classNameToImport);

        if(javaLibraryImport != null)
            return javaLibraryImport + "." + classNameToImport;

        IClass formattedClass = (IClass) getModelFromName(classNameToImport, context);
        String importingClassPackage = getClassPackage(importingClass, context);
        String classToImportPackage = getClassPackage(formattedClass, context);

        if((classToImportPackage != null) && (importingClassPackage != null) && (classToImportPackage.compareTo(importingClassPackage) != 0))
            return classToImportPackage + "." + classNameToImport;

        return null;
    }
    public static String getElementPath(IShapeUIModel modelElement, Codebase codebase){
        IDiagramElement parent = modelElement.getParent();
        IModelElement parentModel = parent != null ? parent.getModelElement() : null;
        Package parentPackage = null;

        if(parentModel instanceof IPackage)
            parentPackage = parsePackageBottomUp((IPackageUIModel) parent, codebase);

        return parentPackage != null ? parentPackage.getPathname() : codebase.getPathname();
    }
    public static IShapeUIModel getUIModelFromElement(IModelElement element, CGContext context){
        for(IShapeUIModel shapeUIModel: context.getDiagram().toShapeUIModelArray())
            if (shapeUIModel.getModelElement().getId().compareTo(element.getId()) == 0)
                return shapeUIModel;

        return null;
    }
    public static IModelElement getModelFromName(String name, CGContext context){
        for(IShapeUIModel shapeUIModel: context.getDiagram().toShapeUIModelArray()) {
            IModelElement modelElement = shapeUIModel.getModelElement();

            if (modelElement.getName().compareTo(name) == 0)
                return modelElement;
        }

        return null;
    }
    public static String getNameFromAddress(String address, CGContext context){
        for(IShapeUIModel shapeUIModel: context.getDiagram().toShapeUIModelArray()) {
            IModelElement modelElement = shapeUIModel.getModelElement();

            if (modelElement.getId().compareTo(address) == 0)
                return modelElement.getName();
        }

        return null;
    }
    public static String getClassPackage(IClass iClass, CGContext context){
        StringBuilder stringBuilder = new StringBuilder();
        int begin, start;
        String contextAddress;

        if(iClass == null)
            return null;

        if(iClass.getAddress().compareTo(context.getPath()) == 0)
            return null;

        if(context.getPath().isEmpty()){
            contextAddress = iClass.getAddress();

            if(context.getDefaultPackage() != null) {
                stringBuilder.append(context.getDefaultPackage());

                if(iClass.getParent().getName().compareTo(context.getDefaultPackage()) != 0)
                    stringBuilder.append(".");

                start = 2;
            }
            else
                start = 1;

        }
        else {
            start = 0;
            begin = context.getPath().length() + 1;
            String[] contextPathParts = context.getPath().split(":");
            contextAddress = contextPathParts[contextPathParts.length-1] + ":" + iClass.getAddress().substring(begin);
        }

        String[] addressParts = contextAddress.split(":");

        for(int i=start; i < addressParts.length -1 ; i++) {
            stringBuilder.append(getNameFromAddress(addressParts[i], context));

            if (i < (addressParts.length - 2))
                stringBuilder.append(".");
        }

        return stringBuilder.toString();
    }
}