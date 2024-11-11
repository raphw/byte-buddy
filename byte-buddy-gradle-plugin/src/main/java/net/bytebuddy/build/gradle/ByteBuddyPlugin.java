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
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.lang.reflect.Method;

/**
 * A plugin for applying Byte Buddy transformations to all standard Java source sets.
 */
public class ByteBuddyPlugin implements Plugin<Project> {

    /**
     * The dispatcher to use.
     */
    private static final Dispatcher<?, ?> DISPATCHER;

    /*
     * Resolves the dispatcher for the current Gradle API version.
     */
    static {
        Dispatcher<?, ?> dispatcher;
        try {
            Class.forName("org.gradle.work.InputChanges"); // Make sure that at least Gradle 6 is available.
            dispatcher = new Dispatcher.ForApi6CapableGradle(
                    SourceDirectorySet.class.getMethod("getDestinationDirectory"),
                    AbstractCompile.class.getMethod("getDestinationDirectory"));
        } catch (Throwable ignored) {
            dispatcher = Dispatcher.ForLegacyGradle.INSTANCE;
        }
        DISPATCHER = dispatcher;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(Project project) {
        if (project.getExtensions().findByName("android") != null) {
            project.getLogger().debug("Applying Byte Buddy Android plugin");
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Plugin<Project>> plugin = (Class<? extends Plugin<Project>>) Class.forName("net.bytebuddy.build.gradle.android.ByteBuddyAndroidPlugin");
                project.getPlugins().apply(plugin);
            } catch (ClassNotFoundException exception) {
                project.getLogger().error("Failed to load Byte Buddy Android plugin", exception);
            }
        } else {
            project.getLogger().debug("Applying Byte Buddy plugin (legacy mode: {})", DISPATCHER instanceof Dispatcher.ForLegacyGradle);
            project.getPlugins().withType(JavaPlugin.class, new JavaPluginConfigurationAction(project));
        }
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
            JavaConventionConfiguration configuration = JavaConventionConfiguration.of(project);
            if (configuration == null) {
                project.getLogger().warn("Skipping implicit Byte Buddy task configuration since Java plugin did not register Java plugin convention or extension");
            } else {
                for (SourceSet sourceSet : configuration.getSourceSets()) {
                    String name = sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME)
                            ? "byteBuddy"
                            : (sourceSet.getName() + "ByteBuddy");
                    AbstractByteBuddyTaskExtension<?> extension = ObjectFactory.newInstance(project,
                            DISPATCHER.getExtensionType(),
                            project);
                    if (extension == null) {
                        extension = DISPATCHER.toExtension(project);
                    }
                    extension.resolve(configuration.getTargetCompatibility());
                    extension.discoverySet(project.getConfigurations().maybeCreate(name));
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
         * Returns the Byte Buddy extension type.
         *
         * @return The Byte Buddy extension type.
         */
        Class<S> getExtensionType();

        /**
         * Creates a Byte Buddy extension instance.
         *
         * @param project The current Gradle project.
         * @return An appropriate Byte Buddy extension instance.
         */
        S toExtension(Project project);

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
            public Class<ByteBuddySimpleTaskExtension> getExtensionType() {
                return ByteBuddySimpleTaskExtension.class;
            }

            /**
             * {@inheritDoc}
             */
            public ByteBuddySimpleTaskExtension toExtension(Project project) {
                return new ByteBuddySimpleTaskExtension(project);
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
            private final Method getDestinationDirectorySource;

            /**
             * The {@code org.gradle.api.file.AbstractCompile#getDestinationDirectory} method.
             */
            private final Method getDestinationDirectoryTarget;

            /**
             * Creates a new dispatcher for a Gradle version of at least 6.
             *
             * @param getDestinationDirectorySource The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
             * @param getDestinationDirectoryTarget The {@code org.gradle.api.file.AbstractCompile#getDestinationDirectory} method.
             */
            protected ForApi6CapableGradle(Method getDestinationDirectorySource, Method getDestinationDirectoryTarget) {
                this.getDestinationDirectorySource = getDestinationDirectorySource;
                this.getDestinationDirectoryTarget = getDestinationDirectoryTarget;
            }

            /**
             * {@inheritDoc}
             */
            public Class<ByteBuddyTaskExtension> getExtensionType() {
                return ByteBuddyTaskExtension.class;
            }

            /**
             * {@inheritDoc}
             */
            public ByteBuddyTaskExtension toExtension(Project project) {
                return new ByteBuddyTaskExtension(project);
            }

            /**
             * {@inheritDoc}
             */
            public ByteBuddyTaskConfiguration toAction(String name, SourceSet sourceSet) {
                return new ByteBuddyTaskConfiguration(name, sourceSet, getDestinationDirectorySource, getDestinationDirectoryTarget);
            }
        }
    }

    /**
     * Resolves the contextual configuration based on the project's Java plugin, if any.
     */
    protected static class JavaConventionConfiguration {

        /**
         * The {@code org.gradle.api.plugins.JavaPluginConvention} class or {@code null} if not available.
         */
        @MaybeNull
        private static final Class<?> JAVA_PLUGIN_CONVENTION;

        /**
         * The {@code org.gradle.api.plugins.JavaPluginExtension} class or {@code null} if not available.
         */
        @MaybeNull
        private static final Class<?> JAVA_PLUGIN_EXTENSION;

        /**
         * The {@code org.gradle.api.Project#getConvention()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_CONVENTION;

        /**
         * The {@code org.gradle.api.Project#getExtensions()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_EXTENSIONS;

        /**
         * The {@code org.gradle.api.plugin.ExtensionContainer#findByType(Class)} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method FIND_BY_TYPE;

        /**
         * The {@code org.gradle.api.plugins.JavaPluginConvention#getSourceSets()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_SOURCE_SETS_CONVENTION;

        /**
         * The {@code org.gradle.api.plugins.JavaPluginExtension#getSourceSets()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_SOURCE_SETS_EXTENSION;

        /**
         * The {@code org.gradle.api.plugins.JavaPluginConvention#getTargetCompatibility()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_TARGET_COMPATIBILITY_CONVENTION;

        /**
         * The {@code org.gradle.api.plugins.JavaPluginExtension#getTargetCompatibility()} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method GET_TARGET_COMPATIBILITY_EXTENSION;

        /**
         * The {@code org.gradle.api.plugins.Convention#findPlugin(Class)} method or {@code null} if not available.
         */
        @MaybeNull
        private static final Method FIND_PLUGIN;

        /*
         * Resolves the convention methods which might no longer be supported.
         */
        static {
            Class<?> javaPluginConvention, javaPluginExtension;
            Method getConvention, getExtensions, findPlugin, findByType, getSourceSetsConvention, getSourceSetsExtension, getTargetCompatibilityConvention, getTargetCompatibilityExtension;
            try {
                javaPluginConvention = Class.forName("org.gradle.api.plugins.JavaPluginConvention");
                getConvention = Project.class.getMethod("getConvention");
                findPlugin = Class.forName("org.gradle.api.plugins.Convention").getMethod("findPlugin", Class.class);
                getSourceSetsConvention = javaPluginConvention.getMethod("getSourceSets");
                getTargetCompatibilityConvention = javaPluginConvention.getMethod("getTargetCompatibility");
            } catch (Throwable ignored) {
                javaPluginConvention = null;
                getConvention = null;
                findPlugin = null;
                getSourceSetsConvention = null;
                getTargetCompatibilityConvention = null;
            }
            try {
                javaPluginExtension = Class.forName("org.gradle.api.plugins.JavaPluginExtension");
                getExtensions = Project.class.getMethod("getExtensions");
                findByType = Class.forName("org.gradle.api.plugins.ExtensionContainer").getMethod("findByType", Class.class);
                getSourceSetsExtension = javaPluginExtension.getMethod("getSourceSets");
                getTargetCompatibilityExtension = javaPluginExtension.getMethod("getTargetCompatibility");
            } catch (Throwable ignored) {
                javaPluginExtension = null;
                getExtensions = null;
                findByType = null;
                getSourceSetsExtension = null;
                getTargetCompatibilityExtension = null;
            }
            JAVA_PLUGIN_CONVENTION = javaPluginConvention;
            JAVA_PLUGIN_EXTENSION = javaPluginExtension;
            GET_CONVENTION = getConvention;
            GET_EXTENSIONS = getExtensions;
            FIND_PLUGIN = findPlugin;
            FIND_BY_TYPE = findByType;
            GET_SOURCE_SETS_CONVENTION = getSourceSetsConvention;
            GET_SOURCE_SETS_EXTENSION = getSourceSetsExtension;
            GET_TARGET_COMPATIBILITY_CONVENTION = getTargetCompatibilityConvention;
            GET_TARGET_COMPATIBILITY_EXTENSION = getTargetCompatibilityExtension;
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
        protected JavaConventionConfiguration(SourceSetContainer sourceSets, JavaVersion targetCompatibility) {
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
        protected static ByteBuddyPlugin.JavaConventionConfiguration of(Project project) {
            if (JAVA_PLUGIN_EXTENSION != null
                    && GET_EXTENSIONS != null
                    && FIND_BY_TYPE != null
                    && GET_SOURCE_SETS_EXTENSION != null
                    && GET_TARGET_COMPATIBILITY_EXTENSION != null) {
                try {
                    Object extension = FIND_BY_TYPE.invoke(GET_EXTENSIONS.invoke(project), JAVA_PLUGIN_EXTENSION);
                    if (extension != null) {
                        return new JavaConventionConfiguration(
                                (SourceSetContainer) GET_SOURCE_SETS_EXTENSION.invoke(extension),
                                (JavaVersion) GET_TARGET_COMPATIBILITY_EXTENSION.invoke(extension));
                    }
                } catch (Throwable ignored) {
                    /* do nothing */
                }
            }
            if (JAVA_PLUGIN_CONVENTION != null
                    && GET_CONVENTION != null
                    && FIND_PLUGIN != null
                    && GET_SOURCE_SETS_CONVENTION != null
                    && GET_TARGET_COMPATIBILITY_CONVENTION != null) {
                try {
                    Object convention = FIND_PLUGIN.invoke(GET_CONVENTION.invoke(project), JAVA_PLUGIN_CONVENTION);
                    if (convention != null) {
                        return new JavaConventionConfiguration(
                                (SourceSetContainer) GET_SOURCE_SETS_CONVENTION.invoke(convention),
                                (JavaVersion) GET_TARGET_COMPATIBILITY_CONVENTION.invoke(convention));
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
