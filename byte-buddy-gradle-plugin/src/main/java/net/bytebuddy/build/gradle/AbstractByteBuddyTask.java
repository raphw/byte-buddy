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
import net.bytebuddy.build.gradle.api.Internal;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private EntryPoint entryPoint;

    /**
     * The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
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
     * The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    private int threads;

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
        transformations.add(ConfigureUtil.configure(closure, new Transformation()));
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
        List<Plugin.Factory> factories = new ArrayList<Plugin.Factory>(getTransformations().size());
        for (Transformation transformation : getTransformations()) {
            try {
                factories.add(new Plugin.Factory.UsingReflection(transformation.getPlugin())
                        .with(transformation.makeArgumentResolvers())
                        .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(File.class, source()),
                                Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Logger.class, getLogger()),
                                Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(BuildLogger.class, new GradleBuildLogger(getLogger()))));
                getLogger().info("Resolved plugin: {}", transformation.getPlugin().getName());
            } catch (Throwable throwable) {
                throw new IllegalStateException("Cannot resolve plugin: " + transformation.getPlugin().getName(), throwable);
            }
        }
        List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
        for (File artifact : classPath()) {
            classFileLocators.add(artifact.isFile()
                    ? ClassFileLocator.ForJarFile.of(artifact)
                    : new ClassFileLocator.ForFolder(artifact));
        }
        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(classFileLocators);
        Plugin.Engine.Summary summary;
        try {
            getLogger().info("Processing class files located in in: {}", source());
            Plugin.Engine pluginEngine;
            try {
                ClassFileVersion classFileVersion;
                JavaPluginConvention convention = (JavaPluginConvention) getProject().getConvention().getPlugins().get("java");
                if (convention == null) {
                    classFileVersion = ClassFileVersion.ofThisVm();
                    getLogger().warn("Could not locate Java target version, build is JDK dependant: {}", classFileVersion.getJavaVersion());
                } else {
                    classFileVersion = ClassFileVersion.ofJavaVersion(Integer.parseInt(convention.getTargetCompatibility().getMajorVersion()));
                    getLogger().debug("Java version detected: {}", classFileVersion.getJavaVersion());
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
                                : Plugin.Engine.Listener.NoOp.INSTANCE)
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
        if (!summary.getFailed().isEmpty()) {
            throw new IllegalStateException(summary.getFailed() + " type transformations have failed");
        } else if (isWarnOnEmptyTypeSet() && summary.getTransformed().isEmpty()) {
            getLogger().warn("No types were transformed during plugin execution");
        } else {
            getLogger().info("Transformed {} types", summary.getTransformed().size());
        }
    }

    /**
     * A {@link BuildLogger} implementation for a Gradle {@link Logger}.
     */
    protected static class GradleBuildLogger implements BuildLogger {

        /**
         * The logger to delegate to.
         */
        private final Logger logger;

        /**
         * Creates a new Gradle build logger.
         *
         * @param logger The logger to delegate to.
         */
        protected GradleBuildLogger(Logger logger) {
            this.logger = logger;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message) {
            logger.debug(message);
        }

        /**
         * {@inheritDoc}
         */
        public void debug(String message, Throwable throwable) {
            logger.debug(message, throwable);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message) {
            logger.info(message);
        }

        /**
         * {@inheritDoc}
         */
        public void info(String message, Throwable throwable) {
            logger.info(message, throwable);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message) {
            logger.warn(message);
        }

        /**
         * {@inheritDoc}
         */
        public void warn(String message, Throwable throwable) {
            logger.warn(message, throwable);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message) {
            logger.error(message);
        }

        /**
         * {@inheritDoc}
         */
        public void error(String message, Throwable throwable) {
            logger.error(message, throwable);
        }
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
}

