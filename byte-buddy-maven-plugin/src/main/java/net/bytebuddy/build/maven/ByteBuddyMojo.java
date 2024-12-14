/*
 * Copyright 2014 - Present Rafael Winterhalter
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.BuildLogger;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.UnknownNull;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * A Maven plugin for applying Byte Buddy transformations during a build.
 */
public abstract class ByteBuddyMojo extends AbstractMojo {

    /**
     * The file extension for Java source files.
     */
    private static final String JAVA_FILE_EXTENSION = ".java";

    /**
     * The file extension for Java class files.
     */
    private static final String JAVA_CLASS_EXTENSION = ".class";

    /**
     * The Maven project.
     */
    @UnknownNull
    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    /**
     * The current execution of this plugin.
     */
    @UnknownNull
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    public MojoExecution execution;

    /**
     * The currently used repository system.
     */
    @UnknownNull
    @Component
    public RepositorySystem repositorySystem;

    /**
     * The currently used system session for the repository system.
     */
    @MaybeNull
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    public RepositorySystemSession repositorySystemSession;

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
    @MaybeNull
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
    @MaybeNull
    @Parameter
    public Initialization initialization;

    /**
     * Specifies the method name suffix that is used when type's method need to be rebased. If this property is not
     * set or is empty, a random suffix will be appended to any rebased method. If this property is set, the supplied
     * value is appended to the original method name.
     */
    @MaybeNull
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
     * Determines if the build should discover Byte Buddy build plugins on this Maven plugin's class loader.
     * Discovered plugins are stored by their name in the <i>/META-INF/net.bytebuddy/build.plugins</i> file
     * where each line contains the fully qualified class name. Discovered plugins are not provided with any
     * explicit constructor arguments.
     */
    @MaybeNull
    @Parameter(defaultValue = "EMPTY", required = true)
    public Discovery discovery;

    /**
     * Scans the class path (or test class path) for Byte Buddy plugins to apply. This is not normally recommended as
     * it might cause a spurious application of plugins that are accidentally configured on the class path. It can
     * however serve as a convenience in projects with few dependencies where this allows for the use of Maven's
     * dependency version management.
     */
    @Parameter(defaultValue = "false", required = true)
    public boolean classPathDiscovery;

    /**
     * Indicates the amount of threads used for parallel type processing or {@code 0} for serial processing.
     */
    @Parameter(defaultValue = "0", required = true)
    public int threads;

    /**
     * Determines the tolerance of many milliseconds between this plugin run and the last edit are permitted
     * for considering a file as stale if the plugin was executed before. Can be set to {@code -1} to disable.
     */
    @Parameter(defaultValue = "0", required = true)
    public int staleMilliseconds;

    /**
     * Defines the version to use for resolving multi-release jar files. If not set, the Java compile version is used.
     */
    @MaybeNull
    @Parameter
    public Integer multiReleaseVersion;

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "The security manager is not normally used within Maven.")
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project == null || repositorySystem == null || discovery == null) {
            throw new MojoExecutionException("Plugin is not initialized correctly");
        } else if (skip) {
            getLog().info("Not applying instrumentation as a result of plugin configuration.");
            return;
        }
        List<Transformer> transformers = new ArrayList<Transformer>();
        Set<String> undiscoverable = new HashSet<String>();
        if (transformations != null) {
            for (Transformation transformation : transformations) {
                transformers.add(new Transformer.ForConfiguredPlugin(transformation));
                if (discovery.isRecordConfiguration()) {
                    undiscoverable.add(transformation.getPlugin());
                }
            }
        }
        Map<Coordinate, String> coordinates = new HashMap<Coordinate, String>();
        if (project.getDependencyManagement() != null) {
            for (Dependency dependency : project.getDependencyManagement().getDependencies()) {
                coordinates.put(new Coordinate(dependency.getGroupId(), dependency.getArtifactId()), dependency.getVersion());
            }
        }
        List<String> elements = resolveClassPathElements(coordinates);
        if (discovery.isDiscover(transformers)) {
            try {
                for (String name : Plugin.Engine.Default.scan(ByteBuddyMojo.class.getClassLoader())) {
                    if (undiscoverable.add(name)) {
                        transformers.add(new Transformer.ForDiscoveredPlugin(name));
                        getLog().debug("Registered discovered plugin: " + name);
                    } else {
                        getLog().info("Skipping discovered plugin " + name + " which was previously discovered or registered");
                    }
                }
                if (classPathDiscovery) {
                    List<URL> urls = new ArrayList<URL>(elements.size());
                    for (String element : elements) {
                        urls.add(new File(element).toURI().toURL());
                    }
                    ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]));
                    try {
                        for (String name : Plugin.Engine.Default.scan(classLoader)) {
                            if (undiscoverable.add(name)) {
                                transformers.add(new Transformer.ForDiscoveredPlugin.FromClassLoader(name, elements));
                                getLog().debug("Registered discovered plugin: " + name);
                            } else {
                                getLog().info("Skipping discovered plugin " + name + " which was previously discovered or registered");
                            }
                        }
                    } finally {
                        if (classLoader instanceof Closeable) {
                            ((Closeable) classLoader).close();
                        }
                    }
                }
            } catch (IOException exception) {
                throw new MojoExecutionException("Failed plugin discovery", exception);
            }
        }
        if (transformers.isEmpty()) {
            getLog().warn("No transformations are specified or discovered. Skipping plugin application.");
            return;
        } else {
            getLog().debug(transformers.size() + " plugins are being applied via configuration and discovery");
        }
        try {
            apply(transformers, elements, coordinates);
        } catch (IOException exception) {
            throw new MojoFailureException("Error during writing process", exception);
        }
    }

    /**
     * Resolves the class path elements of the relevant output directory.
     *
     * @param coordinates Versions for managed dependencies.
     * @return The class path elements of the relevant output directory.
     * @throws MojoExecutionException If the user configuration results in an error.
     * @throws MojoFailureException   If the plugin application raises an error.
     */
    protected abstract List<String> resolveClassPathElements(Map<Coordinate, String> coordinates) throws MojoExecutionException, MojoFailureException;

    /**
     * Applies this mojo for the given setup.
     *
     * @param transformers The transformers to apply.
     * @param elements     The class path elements to consider.
     * @param coordinates  Versions for managed dependencies.
     * @throws MojoExecutionException If the plugin fails due to a user error.
     * @throws MojoFailureException   If the plugin fails due to an application error.
     * @throws IOException            If an I/O exception occurs.
     */
    protected abstract void apply(List<Transformer> transformers, List<String> elements, Map<Coordinate, String> coordinates) throws MojoExecutionException, MojoFailureException, IOException;

    /**
     * Applies the instrumentation.
     *
     * @param classPath    An iterable over all class path elements.
     * @param coordinates  Versions for managed dependencies.
     * @param transformers The transformers to apply.
     * @param source       The source for the plugin engine's application.
     * @param target       The target for the plugin engine's application.
     * @param file         The file representing the source location.
     * @param filtered     {@code true} if files are already filtered and should not be checked for staleness.
     * @return A summary of the applied transformation.
     * @throws MojoExecutionException If the plugin cannot be applied.
     * @throws IOException            If an I/O exception occurs.
     */
    @SuppressWarnings("unchecked")
    protected Plugin.Engine.Summary transform(List<? extends String> classPath,
                                              Map<Coordinate, String> coordinates,
                                              List<Transformer> transformers,
                                              Plugin.Engine.Source source,
                                              Plugin.Engine.Target target,
                                              File file,
                                              boolean filtered) throws MojoExecutionException, IOException {
        File staleness = new File(project.getBuild().getDirectory(), "maven-status"
                + File.separator + execution.getArtifactId()
                + File.separator + execution.getGoal()
                + File.separator + execution.getExecutionId()
                + File.separator + "staleness");
        StalenessFilter stalenessFilter;
        if (filtered || staleMilliseconds < 0) {
            stalenessFilter = null;
            getLog().debug("Stale file detection is disabled");
        } else if (staleness.exists()) {
            stalenessFilter = new StalenessFilter(getLog(), staleness.lastModified() + staleMilliseconds);
            source = new Plugin.Engine.Source.Filtering(source, stalenessFilter);
            getLog().debug("Using stale file detection with a margin of " + staleMilliseconds + " milliseconds");
        } else {
            stalenessFilter = null;
            getLog().debug("Did not discover previous staleness file");
        }
        List<File> artifacts = new ArrayList<File>(classPath.size());
        for (String element : classPath) {
            artifacts.add(new File(element));
        }
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver(getLog(), repositorySystem, repositorySystemSession == null ? MavenRepositorySystemUtils.newSession() : repositorySystemSession, project.getRemotePluginRepositories());
        try {
            List<Plugin.Factory> factories = new ArrayList<Plugin.Factory>(transformers.size());
            for (Transformer transformer : transformers) {
                String plugin = transformer.getPlugin();
                try {
                    factories.add(new Plugin.Factory.UsingReflection((Class<? extends Plugin>) Class.forName(plugin, false, transformer.toClassLoader(classLoaderResolver, coordinates, project.getGroupId(), project.getArtifactId(), project.getVersion(), project.getPackaging())))
                            .with(transformer.toArgumentResolvers())
                            .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File.class, file),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Log.class, getLog()),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, new MavenBuildLogger(getLog())),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File[].class, artifacts.toArray(new File[0]))));
                    getLog().info("Resolved plugin: " + plugin);
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Cannot resolve plugin: " + plugin, throwable);
                }
            }
            String managed = coordinates.get(new Coordinate(project.getGroupId(), project.getArtifactId()));
            EntryPoint entryPoint = (initialization == null ? new Initialization() : initialization).getEntryPoint(classLoaderResolver, project.getGroupId(), project.getArtifactId(), managed == null ? project.getVersion() : managed, project.getPackaging());
            getLog().info("Resolved entry point: " + entryPoint);
            String javaVersionString = findJavaVersionString(project, "release");
            if (javaVersionString == null) {
                javaVersionString = findJavaVersionString(project, "target");
            }
            ClassFileVersion classFileVersion;
            if (javaVersionString == null) {
                classFileVersion = ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V5);
                getLog().warn("Could not locate Java target version, build is JDK dependant: " + classFileVersion.getMajorVersion());
            } else {
                classFileVersion = ClassFileVersion.ofJavaVersionString(javaVersionString);
                getLog().debug("Java version detected: " + javaVersionString);
            }
            ClassFileVersion multiReleaseClassFileVersion = multiReleaseVersion == null
                    ? classFileVersion
                    : ClassFileVersion.ofJavaVersion(multiReleaseVersion);
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(artifacts.size());
            classFileLocators.add(ClassFileLocator.ForClassLoader.ofPlatformLoader());
            for (File artifact : artifacts) {
                classFileLocators.add(artifact.isFile()
                        ? ClassFileLocator.ForJarFile.of(artifact, multiReleaseClassFileVersion)
                        : ClassFileLocator.ForFolder.of(artifact, multiReleaseClassFileVersion));
            }
            ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
            Plugin.Engine.Summary summary;
            try {
                getLog().info("Processing class files located in in: " + file);
                Plugin.Engine pluginEngine;
                try {
                    pluginEngine = Plugin.Engine.Default.of(entryPoint,
                            classFileVersion,
                            suffix == null || suffix.length() == 0 ? MethodNameTransformer.Suffixing.withRandomSuffix() : new MethodNameTransformer.Suffixing(suffix));
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Cannot create plugin engine", throwable);
                }
                try {
                    summary = pluginEngine
                            .with(extendedParsing ? Plugin.Engine.PoolStrategy.Default.EXTENDED : Plugin.Engine.PoolStrategy.Default.FAST)
                            .with(classFileLocator)
                            .with(multiReleaseClassFileVersion)
                            .with(new TransformationLogger(getLog()))
                            .withErrorHandlers(Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED,
                                    failOnLiveInitializer ? Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS : Plugin.Engine.Listener.NoOp.INSTANCE,
                                    failFast ? Plugin.Engine.ErrorHandler.Failing.FAIL_FAST : Plugin.Engine.ErrorHandler.Failing.FAIL_LAST)
                            .with(threads == 0 ? Plugin.Engine.Dispatcher.ForSerialTransformation.Factory.INSTANCE : new Plugin.Engine.Dispatcher.ForParallelTransformation.WithThrowawayExecutorService.Factory(threads))
                            .apply(source, target, factories);
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Failed to transform class files in " + file, throwable);
                }
            } finally {
                classFileLocator.close();
            }
            if (!summary.getFailed().isEmpty()) {
                throw new MojoExecutionException(summary.getFailed() + " type transformation(s) have failed");
            } else if (warnOnEmptyTypeSet && summary.getTransformed().isEmpty()) {
                if (stalenessFilter != null && stalenessFilter.getFiltered() > 0) {
                    getLog().info("No types were transformed during plugin execution but " + stalenessFilter.getFiltered() + " class file(s) were considered stale");
                } else {
                    getLog().warn("No types were transformed during plugin execution");
                }
            } else {
                getLog().info("Transformed " + summary.getTransformed().size() + " type(s)");
            }
            if (!(staleness.getParentFile().isDirectory() || staleness.getParentFile().mkdirs()) || (!staleness.createNewFile() && (!staleness.delete() || !staleness.createNewFile()))) {
                throw new MojoExecutionException("Failed to define instrumentation staleness: " + staleness.getAbsolutePath());
            }
            return summary;
        } finally {
            classLoaderResolver.close();
        }
    }

    /**
     * Makes a best effort of locating the configured Java version.
     *
     * @param project  The relevant Maven project.
     * @param property The targeted Maven property.
     * @return The Java version string of the configured build Java version or {@code null} if no explicit configuration was detected.
     */
    @MaybeNull
    private static String findJavaVersionString(MavenProject project, String property) {
        do {
            String value = project.getProperties().getProperty("maven.compiler." + property);
            if (value != null) {
                return value;
            }
            PluginManagement management = project.getPluginManagement();
            for (org.apache.maven.model.Plugin plugin : management == null ? project.getBuildPlugins() : CompoundList.of(project.getBuildPlugins(), management.getPlugins())) {
                if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                    if (plugin.getConfiguration() instanceof Xpp3Dom) {
                        Xpp3Dom node = ((Xpp3Dom) plugin.getConfiguration()).getChild(property);
                        if (node != null) {
                            return node.getValue();
                        }
                    }
                }
            }
            project = project.getParent();
        } while (project != null);
        return null;
    }

    /**
     * Matches elements which represent a Java class that is represented in the list or an inner class of the classes represented in the list.
     */
    private static class FilePrefixMatcher extends ElementMatcher.Junction.ForNonNullValues<Plugin.Engine.Source.Element> {

        /**
         * A list of names to match.
         */
        private final List<String> names;

        /**
         * Create a new matcher for a list of names.
         *
         * @param names A list of included names.
         */
        private FilePrefixMatcher(List<String> names) {
            this.names = names;
        }

        /**
         * {@inheritDoc}
         */
        protected boolean doMatch(Plugin.Engine.Source.Element target) {
            for (String name : names) {
                if (target.getName().equals(name + JAVA_CLASS_EXTENSION) || target.getName().startsWith(name + "$") && target.getName().endsWith(JAVA_CLASS_EXTENSION)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A version of the plugin that is bound to Maven's lifecycle.
     */
    public abstract static class ForLifecycleTypes extends ByteBuddyMojo {

        /**
         * The build context to support incremental builds.
         */
        @MaybeNull
        @Component
        public BuildContext context;

        /**
         * Determines if plugins are attempted to be built incrementally.
         */
        @Parameter(defaultValue = "false", required = true)
        public boolean incremental;

        /**
         * Returns the output directory to search for class files.
         *
         * @return The output directory to search for class files.
         */
        protected abstract String getOutputDirectory();

        /**
         * Returns the source directory that determines the class files to process.
         *
         * @return The source directory that serves as an input for the transformation.
         */
        @MaybeNull
        protected abstract String getSourceDirectory();

        @Override
        protected void apply(List<Transformer> transformers, List<String> elements, Map<Coordinate, String> coordinates) throws MojoExecutionException, IOException {
            File root = new File(getOutputDirectory());
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
            String sourceDirectory = getSourceDirectory();
            if (incremental && context != null && sourceDirectory != null) {
                getLog().debug("Considering incremental build with context: " + context);
                Plugin.Engine.Source source;
                if (context.isIncremental()) {
                    Scanner scanner = context.newScanner(new File(sourceDirectory));
                    scanner.scan();
                    List<String> names = new ArrayList<String>();
                    for (String file : scanner.getIncludedFiles()) {
                        if (file.endsWith(JAVA_FILE_EXTENSION)) {
                            names.add(file.substring(0, file.length() - JAVA_FILE_EXTENSION.length()));
                        }
                    }
                    source = new Plugin.Engine.Source.Filtering(new Plugin.Engine.Source.ForFolder(root), new FilePrefixMatcher(names));
                    getLog().debug("Incrementally processing: " + names);
                } else {
                    source = new Plugin.Engine.Source.ForFolder(root);
                    getLog().debug("Cannot build incrementally - all class files are processed");
                }
                Plugin.Engine.Summary summary = transform(elements, coordinates, transformers, source, new Plugin.Engine.Target.ForFolder(root), root, true);
                for (TypeDescription typeDescription : summary.getTransformed()) {
                    context.refresh(new File(getOutputDirectory(), typeDescription.getName() + JAVA_CLASS_EXTENSION));
                }
            } else {
                getLog().debug("Not applying incremental build with context: " + context);
                transform(elements, coordinates, transformers, new Plugin.Engine.Source.ForFolder(root), new Plugin.Engine.Target.ForFolder(root), root, false);
            }
        }

        /**
         * A Byte Buddy plugin that transforms a project's production class files.
         */
        public abstract static class ForProductionTypes extends ForLifecycleTypes {

            @Override
            protected String getOutputDirectory() {
                return project.getBuild().getOutputDirectory();
            }

            @MaybeNull
            @Override
            protected String getSourceDirectory() {
                return project.getBuild().getSourceDirectory();
            }

            /**
             * A Byte Buddy plugin that transforms a project's production class files where runtime class
             * path elements are not included.
             */
            @Mojo(name = "transform", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
            public static class WithoutRuntimeDependencies extends ForProductionTypes {

                @Override
                protected List<String> resolveClassPathElements(Map<Coordinate, String> coordinates) throws MojoFailureException {
                    try {
                        return project.getCompileClasspathElements();
                    } catch (DependencyResolutionRequiredException exception) {
                        throw new MojoFailureException("Could not resolve class path", exception);
                    }
                }
            }

            /**
             * A Byte Buddy plugin that transforms a project's production class files where runtime class
             * path elements are included.
             */
            @Mojo(name = "transform-runtime", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
            public static class WithRuntimeDependencies extends ForProductionTypes {

                @Override
                protected List<String> resolveClassPathElements(Map<Coordinate, String> coordinates) {
                    try {
                        return project.getRuntimeClasspathElements();
                    } catch (DependencyResolutionRequiredException exception) {
                        throw new RuntimeException("Could not resolve runtime class path", exception);
                    }
                }
            }

            /**
             * A Byte Buddy plugin that transforms a project's production class files where all scopes but the test scope are included.
             */
            @Mojo(name = "transform-extended", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
            public static class WithExtendedDependencies extends ForProductionTypes {

                @Override
                protected List<String> resolveClassPathElements(Map<Coordinate, String> coordinates) {
                    List<String> classPath = new ArrayList<String>(project.getArtifacts().size() + 1);
                    String directory = project.getBuild().getOutputDirectory();
                    if (directory != null) {
                        classPath.add(directory);
                    }
                    for (Artifact artifact : project.getArtifacts()) {
                        if (artifact.getArtifactHandler().isAddedToClasspath()
                                && !Artifact.SCOPE_TEST.equals(artifact.getScope())
                                && !Artifact.SCOPE_IMPORT.equals(artifact.getScope())) {
                            File file = artifact.getFile();
                            if (file != null) {
                                classPath.add(file.getPath());
                            }
                        }
                    }
                    return classPath;
                }
            }
        }

        /**
         * A Byte Buddy plugin that transforms a project's test class files.
         */
        @Mojo(name = "transform-test", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
        public static class ForTestTypes extends ForLifecycleTypes {

            @Override
            protected String getOutputDirectory() {
                return project.getBuild().getTestOutputDirectory();
            }

            @MaybeNull
            @Override
            protected String getSourceDirectory() {
                return project.getBuild().getTestSourceDirectory();
            }

            @Override
            protected List<String> resolveClassPathElements(Map<Coordinate, String> coordinates) throws MojoFailureException {
                try {
                    return project.getTestClasspathElements();
                } catch (DependencyResolutionRequiredException exception) {
                    throw new MojoFailureException("Could not resolve test class path", exception);
                }
            }
        }
    }

    /**
     * Transforms specified classes from files in a folder or a jar file to a folder or jar file.
     */
    @Mojo(name = "transform-location-empty", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
    public static class ForExplicitLocations extends ByteBuddyMojo {

        /**
         * The source folder or jar file that should be transformed.
         */
        @UnknownNull
        @Parameter(required = true)
        public String source;

        /**
         * The target folder or jar file to where the transformed sources should be written to.
         */
        @UnknownNull
        @Parameter(required = true)
        public String target;

        /**
         * A list of dependencies to be included when resolving class files, additionally to the source jar.
         */
        @MaybeNull
        @Parameter
        public List<CoordinateConfiguration> dependencies;

        @Override
        protected List<String> resolveClassPathElements(Map<Coordinate, String> coordinates) throws MojoExecutionException, MojoFailureException {
            List<String> classPath = new ArrayList<String>();
            classPath.add(source);
            classPath.addAll(resolveImplicitClassPathElements());
            if (dependencies != null && !dependencies.isEmpty()) {
                RepositorySystemSession repositorySystemSession = this.repositorySystemSession == null ? MavenRepositorySystemUtils.newSession() : this.repositorySystemSession;
                for (CoordinateConfiguration dependency : dependencies) {
                    String managed = coordinates.get(new Coordinate(dependency.getGroupId(project.getGroupId()), dependency.getArtifactId(project.getArtifactId())));
                    MavenCoordinate mavenCoordinate = dependency.asCoordinate(project.getGroupId(),
                            project.getArtifactId(),
                            managed == null ? project.getVersion() : managed,
                            project.getPackaging());
                    try {
                        DependencyNode root = repositorySystem.collectDependencies(
                                repositorySystemSession,
                                new CollectRequest(new org.eclipse.aether.graph.Dependency(mavenCoordinate.asArtifact(), "runtime"), project.getRemotePluginRepositories())).getRoot();
                        repositorySystem.resolveDependencies(repositorySystemSession, new DependencyRequest().setRoot(root));
                        PreorderNodeListGenerator preorderNodeListGenerator = new PreorderNodeListGenerator();
                        root.accept(preorderNodeListGenerator);
                        for (org.eclipse.aether.artifact.Artifact artifact : preorderNodeListGenerator.getArtifacts(false)) {
                            classPath.add(artifact.getFile().toString());
                        }
                    } catch (DependencyCollectionException exception) {
                        throw new MojoExecutionException("Could not collect dependencies for " + mavenCoordinate, exception);
                    } catch (DependencyResolutionException exception) {
                        throw new MojoFailureException("Could not resolve dependencies for " + mavenCoordinate, exception);
                    }
                }
            }
            return classPath;
        }

        /**
         * Resolves any implicit dependencies that should be added to the class path.
         *
         * @return The class path elements of the relevant output directory.
         * @throws MojoFailureException If the class loader resolution yields a failure.
         */
        protected List<String> resolveImplicitClassPathElements() throws MojoFailureException {
            return Collections.emptyList();
        }

        @Override
        protected void apply(List<Transformer> transformers, List<String> elements, Map<Coordinate, String> coordinates) throws MojoExecutionException, MojoFailureException, IOException {
            File source = new File(this.source), target = new File(this.target);
            getLog().info("Transforming " + this.source + " to " + this.target);
            Plugin.Engine.Source resolved;
            if (source.isDirectory()) {
                resolved = new Plugin.Engine.Source.ForFolder(source);
            } else if (source.exists()) {
                resolved = new Plugin.Engine.Source.ForJarFile(source);
            } else {
                throw new MojoFailureException("Source location does not exist: " + source);
            }
            transform(elements,
                    coordinates,
                    transformers,
                    resolved,
                    target.isDirectory() ? new Plugin.Engine.Target.ForFolder(target) : new Plugin.Engine.Target.ForJarFile(target),
                    source,
                    false);
        }

        /**
         * Transforms specified classes from files in a folder or a jar file to a folder or jar file. Additionally, all class path dependencies
         * will be made visible during plugin application.
         */
        @Mojo(name = "transform-location", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
        public static class WithoutRuntimeDependencies extends ForExplicitLocations {

            @Override
            protected List<String> resolveImplicitClassPathElements() throws MojoFailureException {
                try {
                    return project.getCompileClasspathElements();
                } catch (DependencyResolutionRequiredException exception) {
                    throw new MojoFailureException("Could not resolve class path", exception);
                }
            }
        }

        /**
         * Transforms specified classes from files in a folder or a jar file to a folder or jar file. Additionally, all class path dependencies
         * will be made visible during plugin application, including runtime dependencies.
         */
        @Mojo(name = "transform-location-runtime", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
        public static class WithRuntimeDependencies extends ForExplicitLocations {

            @Override
            protected List<String> resolveImplicitClassPathElements() {
                try {
                    return project.getRuntimeClasspathElements();
                } catch (DependencyResolutionRequiredException exception) {
                    throw new RuntimeException("Could not resolve runtime class path", exception);
                }
            }
        }

        /**
         * Transforms specified classes from files in a folder or a jar file to a folder or jar file. Additionally, all class path dependencies
         * will be made visible during plugin application, including any non-test dependencies.
         */
        @Mojo(name = "transform-location-extended", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
        public static class WithExtendedDependencies extends ForExplicitLocations {

            @Override
            protected List<String> resolveImplicitClassPathElements() {
                List<String> classPath = new ArrayList<String>(project.getArtifacts().size() + 1);
                String directory = project.getBuild().getOutputDirectory();
                if (directory != null) {
                    classPath.add(directory);
                }
                for (Artifact artifact : project.getArtifacts()) {
                    if (artifact.getArtifactHandler().isAddedToClasspath()
                            && !Artifact.SCOPE_TEST.equals(artifact.getScope())
                            && !Artifact.SCOPE_IMPORT.equals(artifact.getScope())) {
                        File file = artifact.getFile();
                        if (file != null) {
                            classPath.add(file.getPath());
                        }
                    }
                }
                return classPath;
            }
        }

        /**
         * Transforms specified classes from files in a folder or a jar file to a folder or jar file. Additionally, all class path dependencies
         * will be made visible during plugin application, including any non-test and test dependencies.
         */
        @Mojo(name = "transform-location-test", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
        public static class ForTestTypes extends ForExplicitLocations {

            @Override
            protected List<String> resolveImplicitClassPathElements() throws MojoFailureException {
                try {
                    return project.getTestClasspathElements();
                } catch (DependencyResolutionRequiredException exception) {
                    throw new MojoFailureException("Could not resolve test class path", exception);
                }
            }
        }
    }

    /**
     * Transforms all jars for a folder containing jar files, typically project dependencies.
     */
    @Mojo(name = "transform-dependencies", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
    public static class ForDependencyFolder extends ByteBuddyMojo {

        /**
         * The source folder that contains the project's dependencies.
         */
        @UnknownNull
        @Parameter(required = true)
        public String source;

        /**
         * The target folder that contains the project's dependencies or {@code null} if the {@link ForDependencyFolder#source} folder should be used.
         */
        @MaybeNull
        @Parameter(required = false)
        public String target;

        @Override
        protected List<String> resolveClassPathElements(Map<Coordinate, String> coordinates) {
            try {
                return project.getCompileClasspathElements();
            } catch (DependencyResolutionRequiredException exception) {
                throw new RuntimeException("Could not resolve class path", exception);
            }
        }

        @Override
        protected void apply(List<Transformer> transformers, List<String> elements, Map<Coordinate, String> coordinates) throws MojoExecutionException, MojoFailureException, IOException {
            File source = new File(this.source), target = this.target == null ? source : new File(this.target);
            getLog().info("Transforming dependencies in " + this.source + (this.target == null ? "" : (" to " + this.target)));
            if (!source.isDirectory()) {
                throw new MojoFailureException("Expected " + this.source + " to be a folder");
            } else if (this.target != null && target.isFile()) {
                throw new MojoFailureException("Did not expect " + this.target + " to be a file");
            }
            File[] file = source.listFiles();
            if (file != null) {
                for (File aFile : file) {
                    if (aFile.isFile()) {
                        transform(elements,
                                coordinates,
                                transformers,
                                new Plugin.Engine.Source.ForJarFile(aFile),
                                new Plugin.Engine.Target.ForJarFile(new File(target, aFile.getName())),
                                aFile,
                                false);
                    }
                }
            }
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

    /**
     * A transformer that is applied during the plugin's execution.
     */
    protected abstract static class Transformer {

        /**
         * Returns the name of the plugin to apply.
         *
         * @return The name of the plugin to apply.
         * @throws MojoExecutionException If the plugin name was not set.
         */
        protected abstract String getPlugin() throws MojoExecutionException;

        /**
         * Returns the argument resolvers to use.
         *
         * @return The argument resolvers to use.
         */
        protected abstract List<? extends Plugin.Factory.UsingReflection.ArgumentResolver> toArgumentResolvers();

        /**
         * Resolves the class loader to use for resolving the plugin.
         *
         * @param classLoaderResolver The class loader resolver to use.
         * @param coordinates         The managed coordinates of this project.
         * @param groupId             The group id of this project.
         * @param artifactId          The artifact id of this project.
         * @param version             The version of this project.
         * @param packaging           The packaging of this project.
         * @return The class loader to use.
         * @throws MojoFailureException   If the class loader resolution yields a failure.
         * @throws MojoExecutionException The class loader resolution is incorrect.
         */
        protected abstract ClassLoader toClassLoader(ClassLoaderResolver classLoaderResolver, Map<Coordinate, String> coordinates, String groupId, String artifactId, String version, String packaging) throws MojoFailureException, MojoExecutionException;

        /**
         * A transformer for an explicitly configured plugin.
         */
        protected static class ForConfiguredPlugin extends Transformer {

            /**
             * The configured transformation.
             */
            private final Transformation transformation;

            /**
             * Creates a new transformer for an explicitly configured plugin.
             *
             * @param transformation The configured transformation.
             */
            protected ForConfiguredPlugin(Transformation transformation) {
                this.transformation = transformation;
            }

            @Override
            protected String getPlugin() throws MojoExecutionException {
                return transformation.getPlugin();
            }

            @Override
            protected List<? extends Plugin.Factory.UsingReflection.ArgumentResolver> toArgumentResolvers() {
                return transformation.makeArgumentResolvers();
            }

            @Override
            protected ClassLoader toClassLoader(ClassLoaderResolver classLoaderResolver, Map<Coordinate, String> coordinates, String groupId, String artifactId, String version, String packaging) throws MojoFailureException, MojoExecutionException {
                String managed = coordinates.get(new Coordinate(transformation.getGroupId(groupId), transformation.getArtifactId(artifactId)));
                return classLoaderResolver.resolve(transformation.asCoordinate(groupId, artifactId, managed == null ? version : managed, packaging));
            }
        }

        /**
         * A transformer for a discovered plugin.
         */
        protected static class ForDiscoveredPlugin extends Transformer {

            /**
             * The name of the discovered plugin.
             */
            private final String plugin;

            /**
             * Creates a new transformer for a discovered plugin.
             *
             * @param plugin The name of the discovered plugin.
             */
            protected ForDiscoveredPlugin(String plugin) {
                this.plugin = plugin;
            }

            @Override
            protected String getPlugin() {
                return plugin;
            }

            @Override
            protected List<? extends Plugin.Factory.UsingReflection.ArgumentResolver> toArgumentResolvers() {
                return Collections.emptyList();
            }

            @Override
            protected ClassLoader toClassLoader(ClassLoaderResolver classLoaderResolver, Map<Coordinate, String> coordinates, String groupId, String artifactId, String version, String packaging) {
                return ByteBuddyMojo.class.getClassLoader();
            }

            /**
             * A transformer for a discovered plugin from the class path.
             */
            protected static class FromClassLoader extends ForDiscoveredPlugin {

                /**
                 * The class path elements for loading this plugin.
                 */
                private final List<String> classPath;

                /**
                 * Creates a new transformer for a discovered plugin from the class path.
                 *
                 * @param plugin    The name of the discovered plugin.
                 * @param classPath The class path elements for loading this plugin.
                 */
                protected FromClassLoader(String plugin, List<String> classPath) {
                    super(plugin);
                    this.classPath = classPath;
                }

                @Override
                @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "The security manager is not normally used within Maven.")
                protected ClassLoader toClassLoader(ClassLoaderResolver classLoaderResolver, Map<Coordinate, String> coordinates, String groupId, String artifactId, String version, String packaging) {
                    URL[] url = new URL[classPath.size()];
                    for (int index = 0; index < classPath.size(); index++) {
                        try {
                            url[index] = new File(classPath.get(index)).toURI().toURL();
                        } catch (MalformedURLException exception) {
                            throw new IllegalStateException("Failed to resolve class path element to URL: " + classPath.get(index), exception);
                        }
                    }
                    return new URLClassLoader(url, ByteBuddyMojo.class.getClassLoader());
                }
            }
        }
    }

    /**
     * A coordinate to locate a managed dependency.
     */
    protected static class Coordinate {

        /**
         * The managed dependency's group id.
         */
        private final String groupId;

        /**
         * The managed dependency's artifact id.
         */
        private final String artifactId;

        /**
         * Creates a new coordinate for a managed dependency.
         *
         * @param groupId    The managed depencency's group id.
         * @param artifactId The managed depencency's artifact id.
         */
        protected Coordinate(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public int hashCode() {
            int result = groupId.hashCode();
            result = 31 * result + artifactId.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;

            Coordinate that = (Coordinate) other;

            if (!groupId.equals(that.groupId)) return false;
            return artifactId.equals(that.artifactId);
        }
    }

    /**
     * A filter for files that were written before a given timestamp, to avoid duplicate application.
     */
    protected static class StalenessFilter extends ElementMatcher.Junction.ForNonNullValues<Plugin.Engine.Source.Element> {

        /**
         * The logger to use.
         */
        private final Log log;

        /**
         * The timestamp for files to be filtered if they were created before it.
         */
        private final long latestTimestamp;

        /**
         * A count of class files that were filtered.
         */
        private int filtered;

        /**
         * Creates a new staleness filter.
         *
         * @param log             The logger to use.
         * @param latestTimestamp The timestamp for files to be filtered if they were created before it.
         */
        protected StalenessFilter(Log log, long latestTimestamp) {
            this.log = log;
            this.latestTimestamp = latestTimestamp;
        }

        /**
         * {@inheritDoc}
         */
        protected boolean doMatch(Plugin.Engine.Source.Element target) {
            File file = target.resolveAs(File.class);
            if (file == null) {
                throw new IllegalStateException("Expected " + target + " to resolve to a file");
            }
            if (file.lastModified() < latestTimestamp) {
                filtered += 1;
                log.debug("Filtering " + file + " due to staleness: " + file.lastModified());
                return false;
            } else {
                return true;
            }
        }

        /**
         * Returns a count of class files that were filtered as they were created prior to the last build.
         *
         * @return The amount of filtered classes.
         */
        protected int getFiltered() {
            return filtered;
        }
    }
}
