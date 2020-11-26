/*
 * Copyright 2014 - 2020 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build.maven;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.BuildLogger;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.Plugin.Engine.Source.ForFolder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.utility.CompoundList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * A Maven plugin for applying Byte Buddy transformations during a build.
 */
public abstract class ByteBuddyMojo extends AbstractMojo {

    @Component
    public BuildContext buildContext;

    /**
     * The built project's group id.
     */
    @Parameter(defaultValue = "${project.groupId}", required = true, readonly = true)
    public String groupId;

    /**
     * The built project's artifact id.
     */
    @Parameter(defaultValue = "${project.artifactId}", required = true, readonly = true)
    public String artifactId;

    /**
     * The built project's version.
     */
    @Parameter(defaultValue = "${project.version}", required = true, readonly = true)
    public String version;

    /**
     * The built project's packaging.
     */
    @Parameter(defaultValue = "${project.packaging}", required = true, readonly = true)
    public String packaging;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    /**
     * <p>
     * The list of transformations. A transformation <b>must</b> specify the {@code plugin} property, containing the name of a class to apply.
     * Additionally, it is possible to optionally specify Maven coordinates for a project that contains this plugin class as {@code groupId},
     * {@code artifactId} and {@code version}. If any of the latter properties is not set, this projects coordinate is used.
     * </p>
     * <p>
     * For example, the following configuration applies the {@code foo.Bar} class which must implement {@link Plugin} from artifact
     * {@code transform-artifact} with this project's group and version:
     * </p>
     * <blockquote><pre>{@code
     * <transformations>
     *   <transformation>
     *     <plugin>foo.Bar< /plugin>
     *     <artifactId>transform-artifact< /artifactId>
     *   < /transformation>
     * < /transformations>
     * }</pre></blockquote>
     * <p>
     * If the list of {@code transformations} is empty or is not supplied at all, this plugin does not apply but prints a warning.
     * </p>
     */
    @Parameter
    public List<Transformation> transformations;

    /**
     * <p>
     * The initializer used for creating a {@link net.bytebuddy.ByteBuddy} instance and for applying a transformation. By default,
     * a type is rebased. The initializer's {@code entryPoint} property can be set to any constant name of {@link EntryPoint.Default}
     * or to a class name. If the latter applies, it is possible to set Maven coordinates for a Maven plugin which defines this
     * class where any property defaults to this project's coordinates.
     * </p>
     * <p>
     * For example, the following configuration applies the {@code foo.Qux} class which must implement {@link EntryPoint} from
     * artifact {@code initialization-artifact} with this project's group and version:
     * </p>
     * <blockquote><pre>{@code
     * <initialization>
     *   <entryPoint>foo.Qux< /entryPoint>
     *   <artifactId>initialization-artifact< /artifactId>
     * < /initialization>
     * }</pre></blockquote>
     */
    @Parameter
    public Initialization initialization;

    /**
     * Specifies the method name suffix that is used when type's method need to be rebased. If this property is not
     * set or is empty, a random suffix will be appended to any rebased method. If this property is set, the supplied
     * value is appended to the original method name.
     */
    @Parameter
    public String suffix;

    /**
     * When transforming classes during build time, it is not possible to apply any transformations which require a class
     * in its loaded state. Such transformations might imply setting a type's static field to a user interceptor or similar
     * transformations. If this property is set to {@code false}, this plugin does not throw an exception if such a live
     * initializer is defined during a transformation process.
     */
    @Parameter(defaultValue = "true", required = true)
    public boolean failOnLiveInitializer;

    /**
     * When set to {@code true}, this mojo is not applied to the current module.
     */
    @Parameter(defaultValue = "false", required = true)
    public boolean skip;

    /**
     * When set to {@code true}, this mojo warns of an non-existent output directory.
     */
    @Parameter(defaultValue = "true", required = true)
    public boolean warnOnMissingOutputDirectory;

    /**
     * When set to {@code true}, this mojo warns of not having transformed any types.
     */
    @Parameter(defaultValue = "true", required = true)
    public boolean warnOnEmptyTypeSet;

    /**
     * When set to {@code true}, this mojo fails immediately if a plugin cannot be applied.
     */
    @Parameter(defaultValue = "true", required = true)
    public boolean failFast;

    /**
     * When set to {@code true}, the debug information of class files should be parsed to extract parameter names.
     */
    @Parameter(defaultValue = "false", required = true)
    public boolean extendedParsing;

    /**
     * Indicates the amount of threads used for parallel type processing or {@code 0} for serial processing.
     */
    @Parameter(defaultValue = "0", required = true)
    public int threads;

    /**
     * The currently used repository system.
     */
    @Component
    public RepositorySystem repositorySystem;

    /**
     * The currently used system session for the repository system.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
    public RepositorySystemSession repositorySystemSession;

    /**
     * A list of all remote repositories.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", required = true, readonly = true)
    public List<RemoteRepository> remoteRepositories;

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Not applying instrumentation as a result of plugin configuration.");
            return;
        } else if (transformations == null || transformations.isEmpty()) {
            getLog().warn("No transformations are specified. Skipping plugin application.");
            return;
        }
        try {

            String sourceDirectory = getSourceDirectory();
            List<String> modifiedTypes = new ArrayList<String>();
            List<File> modifiedClassFiles = new ArrayList<File>();
            
            getLog().info("Build context: " + (buildContext == null ? "none" : buildContext.getClass().getName()));

            if (sourceDirectory != null && buildContext != null) {

                Scanner scanner = buildContext.newScanner(new File(sourceDirectory));
                scanner.scan();

                for (String file : scanner.getIncludedFiles()) {

                    // Keep logical path to be able to filter classes downstream
                    modifiedTypes.add(file.replace(".java", ""));

                    // Create file reference to class files to mark them as changed later
                    modifiedClassFiles.add(new File(getOutputDirectory(), file.replace(".java", ".class")));
                }

                if (buildContext.isIncremental()) {
                    getLog().info("Incrementally processing: " + modifiedTypes.toString());
                }
            }

            apply(new File(getOutputDirectory()), getClassPathElements(), modifiedTypes);

            if (buildContext != null) {
                for (File classFile : modifiedClassFiles) {
                    buildContext.refresh(classFile);
                }
            }

        } catch (IOException exception) {
            throw new MojoFailureException("Error during writing process", exception);
        }
    }

    /**
     * Returns the output directory to search for class files.
     *
     * @return The output directory to search for class files.
     */
    protected abstract String getOutputDirectory();
    
    protected abstract String getSourceDirectory();

    /**
     * Returns the class path elements of the relevant output directory.
     *
     * @return The class path elements of the relevant output directory.
     */
    protected abstract List<String> getClassPathElements();

    /**
     * Applies the instrumentation.
     *
     * @param root          The root folder that contains all class files.
     * @param classPath     An iterable over all class path elements.
     * @param modifiedFiles A list of logical file names (no .java or .class) so that the files to be precessed can be limited to those.
     * @throws MojoExecutionException If the plugin cannot be applied.
     * @throws IOException            If an I/O exception occurs.
     */
    @SuppressWarnings("unchecked")
    private void apply(File root, List<? extends String> classPath, final List<String> modifiedFiles) throws MojoExecutionException, IOException {
        if (!root.exists()) {
            if (warnOnMissingOutputDirectory) {
                getLog().warn("Skipping instrumentation due to missing directory: " + root);
            } else {
                getLog().info("Skipping instrumentation due to missing directory: " + root);
            }
            return;
        } else if (!root.isDirectory()) {
            throw new MojoExecutionException("Not a directory: " + root);
        }
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver(getLog(), repositorySystem, repositorySystemSession, remoteRepositories);
        try {
            List<Plugin.Factory> factories = new ArrayList<Plugin.Factory>(transformations.size());
            for (Transformation transformation : transformations) {
                String plugin = transformation.getPlugin();
                try {
                    factories.add(new Plugin.Factory.UsingReflection((Class<? extends Plugin>) Class.forName(plugin,
                            false,
                            classLoaderResolver.resolve(transformation.asCoordinate(groupId, artifactId, version, packaging))))
                            .with(transformation.makeArgumentResolvers())
                            .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File.class, root),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Log.class, getLog()),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, new MavenBuildLogger(getLog()))));
                    getLog().info("Resolved plugin: " + transformation.getRawPlugin());
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Cannot resolve plugin: " + transformation.getRawPlugin(), throwable);
                }
            }
            EntryPoint entryPoint = (initialization == null
                    ? Initialization.makeDefault()
                    : initialization).getEntryPoint(classLoaderResolver, groupId, artifactId, version, packaging);
            getLog().info("Resolved entry point: " + entryPoint);
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(classPath.size());
            for (String target : classPath) {
                File artifact = new File(target);
                classFileLocators.add(artifact.isFile()
                        ? ClassFileLocator.ForJarFile.of(artifact)
                        : new ClassFileLocator.ForFolder(artifact));
            }
            ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
            Plugin.Engine.Summary summary;
            try {
                getLog().info("Processing class files located in in: " + root);
                Plugin.Engine pluginEngine;
                try {
                    String javaVersionString = findJavaVersionString(project);
                    ClassFileVersion classFileVersion;
                    if (javaVersionString == null) {
                        classFileVersion = ClassFileVersion.ofThisVm();
                        getLog().warn("Could not locate Java target version, build is JDK dependant: " + classFileVersion.getMajorVersion());
                    } else {
                        classFileVersion = ClassFileVersion.ofJavaVersionString(javaVersionString);
                        getLog().debug("Java version detected: " + javaVersionString);
                    }
                    pluginEngine = Plugin.Engine.Default.of(entryPoint, classFileVersion, suffix == null || suffix.length() == 0
                            ? MethodNameTransformer.Suffixing.withRandomSuffix()
                            : new MethodNameTransformer.Suffixing(suffix));
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Cannot create plugin engine", throwable);
                }

                ForFolder classesSource = new FilteredFolder(root, modifiedFiles);

                try {
                    summary = pluginEngine
                            .with(extendedParsing
                                    ? Plugin.Engine.PoolStrategy.Default.EXTENDED
                                    : Plugin.Engine.PoolStrategy.Default.FAST)
                            .with(classFileLocator)
                            .with(new TransformationLogger(getLog()))
                            .withErrorHandlers(Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED, failOnLiveInitializer
                                    ? Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS
                                    : Plugin.Engine.Listener.NoOp.INSTANCE, failFast
                                    ? Plugin.Engine.ErrorHandler.Failing.FAIL_FAST
                                    : Plugin.Engine.Listener.NoOp.INSTANCE)
                            .with(threads == 0
                                    ? Plugin.Engine.Dispatcher.ForSerialTransformation.Factory.INSTANCE
                                    : new Plugin.Engine.Dispatcher.ForParallelTransformation.WithThrowawayExecutorService.Factory(threads))
                            .apply(classesSource, new Plugin.Engine.Target.ForFolder(root), factories);
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Failed to transform class files in " + root, throwable);
                }
            } finally {
                classFileLocator.close();
            }
            if (!summary.getFailed().isEmpty()) {
                throw new MojoExecutionException(summary.getFailed() + " type transformations have failed");
            } else if (warnOnEmptyTypeSet && summary.getTransformed().isEmpty()) {
                getLog().warn("No types were transformed during plugin execution");
            } else {
                getLog().info("Transformed " + summary.getTransformed().size() + " types");
            }
        } finally {
            classLoaderResolver.close();
        }
    }

    /**
     * Makes a best effort of locating the configured Java target version.
     *
     * @param project The relevant Maven project.
     * @return The Java version string of the configured build target version or {@code null} if no explicit configuration was detected.
     */
    private static String findJavaVersionString(MavenProject project) {
        while (project != null) {
            String target = project.getProperties().getProperty("maven.compiler.target");
            if (target != null) {
                return target;
            }
            for (org.apache.maven.model.Plugin plugin : CompoundList.of(project.getBuildPlugins(), project.getPluginManagement().getPlugins())) {
                if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                    if (plugin.getConfiguration() instanceof Xpp3Dom) {
                        Xpp3Dom node = ((Xpp3Dom) plugin.getConfiguration()).getChild("target");
                        if (node != null) {
                            return node.getValue();
                        }
                    }
                }
            }
            project = project.getParent();
        }
        return null;
    }

    /**
     * A {@link ForFolder} implementation that filters the resources returned to only expose ones with names of the
     * configured list.
     * 
     * @author Oliver Drotbohm
     */
    private static class FilteredFolder extends Plugin.Engine.Source.ForFolder {
        
        private final List<String> includes;

        private FilteredFolder(File folder, List<String> includes) {

            super(folder);
            this.includes = includes;
        }

        @Override
        public Iterator<Element> iterator() {

            Iterator<Element> source = super.iterator();

            if (includes.isEmpty()) {
                return source;
            }

            Set<Element> result = new HashSet<Element>();

            while (source.hasNext()) {
                Element element = source.next();
                for (String name : includes) {
                    if (element.getName().equals(name + ".class") || element.getName().startsWith(name + "$")) {
                        result.add(element);
                    }
                }
            }

            return result.iterator();
        }

        @Override
        public boolean equals(Object obj) {

            if(this == obj) {
                return true;
            }

            if (!(obj instanceof FilteredFolder)) {
                return false;
            }

            return super.equals(obj) && this.includes.equals(((FilteredFolder) obj).includes);
        }

        @Override
        public int hashCode() {
            return super.hashCode() + 17 * this.includes.hashCode();
        }
    }

    /**
     * A Byte Buddy plugin that transforms a project's production class files.
     */
    @Mojo(name = "transform",
            defaultPhase = LifecyclePhase.PROCESS_CLASSES,
            threadSafe = true,
            requiresDependencyResolution = ResolutionScope.COMPILE)
    public static class ForProductionTypes extends ByteBuddyMojo {

        /**
         * The current build's production output directory.
         */
        @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
        public String outputDirectory;

        /**
         * The production class path.
         */
        @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
        public List<String> compileClasspathElements;

        @Override
        protected String getOutputDirectory() {
            return outputDirectory;
        }
        
        /* 
        * (non-Javadoc)
        * @see net.bytebuddy.build.maven.ByteBuddyMojo#getSourceDirectory()
        */
        @Override
        protected String getSourceDirectory() {
            return project.getBuild().getSourceDirectory();
        }

        @Override
        protected List<String> getClassPathElements() {
            return compileClasspathElements;
        }
    }

    /**
     * A Byte Buddy plugin that transforms a project's test class files.
     */
    @Mojo(name = "transform-test",
            defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
            threadSafe = true,
            requiresDependencyResolution = ResolutionScope.TEST)
    public static class ForTestTypes extends ByteBuddyMojo {

        /**
         * The current build's test output directory.
         */
        @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true, readonly = true)
        public String testOutputDirectory;

        /**
         * The test class path.
         */
        @Parameter(defaultValue = "${project.testClasspathElements}", required = true, readonly = true)
        public List<String> testClasspathElements;

        @Override
        protected String getOutputDirectory() {
            return testOutputDirectory;
        }

        @Override
        protected String getSourceDirectory() {
            return project.getBuild().getTestSourceDirectory();
        }

        @Override
        protected List<String> getClassPathElements() {
            return testClasspathElements;
        }
    }

    /**
     * A {@link BuildLogger} implementation for a Maven {@link Log}.
     */
    protected static class MavenBuildLogger implements BuildLogger {

        /**
         * The logger to delegate to.
         */
        private final Log log;

        /**
         * Creates a new Maven build logger.
         *
         * @param log The logger to delegate to.
         */
        protected MavenBuildLogger(Log log) {
            this.log = log;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message) {
            log.debug(message);
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message, Throwable throwable) {
            log.debug(message, throwable);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInfoEnabled() {
            return log.isInfoEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message) {
            log.info(message);
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message, Throwable throwable) {
            log.info(message, throwable);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isWarnEnabled() {
            return log.isWarnEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message) {
            log.warn(message);
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message, Throwable throwable) {
            log.warn(message, throwable);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isErrorEnabled() {
            return log.isErrorEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message) {
            log.error(message);
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message, Throwable throwable) {
            log.error(message, throwable);
        }
    }

    /**
     * A {@link Plugin.Engine.Listener} that logs several relevant events during the build.
     */
    protected static class TransformationLogger extends Plugin.Engine.Listener.Adapter {

        /**
         * The logger to delegate to.
         */
        private final Log log;

        /**
         * Creates a new transformation logger.
         *
         * @param log The logger to delegate to.
         */
        protected TransformationLogger(Log log) {
            this.log = log;
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, List<Plugin> plugins) {
            log.debug("Transformed " + typeDescription + " using " + plugins);
        }

        @Override
        public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
            log.warn("Failed to transform " + typeDescription + " using " + plugin, throwable);
        }

        @Override
        public void onError(Map<TypeDescription, List<Throwable>> throwables) {
            log.warn("Failed to transform " + throwables.size() + " types");
        }

        @Override
        public void onError(Plugin plugin, Throwable throwable) {
            log.error("Failed to close " + plugin, throwable);
        }

        @Override
        public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
            log.debug("Discovered live initializer for " + definingType + " as a result of transforming " + typeDescription);
        }
    }
}
