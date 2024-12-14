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
package net.bytebuddy.build.gradle;

import groovy.lang.Closure;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.BuildLogger;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.UnknownNull;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * An abstract Byte Buddy task implementation.
 */
public abstract class AbstractByteBuddyTask extends DefaultTask {

    /**
     * The transformations to apply.
     */
    private final List<Transformation> transformations;

    /**
     * The entry point to use.
     */
    @UnknownNull
    private EntryPoint entryPoint;

    /**
     * The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
    @UnknownNull
    private String suffix;

    /**
     * {@code true} if the transformation should fail if a live initializer is used.
     */
    private boolean failOnLiveInitializer;

    /**
     * {@code true} if a warning should be issued for an empty type set.
     */
    private boolean warnOnEmptyTypeSet;

    /**
     * {@code true} if the transformation should fail fast.
     */
    private boolean failFast;

    /**
     * {@code true} if extended parsing should be used.
     */
    private boolean extendedParsing;

    /**
     * Determines if the build should discover Byte Buddy build plugins on this Maven plugin's class loader.
     * Discovered plugins are stored by their name in the <i>/META-INF/net.bytebuddy/build.plugins</i> file
     * where each line contains the fully qualified class name. Discovered plugins are not provided with any
     * explicit constructor arguments.
     */
    @UnknownNull
    private Discovery discovery;

    /**
     * The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    private int threads;

    /**
     * The class file version to use for creating auxiliary types or {@code null} if the
     * version is determined implicitly.
     */
    @MaybeNull
    private ClassFileVersion classFileVersion;

    /**
     * The class file version to use for resolving multi-release jar files or {@code null} if
     * {@link #classFileVersion} or the implicit version should be used.
     */
    @MaybeNull
    private ClassFileVersion multiReleaseClassFileVersion;

    /**
     * Creates a new abstract Byte Buddy task.
     */
    protected AbstractByteBuddyTask() {
        transformations = new ArrayList<Transformation>();
    }

    /**
     * Returns the transformations to apply.
     *
     * @return The transformations to apply.
     */
    @Nested
    public List<Transformation> getTransformations() {
        return transformations;
    }

    /**
     * Adds an additional transformation.
     *
     * @param closure The closure to configure the transformation.
     */
    public void transformation(Closure<Transformation> closure) {
        Transformation transformation = ObjectFactory.newInstance(getProject(), Transformation.class, getProject());
        transformations.add((Transformation) getProject().configure(transformation == null
                ? new Transformation(getProject())
                : transformation, closure));
    }

    /**
     * Adds an additional transformation.
     *
     * @param action The action to configure the transformation.
     */
    public void transformation(Action<Transformation> action) {
        Transformation transformation = ObjectFactory.newInstance(getProject(), Transformation.class, getProject());
        if (transformation == null) {
            transformation = new Transformation(getProject());
        }
        action.execute(transformation);
        transformations.add(transformation);
    }

    /**
     * Returns the entry point to use.
     *
     * @return The entry point to use.
     */
    @Input
    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    /**
     * Sets the entry point to use.
     *
     * @param entryPoint The entry point to use.
     */
    public void setEntryPoint(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    /**
     * Returns the suffix to use for rebased methods or the empty string if a random suffix should be used.
     *
     * @return The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
    @Input
    public String getSuffix() {
        return suffix;
    }

    /**
     * Sets the suffix to use for rebased methods or the empty string if a random suffix should be used.
     *
     * @param suffix The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Returns {@code true} if the transformation should fail if a live initializer is used.
     *
     * @return {@code true} if the transformation should fail if a live initializer is used.
     */
    @Internal
    public boolean isFailOnLiveInitializer() {
        return failOnLiveInitializer;
    }

    /**
     * Determines if the transformation should fail if a live initializer is used.
     *
     * @param failOnLiveInitializer {@code true} if the transformation should fail if a live initializer is used.
     */
    public void setFailOnLiveInitializer(boolean failOnLiveInitializer) {
        this.failOnLiveInitializer = failOnLiveInitializer;
    }

    /**
     * Returns {@code true} if a warning should be issued for an empty type set.
     *
     * @return {@code true} if a warning should be issued for an empty type set.
     */
    @Internal
    public boolean isWarnOnEmptyTypeSet() {
        return warnOnEmptyTypeSet;
    }

    /**
     * Returns {@code true} if a warning should be issued for an empty type set.
     *
     * @param warnOnEmptyTypeSet {@code true} if a warning should be issued for an empty type set.
     */
    public void setWarnOnEmptyTypeSet(boolean warnOnEmptyTypeSet) {
        this.warnOnEmptyTypeSet = warnOnEmptyTypeSet;
    }

    /**
     * Returns {@code true} if this task should fail fast.
     *
     * @return {@code true} if this task should fail fast.
     */
    @Internal
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Determines if this task should fail fast.
     *
     * @param failFast {@code true} if this task should fail fast.
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Returns {@code true} if extended parsing should be used.
     *
     * @return {@code true} if extended parsing should be used.
     */
    @Input
    public boolean isExtendedParsing() {
        return extendedParsing;
    }

    /**
     * Determines if extended parsing should be used.
     *
     * @param extendedParsing {@code true} if extended parsing should be used.
     */
    public void setExtendedParsing(boolean extendedParsing) {
        this.extendedParsing = extendedParsing;
    }

    /**
     * Determines the discovery for finding plugins on the class path.
     *
     * @return The discovery for finding plugins on the class path.
     */
    @Input
    public Discovery getDiscovery() {
        return discovery;
    }

    /**
     * Determines the discovery being used for finding plugins on the class path.
     *
     * @param discovery The discovery for finding plugins on the class path.
     */
    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    /**
     * Returns the number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     *
     * @return The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    @Internal
    public int getThreads() {
        return threads;
    }

    /**
     * Sets the number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     *
     * @param threads The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Returns the class file version to use for creating auxiliary types or {@code null} if the
     * version is determined implicitly.
     *
     * @return The class file version to use for creating auxiliary types.
     */
    @MaybeNull
    @Input
    @Optional
    public ClassFileVersion getClassFileVersion() {
        return classFileVersion;
    }

    /**
     * Sets the class file version to use for creating auxiliary types or {@code null} if the
     * version is determined implicitly.
     *
     * @param classFileVersion The class file version to use for creating auxiliary types.
     */
    public void setClassFileVersion(@MaybeNull ClassFileVersion classFileVersion) {
        this.classFileVersion = classFileVersion;
    }

    /**
     * Returns the class file version to use for resolving multi-release jar files or {@code null} if the
     * explicit or implicit class file version of this task should be used.
     *
     * @return The class file version to use for resolving multi-release jar files.
     */
    @MaybeNull
    @Input
    @Optional
    public ClassFileVersion getMultiReleaseClassFileVersion() {
        return multiReleaseClassFileVersion;
    }

    /**
     * Sets the class file version to use for resolving multi-release jar files.
     *
     * @param multiReleaseClassFileVersion The class file version to use for resolving multi-release jar files.
     */
    public void setMultiReleaseClassFileVersion(@MaybeNull ClassFileVersion multiReleaseClassFileVersion) {
        this.multiReleaseClassFileVersion = multiReleaseClassFileVersion;
    }

    /**
     * Returns the source file or folder.
     *
     * @return The source file or folder.
     */
    protected abstract File source();

    /**
     * Returns the target file or folder.
     *
     * @return The target file or folder.
     */
    protected abstract File target();

    /**
     * Returns the class path to supply to the plugin engine.
     *
     * @return The class path to supply to the plugin engine.
     */
    protected abstract Iterable<File> classPath();

    /**
     * Returns the discovery class path or {@code null} if not specified.
     *
     * @return The discovery class path or {@code null} if not specified.
     */
    @MaybeNull
    protected abstract Iterable<File> discoverySet();

    /**
     * Applies the transformation from a source to a target.
     *
     * @param source The plugin engine's source.
     * @param target The plugin engine's target.
     * @throws IOException If an I/O exception occurs.
     */
    protected void doApply(Plugin.Engine.Source source, Plugin.Engine.Target target) throws IOException {
        if (source().equals(target())) {
            throw new IllegalStateException("Source and target cannot be equal: " + source());
        }
        ClassFileVersion classFileVersion;
        if (this.classFileVersion == null) {
            classFileVersion = ClassFileVersion.ofThisVm();
            getLogger().warn("Could not locate Java target version, build is JDK dependant: {}", classFileVersion.getJavaVersion());
        } else {
            classFileVersion = this.classFileVersion;
            getLogger().debug("Java version was configured: {}", classFileVersion.getJavaVersion());
        }
        apply(getLogger(),
                getClass().getClassLoader(),
                new ArrayList<Transformation>(getTransformations()),
                getDiscovery(),
                ClassFileLocator.ForClassLoader.ofPlatformLoader(),
                classPath(),
                discoverySet(),
                getEntryPoint(),
                classFileVersion,
                multiReleaseClassFileVersion == null ? classFileVersion : multiReleaseClassFileVersion,
                Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File.class, source()),
                getSuffix(),
                getThreads(),
                isExtendedParsing(),
                isFailFast(),
                isFailOnLiveInitializer(),
                isWarnOnEmptyTypeSet(),
                source,
                target);
    }

    /**
     * Dispatches a Byte Buddy instrumentation Gradle task.
     *
     * @param logger                       The logger to use.
     * @param rootLoader                   The class loader that is used for searching types and applying plugins.
     * @param transformations              The transformations to apply.
     * @param discovery                    The discovery for plugins to use.
     * @param rootLocator                  The root class file locator.
     * @param artifacts                    The artifacts to include.
     * @param discoverySet                 The source set to discover plugins from or {@code null} if no source set is used.
     * @param entryPoint                   The entry point to use.
     * @param classFileVersion             The class file version to use.
     * @param multiReleaseClassFileVersion The class file version to use for resolving multi-release jars.
     * @param rootLocationResolver         An argument resolver for the root location of this build.
     * @param suffix                       The suffix to use for rebased methods or an empty string for using a random suffix.
     * @param threads                      The number of threads to use while instrumenting.
     * @param extendedParsing              {@code true} if extended parsing should be used.
     * @param failFast                     {@code true} if the build should fail fast.
     * @param failOnLiveInitializer        {@code true} if the build should fail upon discovering a live initializer.
     * @param warnOnEmptyTypeSet           {@code true} if a warning should be logged if no types are instrumented.
     * @param source                       The source to use for instrumenting.
     * @param target                       The target to use for instrumenting.
     * @throws IOException If an I/O error occurs.
     */
    public static void apply(Logger logger,
                             ClassLoader rootLoader,
                             List<Transformation> transformations,
                             Discovery discovery,
                             ClassFileLocator rootLocator,
                             Iterable<File> artifacts,
                             @MaybeNull Iterable<File> discoverySet,
                             EntryPoint entryPoint,
                             ClassFileVersion classFileVersion,
                             ClassFileVersion multiReleaseClassFileVersion,
                             Plugin.Factory.UsingReflection.ArgumentResolver rootLocationResolver,
                             String suffix,
                             int threads,
                             boolean extendedParsing,
                             boolean failFast,
                             boolean failOnLiveInitializer,
                             boolean warnOnEmptyTypeSet,
                             Plugin.Engine.Source source,
                             Plugin.Engine.Target target) throws IOException {
        Plugin.Engine.Summary summary;
        ClassLoader classLoader = ByteBuddySkippingUrlClassLoader.of(rootLoader, discoverySet);
        try {
            if (discovery.isDiscover(transformations)) {
                Set<String> undiscoverable = new HashSet<String>();
                if (discovery.isRecordConfiguration()) {
                    for (Transformation transformation : transformations) {
                        undiscoverable.add(transformation.toPluginName());
                    }
                }
                for (String name : Plugin.Engine.Default.scan(classLoader)) {
                    if (undiscoverable.add(name)) {
                        try {
                            @SuppressWarnings("unchecked")
                            Class<? extends Plugin> plugin = (Class<? extends Plugin>) Class.forName(name, false, classLoader);
                            Transformation transformation = new Transformation();
                            transformation.setPlugin(plugin);
                            transformations.add(transformation);
                        } catch (ClassNotFoundException exception) {
                            throw new IllegalStateException("Discovered plugin is not available: " + name, exception);
                        }
                        logger.debug("Registered discovered plugin: {}", name);
                    } else {
                        logger.info("Skipping discovered plugin {} which was previously discovered or registered", name);
                    }
                }
            }
            if (transformations.isEmpty()) {
                logger.warn("No transformations are specified or discovered. Application will be non-operational.");
            } else {
                logger.debug("{} plugins are being applied via configuration and discovery", transformations.size());
            }
            List<File> classPath = new ArrayList<File>();
            for (File file : artifacts) {
                classPath.add(file);
            }
            List<Plugin.Factory> factories = new ArrayList<Plugin.Factory>(transformations.size());
            for (Transformation transformation : transformations) {
                try {
                    factories.add(new Plugin.Factory.UsingReflection(transformation.toPlugin(classLoader))
                            .with(transformation.makeArgumentResolvers())
                            .with(rootLocationResolver,
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Logger.class, logger),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(org.slf4j.Logger.class, logger),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, new GradleBuildLogger(logger)),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File[].class, classPath.toArray(new File[0]))));
                    logger.info("Resolved plugin: {}", transformation.toPluginName());
                } catch (Throwable throwable) {
                    throw new IllegalStateException("Cannot resolve plugin: " + transformation.toPluginName(), throwable);
                }
            }
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            classFileLocators.add(rootLocator);
            for (File artifact : artifacts) {
                classFileLocators.add(artifact.isFile()
                        ? ClassFileLocator.ForJarFile.of(artifact, multiReleaseClassFileVersion)
                        : ClassFileLocator.ForFolder.of(artifact, multiReleaseClassFileVersion));
            }
            ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
            try {
                summary = Plugin.Engine.Default.of(entryPoint, classFileVersion, suffix.length() == 0
                                ? MethodNameTransformer.Suffixing.withRandomSuffix()
                                : new MethodNameTransformer.Suffixing(suffix))
                        .with(extendedParsing
                                ? Plugin.Engine.PoolStrategy.Default.EXTENDED
                                : Plugin.Engine.PoolStrategy.Default.FAST)
                        .with(classFileLocator)
                        .with(multiReleaseClassFileVersion)
                        .with(new TransformationLogger(logger))
                        .withErrorHandlers(Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED, failOnLiveInitializer
                                ? Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS
                                : Plugin.Engine.Listener.NoOp.INSTANCE, failFast
                                ? Plugin.Engine.ErrorHandler.Failing.FAIL_FAST
                                : Plugin.Engine.ErrorHandler.Failing.FAIL_LAST)
                        .with(threads == 0
                                ? Plugin.Engine.Dispatcher.ForSerialTransformation.Factory.INSTANCE
                                : new Plugin.Engine.Dispatcher.ForParallelTransformation.WithThrowawayExecutorService.Factory(threads))
                        .apply(source, target, factories);
            } finally {
                classFileLocator.close();
            }
        } finally {
            if (classLoader instanceof Closeable && classLoader instanceof ByteBuddySkippingUrlClassLoader) {
                ((Closeable) classLoader).close();
            }
        }
        if (!summary.getFailed().isEmpty()) {
            throw new IllegalStateException(summary.getFailed() + " type transformation(s) have failed");
        } else if (warnOnEmptyTypeSet && summary.getTransformed().isEmpty()) {
            logger.warn("No types were transformed during plugin execution");
        } else {
            logger.info("Transformed {} type(s)", summary.getTransformed().size());
        }
    }

    /**
     * Deletes a collection of files or a folders recursively.
     *
     * @param files The files or folders to delete.
     * @return {@code true} if any of the files or folders were deleted.
     */
    protected static boolean deleteRecursively(Iterable<File> files) {
        boolean deleted = false;
        for (File file : files) {
            deleted = deleteRecursively(file) || deleted;
        }
        return deleted;
    }

    /**
     * Deletes a file or a folder recursively.
     *
     * @param file The file or folder to delete.
     * @return {@code true} if the file or folders were deleted.
     */
    protected static boolean deleteRecursively(File file) {
        boolean deleted = false;
        Queue<File> queue = new LinkedList<File>();
        queue.add(file);
        while (!queue.isEmpty()) {
            File current = queue.remove();
            File[] child = current.listFiles();
            if (child == null || child.length == 0) {
                deleted = current.delete() || deleted;
            } else {
                queue.addAll(Arrays.asList(child));
                queue.add(current);
            }
        }
        return deleted;
    }

    /**
     * A {@link net.bytebuddy.build.Plugin.Engine.Listener} that logs several relevant events during the build.
     */
    protected static class TransformationLogger extends Plugin.Engine.Listener.Adapter {

        /**
         * The logger to delegate to.
         */
        private final Logger logger;

        /**
         * Creates a new transformation logger.
         *
         * @param logger The logger to delegate to.
         */
        protected TransformationLogger(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, List<Plugin> plugins) {
            logger.debug("Transformed {} using {}", typeDescription, plugins);
        }

        @Override
        public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
            logger.warn("Failed to transform {} using {}", typeDescription, plugin, throwable);
        }

        @Override
        public void onError(Map<TypeDescription, List<Throwable>> throwables) {
            logger.warn("Failed to transform {} types", throwables.size());
        }

        @Override
        public void onError(Plugin plugin, Throwable throwable) {
            logger.error("Failed to close {}", plugin, throwable);
        }

        @Override
        public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
            logger.debug("Discovered live initializer for {} as a result of transforming {}", definingType, typeDescription);
        }
    }

    /**
     * A class loader that resolves a source set while still loading Byte Buddy classes from the Gradle plugin.
     */
    protected static class ByteBuddySkippingUrlClassLoader extends URLClassLoader {

        /**
         * Creates a new class loader that skips Byte Buddy classes.
         *
         * @param parent The parent class loader.
         * @param url    The URLs of the source set.
         */
        protected ByteBuddySkippingUrlClassLoader(ClassLoader parent, URL[] url) {
            super(url, parent);
        }

        /**
         * Resolves a class loader.
         *
         * @param classLoader  The class loader of the Byte Buddy plugin.
         * @param discoverySet The source set to discover plugins from or {@code null} if no source set is used.
         * @return The resolved class loader.
         */
        protected static ClassLoader of(ClassLoader classLoader, @MaybeNull Iterable<File> discoverySet) {
            if (discoverySet == null) {
                return classLoader;
            }
            List<URL> urls = new ArrayList<URL>();
            for (File file : discoverySet) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException exception) {
                    throw new IllegalStateException(exception);
                }
            }
            return urls.isEmpty()
                    ? classLoader
                    : new ByteBuddySkippingUrlClassLoader(classLoader, urls.toArray(new URL[0]));
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("net.bytebuddy.")) {
                Class<?> type = getParent().loadClass(name);
                if (resolve) {
                    resolveClass(type);
                }
                return type;
            } else {
                return super.loadClass(name, resolve);
            }
        }
    }
}

