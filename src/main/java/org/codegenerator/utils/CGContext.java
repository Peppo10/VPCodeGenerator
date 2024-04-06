package org.codegenerator.utils;

import com.vp.plugin.action.VPContext;
import com.vp.plugin.diagram.IDiagramElement;
import org.codegenerator.parser.models.Codebase;

public class CGContext extends VPContext {
    Codebase codebase;
    private String path;
    private Boolean errorFlag;
    private String defaultPackage;

    public CGContext(VPContext vpContext, Codebase codebase, String path, Boolean errorFlag, String defaultPackage){
        super(vpContext.getContextType(), vpContext.getDiagram(), vpContext.getDiagramElement());

        this.codebase = codebase;
        this.path = path;
        this.errorFlag = errorFlag;
        this.defaultPackage = defaultPackage;
    }

    public Codebase getCodebase() {
        return codebase;
    }

    public void setCodebase(Codebase codebase) {
        this.codebase = codebase;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getErrorFlag() {
        return errorFlag;
    }

    public void setErrorFlag(Boolean errorFlag) {
        this.errorFlag = errorFlag;
    }

    public String getDefaultPackage() {
        return defaultPackage;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }
}
