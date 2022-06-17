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

import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.lang.reflect.Method;

/**
 * A plugin for applying Byte Buddy transformations to all standard Java source sets.
 */
public class ByteBuddyPlugin implements Plugin<Project> {

    /**
     * If set to {@code true}, the Byte Buddy plugin will be configured as if running on a legacy version of Gradle.
     */
    public static final String LEGACY = "net.bytebuddy.build.gradle.legacy";

    /**
     * The dispatcher to use.
     */
    private static final Dispatcher<?, ?> DISPATCHER;

    /*
     * Resolves the dispatcher for the current Gradle API version.
     */
    static {
        Dispatcher<?, ?> dispatcher;
        if (Boolean.getBoolean(LEGACY)) {
            dispatcher = Dispatcher.ForLegacyGradle.INSTANCE;
        } else {
            try {
                Class.forName("org.gradle.work.InputChanges"); // Make sure Gradle 6 is available.
                dispatcher = new Dispatcher.ForApi6CapableGradle(SourceDirectorySet.class.getMethod("getDestinationDirectory"),
                        AbstractCompile.class.getMethod("setDestinationDir", Class.forName("org.gradle.api.provider.Provider")));
            } catch (Exception ignored) {
                dispatcher = Dispatcher.ForLegacyGradle.INSTANCE;
            }
        }
        DISPATCHER = dispatcher;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(final Project project) {
        project.getLogger().debug("Applying Byte Buddy Gradle plugin (legacy mode: {})", DISPATCHER instanceof Dispatcher.ForLegacyGradle);
        project.getPlugins().withType(JavaPlugin.class, new JavaPluginConfigurationAction(project));
    }

    /**
     * An action to configure the Java plugin to apply transformations.
     */
    protected static class JavaPluginConfigurationAction implements Action<JavaPlugin> {

        /**
         * The Gradle project.
         */
        private final Project project;

        /**
         * Creates a Java plugin configuration action.
         *
         * @param project The Gradle project.
         */
        protected JavaPluginConfigurationAction(Project project) {
            this.project = project;
        }

        /**
         * {@inheritDoc}
         */
        public void execute(JavaPlugin plugin) {
            project.getLogger().debug("Java plugin was discovered for modification: {}", plugin);
            ConventionConfiguration configuration = ConventionConfiguration.of(project);
            if (configuration == null) {
                project.getLogger().warn("Skipping implicit Byte Buddy task configuration since Java plugin did not register Java plugin convention or extension");
            } else {
                for (SourceSet sourceSet : configuration.getSourceSets()) {
                    String name = sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME)
                            ? "byteBuddy"
                            : (sourceSet.getName() + "ByteBuddy");
                    AbstractByteBuddyTaskExtension<?> extension = project.getObjects().newInstance(DISPATCHER.toExtension());
                    extension.resolve(configuration.getTargetCompatibility());
                    project.getExtensions().add(name, extension);
                    project.afterEvaluate(DISPATCHER.toAction(name, sourceSet));
                }
            }
        }
    }

    /**
     * A dispatcher for creating Gradle integrations depending on the available API.
     *
     * @param <T> The Byte Buddy task type.
     * @param <S> The Byte Buddy extension type.
     */
    protected interface Dispatcher<T extends AbstractByteBuddyTask, S extends AbstractByteBuddyTaskExtension<T>> {

        /**
         * Creates a Byte Buddy extension instance.
         *
         * @return An appropriate Byte Buddy extension instance.
         */
        Class<S> toExtension();

        /**
         * Creates a Byte Buddy task configuration.
         *
         * @param name      The name of the task.
         * @param sourceSet The source set being configured.
         * @return An appropriate Byte Buddy task configuration.
         */
        AbstractByteBuddyTaskConfiguration<T, S> toAction(String name, SourceSet sourceSet);

        /**
         * A dispatcher for a legacy version of Gradle.
         */
        enum ForLegacyGradle implements Dispatcher<ByteBuddySimpleTask, ByteBuddySimpleTaskExtension> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Class<ByteBuddySimpleTaskExtension> toExtension() {
                return ByteBuddySimpleTaskExtension.class;
            }

            /**
             * {@inheritDoc}
             */
            public ByteBuddySimpleTaskConfiguration toAction(String name, SourceSet sourceSet) {
                return new ByteBuddySimpleTaskConfiguration(name, sourceSet);
            }
        }

        /**
         * A dispatcher for a Gradle version of at least 6.
         */
        class ForApi6CapableGradle implements Dispatcher<ByteBuddyTask, ByteBuddyTaskExtension> {

            /**
             * The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
             */
            private final Method getDestinationDirectory;

            /**
             * The {@code org.gradle.api.tasks.compile.AbstractCompile#setDestinationDir} method.
             */
            private final Method setDestinationDir;

            /**
             * Creates a new dispatcher for a Gradle version of at least 6.
             *
             * @param getDestinationDirectory The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
             * @param setDestinationDir       The {@code org.gradle.api.tasks.compile.AbstractCompile#setDestinationDir} method.
             */
            protected ForApi6CapableGradle(Method getDestinationDirectory, Method setDestinationDir) {
                this.getDestinationDirectory = getDestinationDirectory;
                this.setDestinationDir = setDestinationDir;
            }

            /**
             * {@inheritDoc}
             */
            public Class<ByteBuddyTaskExtension> toExtension() {
                return ByteBuddyTaskExtension.class;
            }

            /**
             * {@inheritDoc}
             */
            public ByteBuddyTaskConfiguration toAction(String name, SourceSet sourceSet) {
                return new ByteBuddyTaskConfiguration(name, sourceSet, getDestinationDirectory, setDestinationDir);
            }
        }
    }

    /**
     * Resolves the contextual configuration based on the project's Java plugin, if any.
     */
    protected static class ConventionConfiguration {

        /**
         * The {@code org.gradle.api.plugins.JavaPluginConvention} class or {@code null} if not available.
         */
        @MaybeNull
        private static final Class<?> JAVA_PLUGIN_CONVENTION;

        /**
         * The {@code org.gradle.api.Project#getConvention()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_CONVENTION;

        /**
         * The {@code org.gradle.api.plugins.JavaPluginConvention#getSourceSets()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_SOURCE_SETS;

        /**
         * The {@code org.gradle.api.plugins.JavaPluginConvention#getTargetCompatibility()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_TARGET_COMPATIBILITY;

        /*
         * Resolves the convention methods which might no longer be supported.
         */
        static {
            Class<?> javaPluginConvention;
            Method getConvention, getSourceSets, getTargetCompatibility;
            try {
                javaPluginConvention = Class.forName("org.gradle.api.plugins.JavaPluginConvention");
                getConvention = Project.class.getMethod("getConvention");
                getSourceSets = javaPluginConvention.getMethod("getSourceSets");
                getTargetCompatibility = javaPluginConvention.getMethod("getTargetCompatibility");
            } catch (Throwable ignored) {
                javaPluginConvention = null;
                getConvention = null;
                getSourceSets = null;
                getTargetCompatibility = null;
            }
            JAVA_PLUGIN_CONVENTION = javaPluginConvention;
            GET_CONVENTION = getConvention;
            GET_SOURCE_SETS = getSourceSets;
            GET_TARGET_COMPATIBILITY = getTargetCompatibility;
        }

        /**
         * The resolved source set container.
         */
        private final SourceSetContainer sourceSets;

        /**
         * The target Java version.
         */
        private final JavaVersion targetCompatibility;

        /**
         * Creates a new convention configuration.
         *
         * @param sourceSets          The resolved source set container.
         * @param targetCompatibility The target Java version.
         */
        protected ConventionConfiguration(SourceSetContainer sourceSets, JavaVersion targetCompatibility) {
            this.sourceSets = sourceSets;
            this.targetCompatibility = targetCompatibility;
        }

        /**
         * Resolves a convention configuration of the current project.
         *
         * @param project The current Gradle project.
         * @return The resolved convention configuration or {@code null} if not configured.
         */
        @MaybeNull
        protected static ConventionConfiguration of(Project project) {
            JavaPluginExtension extension = project.getExtensions().findByType(JavaPluginExtension.class);
            if (extension != null) {
                return new ConventionConfiguration(extension.getSourceSets(), extension.getTargetCompatibility());
            }
            if (JAVA_PLUGIN_CONVENTION != null && GET_CONVENTION != null && GET_SOURCE_SETS != null && GET_TARGET_COMPATIBILITY != null) {
                try {
                    Object convention = ((Convention) GET_CONVENTION.invoke(project)).findPlugin(JAVA_PLUGIN_CONVENTION);
                    if (convention != null) {
                        return new ConventionConfiguration(
                                (SourceSetContainer) GET_SOURCE_SETS.invoke(convention),
                                (JavaVersion) GET_TARGET_COMPATIBILITY.invoke(convention));
                    }
                } catch (Throwable ignored) {
                    /* do nothing */
                }
            }
            return null;
        }

        /**
         * Returns the resolved source set container.
         *
         * @return The resolved source set container.
         */
        protected SourceSetContainer getSourceSets() {
            return sourceSets;
        }

        /**
         * Returns the target Java version.
         *
         * @return The target Java version.
         */
        protected JavaVersion getTargetCompatibility() {
            return targetCompatibility;
        }
    }
}
