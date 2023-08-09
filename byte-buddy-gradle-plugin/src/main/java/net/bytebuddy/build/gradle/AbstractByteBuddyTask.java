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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
     * Returns {@code true} if a warning should be issued for an empty type set.
     *
     * @return {@code true} if a warning should be issued for an empty type set.
     */
    @Internal
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Determines if a warning should be issued for an empty type set.
     *
     * @param failFast {@code true} if a warning should be issued for an empty type set.
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
        List<Transformation> transformations = new ArrayList<Transformation>(getTransformations());
        ClassLoader classLoader = ByteBuddySkippingUrlClassLoader.of(getClass().getClassLoader(), discoverySet());
        Plugin.Engine.Summary summary;
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
                        getLogger().debug("Registered discovered plugin: {}", name);
                    } else {
                        getLogger().info("Skipping discovered plugin {} which was previously discovered or registered", name);
                    }
                }
            }
            if (transformations.isEmpty()) {
                getLogger().warn("No transformations are specified or discovered. Application will be non-operational.");
            } else {
                getLogger().debug("{} plugins are being applied via configuration and discovery", transformations.size());
            }
            List<Plugin.Factory> factories = new ArrayList<Plugin.Factory>(transformations.size());
            for (Transformation transformation : transformations) {
                try {
                    factories.add(new Plugin.Factory.UsingReflection(transformation.toPlugin(classLoader))
                            .with(transformation.makeArgumentResolvers())
                            .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File.class, source()),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Logger.class, getLogger()),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(org.slf4j.Logger.class, getLogger()),
                                    Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, new GradleBuildLogger(getLogger()))));
                    getLogger().info("Resolved plugin: {}", transformation.toPluginName());
                } catch (Throwable throwable) {
                    throw new IllegalStateException("Cannot resolve plugin: " + transformation.toPluginName(), throwable);
                }
            }
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            classFileLocators.add(ClassFileLocator.ForClassLoader.ofPlatformLoader());
            for (File artifact : classPath()) {
                classFileLocators.add(artifact.isFile()
                        ? ClassFileLocator.ForJarFile.of(artifact)
                        : new ClassFileLocator.ForFolder(artifact));
            }
            ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
            try {
                getLogger().info("Processing class files located in in: {}", source());
                Plugin.Engine pluginEngine;
                try {
                    ClassFileVersion classFileVersion;
                    if (this.classFileVersion == null) {
                        classFileVersion = ClassFileVersion.ofThisVm();
                        getLogger().warn("Could not locate Java target version, build is JDK dependant: {}", classFileVersion.getJavaVersion());
                    } else {
                        classFileVersion = this.classFileVersion;
                        getLogger().debug("Java version was configured: {}", classFileVersion.getJavaVersion());
                    }
                    pluginEngine = Plugin.Engine.Default.of(getEntryPoint(), classFileVersion, getSuffix().length() == 0
                            ? MethodNameTransformer.Suffixing.withRandomSuffix()
                            : new MethodNameTransformer.Suffixing(getSuffix()));
                } catch (Throwable throwable) {
                    throw new IllegalStateException("Cannot create plugin engine", throwable);
                }
                try {
                    summary = pluginEngine
                            .with(isExtendedParsing()
                                    ? Plugin.Engine.PoolStrategy.Default.EXTENDED
                                    : Plugin.Engine.PoolStrategy.Default.FAST)
                            .with(classFileLocator)
                            .with(new TransformationLogger(getLogger()))
                            .withErrorHandlers(Plugin.Engine.ErrorHandler.Enforcing.ALL_TYPES_RESOLVED, isFailOnLiveInitializer()
                                    ? Plugin.Engine.ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS
                                    : Plugin.Engine.Listener.NoOp.INSTANCE, isFailFast()
                                    ? Plugin.Engine.ErrorHandler.Failing.FAIL_FAST
                                    : Plugin.Engine.ErrorHandler.Failing.FAIL_LAST)
                            .with(getThreads() == 0
                                    ? Plugin.Engine.Dispatcher.ForSerialTransformation.Factory.INSTANCE
                                    : new Plugin.Engine.Dispatcher.ForParallelTransformation.WithThrowawayExecutorService.Factory(getThreads()))
                            .apply(source, target, factories);
                } catch (Throwable throwable) {
                    throw new IllegalStateException("Failed to transform class files in " + source(), throwable);
                }
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
        } else if (isWarnOnEmptyTypeSet() && summary.getTransformed().isEmpty()) {
            getLogger().warn("No types were transformed during plugin execution");
        } else {
            getLogger().info("Transformed {} type(s)", summary.getTransformed().size());
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
                } catch (MalformedURLException e) {
                    throw new IllegalStateException(e);
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

