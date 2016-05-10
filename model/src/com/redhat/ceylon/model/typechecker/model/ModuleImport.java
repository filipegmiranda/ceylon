package com.redhat.ceylon.model.typechecker.model;

import java.util.ArrayList;
import java.util.List;

import com.redhat.ceylon.common.Backend;
import com.redhat.ceylon.common.Backends;

/**
 * Describes data specific to module imports
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ModuleImport implements Annotated {
    private boolean optional;
    private boolean export;
    private String namespace;
    private Module module;
    private Backends nativeBackends;
    private List<Annotation> annotations = new ArrayList<Annotation>();
    private ModuleImport overridenModuleImport = null;

    public ModuleImport(String namespace, Module module, boolean optional, boolean export) {
        this(namespace, module, optional, export, (Backend)null);
    }

    public ModuleImport(String namespace, Module module, boolean optional, boolean export, Backend backend) {
        this(namespace, module, optional, export, backend != null ? backend.asSet() : Backends.ANY);
    }

    public ModuleImport(String namespace, Module module, boolean optional, boolean export, Backends backends) {
        this.namespace = namespace;
        this.module = module;
        this.optional = optional;
        this.export = export;
        this.nativeBackends = backends;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isExport() {
        return export;
    }

    public String getNamespace() {
        return namespace;
    }

    public Module getModule() {
        return module;
    }
    
    public boolean isNative() {
        return !getNativeBackends().none();
    }
    
    public Backends getNativeBackends() {
        return nativeBackends;
    }
    
    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public ModuleImport getOverridenModuleImport() {
        return overridenModuleImport;
    }

    public boolean override(ModuleImport moduleImportOverride) {
        if (overridenModuleImport == null
        		&& moduleImportOverride != null) {
            this.overridenModuleImport = new ModuleImport(namespace, module, optional, export, nativeBackends);
            namespace = moduleImportOverride.getNamespace();
            module = moduleImportOverride.getModule();
            optional = moduleImportOverride.isOptional();
            export = moduleImportOverride.isExport();
            nativeBackends = moduleImportOverride.getNativeBackends();
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (export) sb.append("shared ");
        if (optional) sb.append("optional ");
        if (!nativeBackends.none()) {
            sb.append("native(")
              .append(nativeBackends)
              .append(") ");
        }
        sb.append("import ");
        if (namespace != null) {
            sb.append("\"")
              .append(namespace)
              .append(":")
              .append(module.getNameAsString())
              .append("\"");
        } else {
            sb.append(module.getNameAsString());
        }
        sb.append(" \"")
          .append(module.getVersion())
          .append("\"");
        return sb.toString();
    }
}
