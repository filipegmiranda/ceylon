package com.redhat.ceylon.tools.war;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import com.redhat.ceylon.cmr.api.ModuleQuery;
import com.redhat.ceylon.cmr.ceylon.loader.ModuleGraph;
import com.redhat.ceylon.cmr.ceylon.loader.ModuleGraph.Module;
import com.redhat.ceylon.cmr.impl.IOUtils;
import com.redhat.ceylon.common.ModuleSpec;
import com.redhat.ceylon.common.ModuleUtil;
import com.redhat.ceylon.common.Versions;
import com.redhat.ceylon.common.tool.Argument;
import com.redhat.ceylon.common.tool.Description;
import com.redhat.ceylon.common.tool.Hidden;
import com.redhat.ceylon.common.tool.Option;
import com.redhat.ceylon.common.tool.OptionArgument;
import com.redhat.ceylon.common.tool.Summary;
import com.redhat.ceylon.common.tool.ToolUsageError;
import com.redhat.ceylon.model.cmr.ArtifactResult;
import com.redhat.ceylon.model.cmr.ModuleScope;
import com.redhat.ceylon.model.loader.JvmBackendUtil;
import com.redhat.ceylon.tools.moduleloading.ModuleLoadingTool;

@Summary("Generates a WAR file from a compiled `.car` file")
@Description("Generates a WAR file from the `.car` file of the "
        + "given `module-with-version`, "
        + "suitable for deploying to a standard Servlet container.\n\n"
        + "The version number is required since, in general, there "
        + "can be multiple versions available in the configured repositories.\n\n"
        + "The given module's `.car` file and those of its "
        + "transitive dependencies will be copied to the `WEB-INF/lib` of "
        + "the generated WAR file. Dependencies which are provided by "
        + "the application container "
        + "(and thus not required to be in `WEB-INF/lib`) can be "
        + "excluded using `--provided-module`.")
public class CeylonWarTool extends ModuleLoadingTool {

    private List<ModuleSpec> modules;
    private final List<EntrySpec> entrySpecs = new ArrayList<>();
    private final List<String> excludedModules = new ArrayList<>();
    private final List<String> providedModules = new ArrayList<>();
    private String out = null;
    private String name = null;
    private String resourceRoot;
    
    @Hidden
    @Option(longName="static-metamodel")
    @Description("Obsolete: Generate a static metamodel, skip the WarInitializer (always true).")
    public void setStaticMetamodel(boolean staticMetamodel) throws IOException {
        append("WARNING: --static-metamodel option no longer supported: enabled by default");
    }

    @Argument(argumentName="module", multiplicity="+")
    public void setModules(List<String> modules) {
        setModuleSpecs(ModuleSpec.parseEachList(modules));
    }
    
    public void setModuleSpecs(List<ModuleSpec> modules) {
        this.modules = modules;
    }

    @OptionArgument(shortName='o', argumentName="dir")
    @Description("Sets the output directory for the WAR file (default: .)")
    public void setOut(String out) {
        this.out = out;
    }
    
    @OptionArgument(shortName='n', argumentName="name")
    @Description("Sets name of the WAR file (default: moduleName-version.war)")
    public void setName(String name) {
        this.name = name;
    }
    
    @OptionArgument(shortName='R', argumentName="directory")
    @Description("Sets the special resource directory whose files will " +
            "end up in the root of the resulting WAR file (default: web-content).")
    public void setResourceRoot(String root) {
        this.resourceRoot = root;
    }
    
    @OptionArgument(argumentName="moduleOrFile", shortName='x')
    @Description("Excludes modules from the WAR file. Can be a module name or " + 
            "a file containing module names. Can be specified multiple times. Note that "+
            "this excludes the module from the WAR file, but if your modules require that "+
            "module to be present at runtime it will still be required and may cause your "+
            "application to fail to start if it is not provided at runtime.")
    public void setExcludeModule(List<String> exclusions) {
        for (String each : exclusions) {
            File xFile = new File(each);
            if (xFile.exists() && xFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(xFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.excludedModules.add(line);
                    }
                } catch (IOException e) {
                    throw new ToolUsageError(CeylonWarMessages.msg("exclude.file.failure", each), 
                            e);
                }
            } else {
                this.excludedModules.add(each);
            }
        }
    }

    @OptionArgument(argumentName="moduleOrFile", shortName='p')
    @Description("Excludes modules from the WAR file by treating them as provided. Can be a module name or " + 
            "a file containing module names. Can be specified multiple times. Note that "+
            "this excludes the module from the WAR file, but if your modules require that "+
            "module to be present at runtime it will still be required and may cause your "+
            "application to fail to start if it is not provided at runtime. The only difference "+
            "between this and `--exclude-module` is that provided modules are listed in the metamodel.")
    public void setProvidedModule(List<String> exclusions) {
        for (String each : exclusions) {
            File xFile = new File(each);
            if (xFile.exists() && xFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(xFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        this.providedModules.add(line);
                    }
                } catch (IOException e) {
                    throw new ToolUsageError(CeylonWarMessages.msg("exclude.file.failure", each), 
                            e);
                }
            } else {
                this.providedModules.add(each);
            }
        }
    }

    @Override
    protected boolean skipDependency(ArtifactResult dep) {
        // skip any provided dep
        return dep.moduleScope() == ModuleScope.PROVIDED;
    }
    
    @Override
    public void run() throws Exception {
        String moduleName = null;
        String moduleVersion = null;
        final Properties properties = new Properties();
        
        for (ModuleSpec module : modules) {
            String name = module.getName();
            String version = checkModuleVersionsOrShowSuggestions(
                    name,
                    module.isVersioned() ? module.getVersion() : null,
                    ModuleQuery.Type.JVM,
                    Versions.JVM_BINARY_MAJOR_VERSION,
                    Versions.JVM_BINARY_MINOR_VERSION,
                    null, null, // JS binary but don't care since JVM
                    null);
            if(version == null)
                continue;
            if(!loadModule(null, name, version))
                throw new ToolUsageError(CeylonWarMessages.msg("abort.missing.modules"));
            // save the first module
            if(moduleName == null){
                moduleName = name;
                moduleVersion = version;
            }
        }

        loader.resolve();

        List<ArtifactResult> staticMetamodelEntries = new ArrayList<>();
        addLibEntries(staticMetamodelEntries);
        
        addResources(entrySpecs);
        
        if (this.name == null) {
            this.name = moduleVersion != null && !moduleVersion.isEmpty() 
                    ? String.format("%s-%s.war", moduleName, moduleVersion)
                    : String.format("%s.war", moduleName);
            debug("default.name", this.name);
        }
        
        final File jarFile = getJarFile();
        writeJarFile(jarFile, staticMetamodelEntries);
        
        String descr = moduleVersion != null && !moduleVersion.isEmpty()
                ? moduleName + "/" + moduleVersion
                : moduleName;
        append(CeylonWarMessages.msg("archive.created", descr, jarFile.getAbsolutePath()));
        newline();
    }

    public File getJarFile() {
        return applyCwd(this.out == null ? new File(this.name) : new File(this.out, this.name));
    }

    @Override
    protected boolean shouldExclude(String moduleName, String version) {
        return super.shouldExclude(moduleName, version) ||
                this.excludedModules.contains(moduleName);
    }

    @Override
    protected boolean isProvided(String moduleName, String version) {
        return super.isProvided(moduleName, version)
                || this.providedModules.contains(moduleName);
    }
    
    protected void addSpec(EntrySpec spec) {
        debug("adding.entry", spec.name);
        this.entrySpecs.add(spec);
    }
    
    protected void debug(String key, Object... args) {
        if (this.verbose != null &&
                !this.verbose.equals("loader")) {
            try {
                append("Debug: ").append(CeylonWarMessages.msg(key, args)).newline();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /** 
     * Copies resources from the {@link #resourceRoot} to the WAR.
     */
    protected void addResources(List<EntrySpec> entries) throws MalformedURLException {
        final File root;
        if (this.resourceRoot == null) {
            File defaultRoot = applyCwd(new File("web-content"));
            if (!defaultRoot.exists()) {
                return;
            }
            root = defaultRoot;
        } else {
            root = applyCwd(new File(this.resourceRoot));
        }
        if (!root.exists()) {
            throw new ToolUsageError(CeylonWarMessages.msg("resourceRoot.missing", root.getAbsolutePath()));
        }
        if (!root.isDirectory()) {
            throw new ToolUsageError(CeylonWarMessages.msg("resourceRoot.nondir", root.getAbsolutePath()));
        }
        debug("adding.resources", root.getAbsolutePath());
        
        addResources(root, "", entries);
    }
    
    protected void addResources(File root, String prefix, List<EntrySpec> entries) throws MalformedURLException {
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                addResources(f, prefix + f.getName() + "/", entries);
            } else {
                addSpec(new URLEntrySpec(f.toURI().toURL(), prefix + f.getName()));
            }
        }
    }
    
    protected void addLibEntries(final List<ArtifactResult> staticMetamodelEntries) throws MalformedURLException { 
        final List<String> libs = new ArrayList<>();

        loader.visitModules(new ModuleGraph.Visitor() {
            @Override
            public void visit(Module module) {
                if(module.artifact != null){
                    File artifact = module.artifact.artifact();
                    try{
                        if(artifact != null){
                            staticMetamodelEntries.add(module.artifact);
                            
                            // write the metamodel, but not the jar, for provided modules
                            if(isProvided(module.name, module.version))
                                return;
                            
                            final String moduleName = module.name;

                            // use "-" for the version separator
                            // use ".jar" so they'll get loaded by the container classloader
                            String version = module.version;
                            String versionSuffix = version != null && !version.isEmpty()
                                    ? "-" + version
                                    : "";
                            final String name = ModuleUtil.moduleName(moduleName).replace(":", ".")
                                                    + versionSuffix + ".jar";
                            if (name.contains("/") || name.contains("\\") || name.length() == 0) {
                                throw new ToolUsageError(CeylonWarMessages.msg("module.name.illegal", name));
                            }

                            addSpec(new URLEntrySpec(artifact.toURI().toURL(),
                                            "WEB-INF/lib/" + name));
                            libs.add(name);
                        }
                    }catch(IOException x){
                        // lame
                        throw new RuntimeException(x);
                    }
                }
            }
        });
    }
    
    protected void writeJarFile(File jarFile, List<ArtifactResult> staticMetamodelEntries) throws IOException {
        try (JarOutputStream out = 
                new JarOutputStream(new 
                        BufferedOutputStream(new 
                                FileOutputStream(jarFile)))) {
            for (EntrySpec entry : entrySpecs) {
                entry.write(out);
            }
            // FIXME: this is not done properly
            Set<String> added = new HashSet<>();
            JvmBackendUtil.writeStaticMetamodel(out, added, staticMetamodelEntries, jdkProvider, 
                    new HashSet<>(providedModules));
        }
    }
    
    abstract class EntrySpec {
        EntrySpec(final String name) {
            this.name = name;
        }
        
        void write(final JarOutputStream out) throws IOException {
            out.putNextEntry(new ZipEntry(this.name));
            IOUtils.copyStream(openStream(), out, true, false);
        }
        
        abstract InputStream openStream() throws IOException;
        
        final protected String name; 
    }
    
    class URLEntrySpec extends EntrySpec {
        URLEntrySpec(final URL url, final String name) {
            super(name);
            this.url = url;
        }
        
        InputStream openStream() throws IOException {
            return this.url.openStream();
        }
        
        final private URL url;
    }
    
    class StringEntrySpec extends EntrySpec {
        StringEntrySpec(final String content, final String name) {
            super(name);
            this.content = content;
        }
        
        InputStream openStream() throws IOException {
            return new ByteArrayInputStream(this.content.getBytes());
        }
    
        final private String content;
    }

    class PropertiesEntrySpec extends EntrySpec {

        PropertiesEntrySpec(final Properties properties, final String name) {
            super(name);
            this.properties = properties;
        }
        
        void write(final JarOutputStream out) throws IOException {
            out.putNextEntry(new ZipEntry(this.name));
            this.properties.store(out, "");
        }
        
        InputStream openStream() throws IOException {
            //unused
            return null;
        }
        
        final private Properties properties;
    }
}
