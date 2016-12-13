package net.bytebuddy.build.maven;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.pool.TypePool;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Maven plugin for applying Byte Buddy transformations during a build.
 */
public abstract class ByteBuddyMojo extends AbstractMojo {

    /**
     * The file extension of a Java class file.
     */
    private static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * The built project's group id.
     */
    @Parameter(defaultValue = "${project.groupId}", required = true, readonly = true)
    protected String groupId;

    /**
     * The built project's artifact id.
     */
    @Parameter(defaultValue = "${project.artifactId}", required = true, readonly = true)
    protected String artifactId;

    /**
     * The built project's version.
     */
    @Parameter(defaultValue = "${project.version}", required = true, readonly = true)
    protected String version;

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
    protected List<Transformation> transformations;

    /**
     * <p>
     * The initializer used for creating a {@link ByteBuddy} instance and for applying a transformation. By default, a type is
     * rebased. The initializer's {@code entryPoint} property can be set to any constant name of {@link EntryPoint.Default} or
     * to a class name. If the latter applies, it is possible to set Maven coordinates for a Maven plugin which defines this
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
    protected Initialization initialization;

    /**
     * Specifies the method name suffix that is used when type's method need to be rebased. If this property is not
     * set or is empty, a random suffix will be appended to any rebased method. If this property is set, the supplied
     * value is appended to the original method name.
     */
    @Parameter
    protected String suffix;

    /**
     * When transforming classes during build time, it is not possible to apply any transformations which require a class
     * in its loaded state. Such transformations might imply setting a type's static field to a user interceptor or similar
     * transformations. If this property is set to {@code false}, this plugin does not throw an exception if such a live
     * initializer is defined during a transformation process.
     */
    @Parameter(defaultValue = "true", required = true)
    protected boolean failOnLiveInitializer;

    /**
     * The currently used repository system.
     */
    @Component
    protected RepositorySystem repositorySystem;

    /**
     * The currently used system session for the repository system.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
    protected RepositorySystemSession repositorySystemSession;

    /**
     * A list of all remote repositories.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", required = true, readonly = true)
    protected List<RemoteRepository> remoteRepositories;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (transformations == null || transformations.isEmpty()) {
            getLog().warn("No transformations are specified. Skipping plugin application.");
            return;
        }
        ByteBuddyLogHandler byteBuddyLogHandler = ByteBuddyLogHandler.initialize(getLog());
        try {
            processOutputDirectory(new File(getOutputDirectory()), getClassPathElements());
        } catch (IOException exception) {
            throw new MojoFailureException("Error during writing process", exception);
        } finally {
            byteBuddyLogHandler.reset();
        }
    }

    /**
     * Returns the output directory to search for class files.
     *
     * @return The output directory to search for class files.
     */
    protected abstract String getOutputDirectory();

    /**
     * Returns the class path elements of the relevant output directory.
     *
     * @return The class path elements of the relevant output directory.
     */
    protected abstract List<String> getClassPathElements();

    /**
     * Processes all class files within the given directory.
     *
     * @param root      The root directory to process.
     * @param classPath A list of class path elements expected by the processed classes.
     * @throws MojoExecutionException If the user configuration results in an error.
     * @throws MojoFailureException   If the plugin application raises an error.
     * @throws IOException            If an I/O exception occurs.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Applies Maven exception wrapper")
    private void processOutputDirectory(File root, List<? extends String> classPath) throws MojoExecutionException, MojoFailureException, IOException {
        if (!root.isDirectory()) {
            throw new MojoExecutionException("Target location does not exist or is no directory: " + root);
        }
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver(getLog(), repositorySystem, repositorySystemSession, remoteRepositories);
        try {
            List<Plugin> plugins = new ArrayList<Plugin>(transformations.size());
            for (Transformation transformation : transformations) {
                String plugin = transformation.getPlugin();
                try {
                    plugins.add((Plugin) Class.forName(plugin, false, classLoaderResolver.resolve(transformation.asCoordinate(groupId, artifactId, version)))
                            .getDeclaredConstructor()
                            .newInstance());
                    getLog().info("Created plugin: " + plugin);
                } catch (Exception exception) {
                    throw new MojoExecutionException("Cannot create plugin: " + transformation.getRawPlugin(), exception);
                }
            }
            EntryPoint entryPoint = (initialization == null
                    ? Initialization.makeDefault()
                    : initialization).getEntryPoint(classLoaderResolver, groupId, artifactId, version);
            getLog().info("Resolved entry point: " + entryPoint);
            transform(root, entryPoint, classPath, plugins);
        } finally {
            classLoaderResolver.close();
        }
    }

    /**
     * Applies all registered transformations.
     *
     * @param root       The root directory to process.
     * @param entryPoint The transformation's entry point.
     * @param classPath  A list of class path elements expected by the processed classes.
     * @param plugins    The plugins to apply.
     * @throws MojoExecutionException If the user configuration results in an error.
     * @throws MojoFailureException   If the plugin application raises an error.
     * @throws IOException            If an I/O exception occurs.
     */
    private void transform(File root,
                           EntryPoint entryPoint,
                           List<? extends String> classPath,
                           List<Plugin> plugins) throws MojoExecutionException, MojoFailureException, IOException {
        List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(classPath.size() + 1);
        classFileLocators.add(new ClassFileLocator.ForFolder(root));
        for (String target : classPath) {
            File artifact = new File(target);
            classFileLocators.add(artifact.isFile()
                    ? ClassFileLocator.ForJarFile.of(artifact)
                    : new ClassFileLocator.ForFolder(artifact));
        }
        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
        try {
            TypePool typePool = new TypePool.Default.WithLazyResolution(new TypePool.CacheProvider.Simple(),
                    classFileLocator,
                    TypePool.Default.ReaderMode.FAST,
                    TypePool.ClassLoading.ofBootPath());
            getLog().info("Processing class files located in in: " + root);
            ByteBuddy byteBuddy;
            try {
                byteBuddy = entryPoint.getByteBuddy();
            } catch (Throwable throwable) {
                throw new MojoExecutionException("Cannot create Byte Buddy instance", throwable);
            }
            processDirectory(root,
                    root,
                    byteBuddy,
                    entryPoint,
                    suffix == null || suffix.isEmpty()
                            ? MethodNameTransformer.Suffixing.withRandomSuffix()
                            : new MethodNameTransformer.Suffixing(suffix),
                    classFileLocator,
                    typePool,
                    plugins);
        } finally {
            classFileLocator.close();
        }
    }

    /**
     * Processes a directory.
     *
     * @param root                  The root directory to process.
     * @param folder                The currently processed folder.
     * @param byteBuddy             The Byte Buddy instance to use.
     * @param entryPoint            The transformation's entry point.
     * @param methodNameTransformer The method name transformer to use.
     * @param classFileLocator      The class file locator to use.
     * @param typePool              The type pool to query for type descriptions.
     * @param plugins               The plugins to apply.
     * @throws MojoExecutionException If the user configuration results in an error.
     * @throws MojoFailureException   If the plugin application raises an error.
     */
    private void processDirectory(File root,
                                  File folder,
                                  ByteBuddy byteBuddy,
                                  EntryPoint entryPoint,
                                  MethodNameTransformer methodNameTransformer,
                                  ClassFileLocator classFileLocator,
                                  TypePool typePool,
                                  List<Plugin> plugins) throws MojoExecutionException, MojoFailureException {
        File[] file = folder.listFiles();
        if (file != null) {
            for (File aFile : file) {
                if (aFile.isDirectory()) {
                    processDirectory(root, aFile, byteBuddy, entryPoint, methodNameTransformer, classFileLocator, typePool, plugins);
                } else if (aFile.isFile() && aFile.getName().endsWith(CLASS_FILE_EXTENSION)) {
                    processClassFile(root,
                            root.toURI().relativize(aFile.toURI()).toString(),
                            byteBuddy,
                            entryPoint,
                            methodNameTransformer,
                            classFileLocator,
                            typePool,
                            plugins);
                } else {
                    getLog().debug("Skipping ignored file: " + aFile);
                }
            }
        }
    }

    /**
     * Processes a class file.
     *
     * @param root                  The root directory to process.
     * @param file                  The class file to process.
     * @param byteBuddy             The Byte Buddy instance to use.
     * @param entryPoint            The transformation's entry point.
     * @param methodNameTransformer The method name transformer to use.
     * @param classFileLocator      The class file locator to use.
     * @param typePool              The type pool to query for type descriptions.
     * @param plugins               The plugins to apply.
     * @throws MojoExecutionException If the user configuration results in an error.
     * @throws MojoFailureException   If the plugin application raises an error.
     */
    private void processClassFile(File root,
                                  String file,
                                  ByteBuddy byteBuddy,
                                  EntryPoint entryPoint,
                                  MethodNameTransformer methodNameTransformer,
                                  ClassFileLocator classFileLocator,
                                  TypePool typePool,
                                  List<Plugin> plugins) throws MojoExecutionException, MojoFailureException {
        String typeName = file.replace(File.separatorChar, '.').substring(0, file.length() - CLASS_FILE_EXTENSION.length());
        getLog().debug("Processing class file: " + typeName);
        TypeDescription typeDescription = typePool.describe(typeName).resolve();
        DynamicType.Builder<?> builder;
        try {
            builder = entryPoint.transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer);
        } catch (Throwable throwable) {
            throw new MojoExecutionException("Cannot transform type: " + typeName, throwable);
        }
        boolean transformed = false;
        for (Plugin plugin : plugins) {
            try {
                if (plugin.matches(typeDescription)) {
                    builder = plugin.apply(builder, typeDescription);
                    transformed = true;
                }
            } catch (Throwable throwable) {
                throw new MojoExecutionException("Cannot apply " + plugin + " on " + typeName, throwable);
            }
        }
        if (transformed) {
            getLog().info("Transformed type: " + typeName);
            DynamicType dynamicType = builder.make();
            for (Map.Entry<TypeDescription, LoadedTypeInitializer> entry : dynamicType.getLoadedTypeInitializers().entrySet()) {
                if (failOnLiveInitializer && entry.getValue().isAlive()) {
                    throw new MojoExecutionException("Cannot apply live initializer for " + entry.getKey());
                }
            }
            try {
                dynamicType.saveIn(root);
            } catch (IOException exception) {
                throw new MojoFailureException("Cannot save " + typeName + " in " + root, exception);
            }
        } else {
            getLog().debug("Skipping non-transformed type: " + typeName);
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
        protected String outputDirectory;

        /**
         * The production class path.
         */
        @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
        protected List<String> compileClasspathElements;

        @Override
        protected String getOutputDirectory() {
            return outputDirectory;
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
        protected String testOutputDirectory;

        /**
         * The test class path.
         */
        @Parameter(defaultValue = "${project.testClasspathElements}", required = true, readonly = true)
        protected List<String> testClasspathElements;

        @Override
        protected String getOutputDirectory() {
            return testOutputDirectory;
        }

        @Override
        protected List<String> getClassPathElements() {
            return testClasspathElements;
        }
    }
}
