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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
     * The build context to support incremental builds.
     */
    @MaybeNull
    @Component
    public BuildContext context;

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
     * Determines if plugins are attempted to be built incrementally.
     */
    @Parameter(defaultValue = "false", required = true)
    public boolean incremental;

    /**
     * Determines the tolerance of many milliseconds between this plugin run and the last edit are permitted
     * for considering a file as stale if the plugin was executed before. Can be set to {@code -1} to disable.
     */
    @Parameter(defaultValue = "0", required = true)
    public int staleMilliseconds;

    /**
     * {@inheritDoc}
     */
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
        if (discovery.isDiscover(transformers)) {
            try {
                Enumeration<URL> plugins = ByteBuddyMojo.class.getClassLoader().getResources(Plugin.Engine.Default.PLUGIN_FILE);
                while (plugins.hasMoreElements()) {
                    discover(plugins.nextElement().openStream(), undiscoverable, transformers, null);
                }
                if (classPathDiscovery) {
                    List<String> elements = getClassPathElements();
                    for (String element : elements) {
                        File artifact = new File(element);
                        if (artifact.isFile()) {
                            JarFile file = new JarFile(artifact);
                            try {
                                JarEntry entry = file.getJarEntry(Plugin.Engine.Default.PLUGIN_FILE);
                                if (entry != null) {
                                    discover(file.getInputStream(entry), undiscoverable, transformers, elements);
                                }
                            } finally {
                                file.close();
                            }
                        } else {
                            File file = new File(artifact, Plugin.Engine.Default.PLUGIN_FILE);
                            if (file.exists()) {
                                discover(new FileInputStream(file), undiscoverable, transformers, elements);
                            }
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
                    source = new Plugin.Engine.Source.Filtering(new Plugin.Engine.Source.ForFolder(new File(getOutputDirectory())), new FilePrefixMatcher(names));
                    getLog().debug("Incrementally processing: " + names);
                } else {
                    source = new Plugin.Engine.Source.ForFolder(new File(getOutputDirectory()));
                    getLog().debug("Cannot build incrementally - all class files are processed");
                }
                Plugin.Engine.Summary summary = apply(new File(getOutputDirectory()), getClassPathElements(), transformers, source, true);
                for (TypeDescription typeDescription : summary.getTransformed()) {
                    context.refresh(new File(getOutputDirectory(), typeDescription.getName() + JAVA_CLASS_EXTENSION));
                }
            } else {
                getLog().debug("Not applying incremental build with context: " + context);
                apply(new File(getOutputDirectory()), getClassPathElements(), transformers, new Plugin.Engine.Source.ForFolder(new File(getOutputDirectory())), false);
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

    /**
     * Returns the source directory that determines the class files to process.
     *
     * @return The source directory that serves as an input for the transformation.
     */
    @MaybeNull
    protected abstract String getSourceDirectory();

    /**
     * Returns the class path elements of the relevant output directory.
     *
     * @return The class path elements of the relevant output directory.
     * @throws MojoFailureException If the class path cannot be resolved.
     */
    protected abstract List<String> getClassPathElements() throws MojoFailureException;

    /**
     * Applies the instrumentation.
     *
     * @param root         The root folder that contains all class files.
     * @param classPath    An iterable over all class path elements.
     * @param transformers The transformers to apply.
     * @param source       The source for the plugin's application.
     * @param filtered     {@code true} if files are already filtered and should not be checked for staleness.
     * @return A summary of the applied transformation.
     * @throws MojoExecutionException If the plugin cannot be applied.
     * @throws IOException            If an I/O exception occurs.
     */
    @SuppressWarnings("unchecked")
    private Plugin.Engine.Summary apply(File root,
                                        List<? extends String> classPath,
                                        List<Transformer> transformers,
                                        Plugin.Engine.Source source,
                                        boolean filtered) throws MojoExecutionException, IOException {
        if (!root.exists()) {
            if (warnOnMissingOutputDirectory) {
                getLog().warn("Skipping instrumentation due to missing directory: " + root);
            } else {
                getLog().info("Skipping instrumentation due to missing directory: " + root);
            }
            return new Plugin.Engine.Summary(Collections.<TypeDescription>emptyList(), Collections.<TypeDescription, List<Throwable>>emptyMap(), Collections.<String>emptyList());
        } else if (!root.isDirectory()) {
            throw new MojoExecutionException("Not a directory: " + root);
        }
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
        Map<Coordinate, String> coordinates = new HashMap<Coordinate, String>();
        if (project.getDependencyManagement() != null) {
            for (Dependency dependency : project.getDependencyManagement().getDependencies()) {
                coordinates.put(new Coordinate(dependency.getGroupId(), dependency.getArtifactId()), dependency.getVersion());
            }
        }
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver(getLog(), repositorySystem, repositorySystemSession == null ? MavenRepositorySystemUtils.newSession() : repositorySystemSession, project.getRemotePluginRepositories());
        try {
            List<Plugin.Factory> factories = new ArrayList<Plugin.Factory>(transformers.size());
            for (Transformer transformer : transformers) {
                String plugin = transformer.getPlugin();
                try {
                    factories.add(new Plugin.Factory.UsingReflection((Class<? extends Plugin>) Class.forName(plugin, false, transformer.toClassLoader(classLoaderResolver, coordinates, project.getGroupId(), project.getArtifactId(), project.getVersion(), project.getPackaging())))
                            .with(transformer.toArgumentResolvers())
                            .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File.class, root),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Log.class, getLog()),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, new MavenBuildLogger(getLog()))));
                    getLog().info("Resolved plugin: " + plugin);
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Cannot resolve plugin: " + plugin, throwable);
                }
            }
            String managed = coordinates.get(new Coordinate(project.getGroupId(), project.getArtifactId()));
            EntryPoint entryPoint = (initialization == null ? new Initialization() : initialization).getEntryPoint(classLoaderResolver, project.getGroupId(), project.getArtifactId(), managed == null ? project.getVersion() : managed, project.getPackaging());
            getLog().info("Resolved entry point: " + entryPoint);
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(classPath.size());
            classFileLocators.add(ClassFileLocator.ForClassLoader.ofPlatformLoader());
            for (String target : classPath) {
                File artifact = new File(target);
                classFileLocators.add(artifact.isFile() ? ClassFileLocator.ForJarFile.of(artifact) : new ClassFileLocator.ForFolder(artifact));
            }
            ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
            Plugin.Engine.Summary summary;
            try {
                getLog().info("Processing class files located in in: " + root);
                Plugin.Engine pluginEngine;
                try {
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
                    pluginEngine = Plugin.Engine.Default.of(entryPoint, classFileVersion, suffix == null || suffix.length() == 0 ? MethodNameTransformer.Suffixing.withRandomSuffix() : new MethodNameTransformer.Suffixing(suffix));
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Cannot create plugin engine", throwable);
                }
                try {
                    summary = pluginEngine.with(extendedParsing ? Plugin.Engine.PoolStrategy.Default.EXTENDED : Plugin.Engine.PoolStrategy.Default.FAST).with(classFileLocator).with(new TransformationLogger(getLog())).withErrorHandlers(Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED, failOnLiveInitializer ? Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS : Plugin.Engine.Listener.NoOp.INSTANCE, failFast ? Plugin.Engine.ErrorHandler.Failing.FAIL_FAST : Plugin.Engine.ErrorHandler.Failing.FAIL_LAST).with(threads == 0 ? Plugin.Engine.Dispatcher.ForSerialTransformation.Factory.INSTANCE : new Plugin.Engine.Dispatcher.ForParallelTransformation.WithThrowawayExecutorService.Factory(threads)).apply(source, new Plugin.Engine.Target.ForFolder(root), factories);
                } catch (Throwable throwable) {
                    throw new MojoExecutionException("Failed to transform class files in " + root, throwable);
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
     * Discovers plugins from an input stream representing a <i>META-INF/net.bytebuddy/build.plugins</i> file.
     *
     * @param inputStream    The input stream to read from.
     * @param undiscoverable A set of undiscoverable plugins.
     * @param transformers   The list of transformers to add discovered plugins to.
     * @param classPath      The class path elements to add if a plugin is loaded from the class path or {@code null} if the plugin is discovered as a dependency
     * @throws IOException If an I/O exception occurs.
     */
    private void discover(InputStream inputStream, Set<String> undiscoverable, List<Transformer> transformers, @MaybeNull List<String> classPath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (undiscoverable.add(line)) {
                    transformers.add(classPath == null
                            ? new Transformer.ForDiscoveredPlugin(line)
                            : new Transformer.ForDiscoveredPlugin.FromClassLoader(line, classPath));
                    getLog().debug("Registered discovered plugin: " + line);
                } else {
                    getLog().info("Skipping discovered plugin " + line + " which was previously discovered or registered");
                }
            }
        } finally {
            reader.close();
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
     * A Byte Buddy plugin that transforms a project's production class files.
     */
    public abstract static class ForProductionTypes extends ByteBuddyMojo {

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
            protected List<String> getClassPathElements() throws MojoFailureException {
                try {
                    return project.getCompileClasspathElements();
                } catch (DependencyResolutionRequiredException e) {
                    throw new MojoFailureException("Could not resolve class path", e);
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
            protected List<String> getClassPathElements() {
                try {
                    return project.getRuntimeClasspathElements();
                } catch (DependencyResolutionRequiredException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * A Byte Buddy plugin that transforms a project's production class files where all scopes but the test scope are included.
         */
        @Mojo(name = "transform-extended", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
        public static class WithExtendedDependencies extends ForProductionTypes {

            @Override
            protected List<String> getClassPathElements() {
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
    public static class ForTestTypes extends ByteBuddyMojo {

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
        protected List<String> getClassPathElements() throws MojoFailureException {
            try {
                return project.getTestClasspathElements();
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoFailureException("Could not resolve test class path", e);
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
         * @throws MojoExecutionException The the class loader resolution is incorrect.
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
                @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "The security manager is not normally used within Maven")
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
