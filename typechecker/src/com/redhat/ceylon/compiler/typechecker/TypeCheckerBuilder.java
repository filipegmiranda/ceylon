package com.redhat.ceylon.compiler.typechecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.ceylon.CeylonUtils;
import com.redhat.ceylon.compiler.typechecker.analyzer.UnsupportedError;
import com.redhat.ceylon.compiler.typechecker.io.VFS;
import com.redhat.ceylon.compiler.typechecker.io.VirtualFile;
import com.redhat.ceylon.compiler.typechecker.io.cmr.impl.LeakingLogger;
import com.redhat.ceylon.compiler.typechecker.tree.Message;
import com.redhat.ceylon.compiler.typechecker.util.AssertionVisitor;
import com.redhat.ceylon.compiler.typechecker.util.ModuleManagerFactory;

/**
 * Build a TypeChecker using the builder pattern
 * <code>
 *     TypeChecker typeChecker =
 *     new TypeCheckerBuilder()
 *         .addSrcDirectory(srcDirectory)
 *         .getTypeChecker();
 * </code>
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TypeCheckerBuilder {
    private boolean verbose = false;
    private boolean statistics = false;
    private String encoding;
    private List<VirtualFile> srcDirectories = new ArrayList<VirtualFile>();
    private List<VirtualFile> srcFiles = null;
    private final VFS vfs;
    private boolean verifyDependencies = true;
    private AssertionVisitor assertionVisitor = new AssertionVisitor() {
        @Override
        protected boolean includeError(Message err, int phase) {
            return !(phase == 3 && err instanceof UnsupportedError);
        }
        
    };
    private ModuleManagerFactory moduleManagerFactory;
    private RepositoryManager repositoryManager;
    private List<String> moduleFilters = new ArrayList<String>();

    public TypeCheckerBuilder(VFS vfs) {
        this.vfs = vfs != null ? vfs : new VFS();
    }

    public TypeCheckerBuilder() {
        vfs = new VFS();
    }

    /**
     * Let's you add a directory or a file.
     * Directories are better as the type checker can extract the context like module name, package etc
     */
    public TypeCheckerBuilder addSrcDirectory(File srcDirectory) {
        return addSrcDirectory( vfs.getFromFile( srcDirectory ) );
    }

    /**
     * Let's you add a directory or a file.
     * Directories are better as the type checker can extract the context like module name, package etc
     */
    public TypeCheckerBuilder addSrcDirectory(VirtualFile srcDirectory) {
        srcDirectories.add( srcDirectory);
        return this;
    }

    public TypeCheckerBuilder setRepositoryManager(RepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
        return this;
    }

    public TypeCheckerBuilder setModuleFilters(List<String> moduleFilters){
        this.moduleFilters.clear();
        this.moduleFilters.addAll(moduleFilters);
        return this;
    }

    public TypeCheckerBuilder setSourceFiles(List<File> srcFiles){
        if (srcFiles != null) {
            this.srcFiles = new ArrayList<VirtualFile>();
            for (File src : srcFiles) {
                this.srcFiles.add(vfs.getFromFile(src));
            }
        } else {
            this.srcFiles = null;
        }
        return this;
    }
    
    /**
     * @deprecated this is bad and a temporary hack
     *
     * The problem (which is a temporary one) with this is that it will try to load required modules from the ceylon repo,
     * as source (such as the ceylon language module) and we want to load them using our javac process and from class files
     * (or source code to autocompile).
     * This is a temporary hack until we figure out how to do module loading properly in the compiler (including repos and stuff,
     * but really, later).
     *
     * @author Emmanuel Bernard <emmanuel@hibernate.org>
     * @author Stephane Epardaud <stephane.epardaud@gmail.com>
     *
     */
    public TypeCheckerBuilder skipDependenciesVerification() {
        this.verifyDependencies = false;
        return this;
    }

    public TypeCheckerBuilder assertionVisitor(AssertionVisitor visitor) {
        this.assertionVisitor = visitor;
        return this;
    }

    public TypeCheckerBuilder verbose(boolean isVerbose) {
        this.verbose = isVerbose;
        return this;
    }
    
    public TypeCheckerBuilder statistics(boolean statistics) {
        this.statistics = statistics;
        return this;
    }

    /**
     * Enables or disables output of the warning messages about unused declarations.
     *
     * @param usageWarnings error message.
     * @return type checker instance.
     */
    public TypeCheckerBuilder usageWarnings(boolean usageWarnings) {
        this.assertionVisitor.includeUsageWarnings(usageWarnings);
        return this;
    }

    public TypeCheckerBuilder moduleManagerFactory(ModuleManagerFactory moduleManagerFactory){
        this.moduleManagerFactory = moduleManagerFactory;
        return this;
    }

    public VFS getVFS(){
        return vfs;
    }

    public TypeCheckerBuilder encoding(String value) {
        encoding = value;
        return this;
    }

    public TypeChecker getTypeChecker() {
        if (repositoryManager == null) {
            repositoryManager = CeylonUtils.repoManager()
                    .logger(new LeakingLogger())
                    .buildManager();
        }
        return new TypeChecker(vfs, srcDirectories, repositoryManager, verifyDependencies, assertionVisitor,
                moduleManagerFactory, verbose, statistics, moduleFilters, srcFiles, encoding);
    }

}
