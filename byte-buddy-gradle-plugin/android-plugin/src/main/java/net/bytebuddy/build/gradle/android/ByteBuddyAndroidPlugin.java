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
package net.bytebuddy.build.gradle.android;

import com.android.build.api.AndroidPluginVersion;
import com.android.build.api.artifact.Artifact;
import com.android.build.api.artifact.Artifacts;
import com.android.build.api.artifact.MultipleArtifact;
import com.android.build.api.attributes.BuildTypeAttr;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.component.ComponentCreationConfig;
import com.android.build.gradle.internal.publishing.AndroidArtifacts;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.AttributeMatchingStrategy;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

/**
 * A Byte Buddy plugin-variant to use in combination with Gradle's support for Android.
 */
public class ByteBuddyAndroidPlugin implements Plugin<Project> {

    /**
     * The name of the artifact type attribute.
     */
    protected static final Attribute<String> ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String.class);

    /**
     * The dispatcher to use for registering the transformation.
     */
    protected static final TransformationDispatcher TRANSFORMATION_DISPATCHER;

    /**
     * The name of the Byte Buddy jar type.
     */
    public static final String BYTE_BUDDY_CLASSES_TYPE = "bytebuddy-classes";

    /**
     * The name of the Byte Buddy resources type.
     */
    public static final String BYTE_BUDDY_RESOURCES_TYPE = "bytebuddy-resources";

    /*
     * Resolves the dispatcher.
     */
    static {
        TransformationDispatcher dispatcher;
        try {
            Class<?> scope = Class.forName("com.android.build.api.variant.ScopedArtifacts$Scope");
            Class<?> scopedArtifacts = Class.forName("com.android.build.api.variant.ScopedArtifacts");
            Class<?> scopedArtifact = Class.forName("com.android.build.api.artifact.ScopedArtifact");
            @SuppressWarnings("unchecked")
            Object project = Enum.valueOf((Class) scope, "ALL");
            @SuppressWarnings("unchecked")
            Artifact<FileSystemLocation> location = (Artifact<FileSystemLocation>) Class.forName("com.android.build.api.artifact.ScopedArtifact$CLASSES").getField("INSTANCE").get(null);
            dispatcher = new TransformationDispatcher.ForApk74CompatibleAndroid(
                Artifacts.class.getMethod("forScope", scope),
                scopedArtifacts.getMethod("use", TaskProvider.class),
                Class.forName("com.android.build.api.variant.ScopedArtifactsOperation").getMethod("toTransform",
                    scopedArtifact,
                    Function1.class,
                    Function1.class,
                    Function1.class),
                project,
                location);
        } catch (Throwable ignored) {
            dispatcher = TransformationDispatcher.ForLegacyAndroid.INSTANCE;
        }
        TRANSFORMATION_DISPATCHER = dispatcher;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(Project project) {
        @SuppressWarnings("unchecked")
        AndroidComponentsExtension<?, ?, Variant> extension = project.getExtensions().getByType(AndroidComponentsExtension.class);
        AndroidPluginVersion currentAgpVersion = extension.getPluginVersion();
        if (currentAgpVersion.compareTo(new AndroidPluginVersion(7, 2)) < 0) {
            throw new IllegalStateException("Byte Buddy requires at least Gradle Plugin version 7.2+, but found " + currentAgpVersion);
        }
        project.getDependencies().getAttributesSchema().attribute(ARTIFACT_TYPE_ATTRIBUTE, new AttributeMatchingStrategyConfigurationAction());
        project.getExtensions().add("byteBuddy", new ByteBuddyAndroidTaskExtension(project));
        extension.onVariants(extension.selector().all(), new VariantAction(project, project.getConfigurations().create("byteBuddy", new ConfigurationConfigurationAction())));
    }

    /**
     * An action to handle a {@link Variant}.
     */
    protected static class VariantAction implements Action<Variant> {

        /**
         * The current Gradle project.
         */
        private final Project project;

        /**
         * The general Byte Buddy configuration.
         */
        private final Configuration configuration;

        /**
         * A cache of configurations by built type name.
         */
        private final ConcurrentMap<String, Configuration> configurations;

        /**
         * Creates a new variant action.
         *
         * @param project       The current Gradle project.
         * @param configuration The general Byte Buddy configuration.
         */
        protected VariantAction(Project project, Configuration configuration) {
            this.project = project;
            this.configuration = configuration;
            configurations = new ConcurrentHashMap<String, Configuration>();
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Variant variant) {
            String buildType = variant.getBuildType();
            if (buildType == null) {
                throw new GradleException("Build type for " + variant + " was null");
            }
            String variantName = variant.getName();
            ConfigurationConfigurationAction declarableConfigurationAction = new ConfigurationConfigurationAction();
            Configuration buildTypeDeclarableConfiguration = configurations.get(buildType);
            if (buildTypeDeclarableConfiguration == null) {
                buildTypeDeclarableConfiguration = project.getConfigurations().maybeCreate(buildType + "ByteBuddy");
                declarableConfigurationAction.execute(buildTypeDeclarableConfiguration);
                buildTypeDeclarableConfiguration.extendsFrom(configuration);
                Configuration previous = configurations.putIfAbsent(buildType, buildTypeDeclarableConfiguration);
                if (previous != null) {
                    buildTypeDeclarableConfiguration = previous;
                }
            }
            String variantDeclarableConfigurationName = variantName + "ByteBuddy";
            Configuration variantDeclarableConfiguration;
            if (variantDeclarableConfigurationName.equals(buildTypeDeclarableConfiguration.getName())) {
                variantDeclarableConfiguration = buildTypeDeclarableConfiguration;
            } else {
                variantDeclarableConfiguration = project.getConfigurations().maybeCreate(variantDeclarableConfigurationName);
                declarableConfigurationAction.execute(variantDeclarableConfiguration);
                variantDeclarableConfiguration.extendsFrom(buildTypeDeclarableConfiguration);
            }
            Configuration variantResolvableConfiguration = project.getConfigurations().create(
                    variantDeclarableConfigurationName + "Classpath",
                    new VariantConfigurationConfigurationAction(project, variantDeclarableConfiguration, buildType));
            if (TRANSFORMATION_DISPATCHER instanceof TransformationDispatcher.ForApk74CompatibleAndroid) {
                TRANSFORMATION_DISPATCHER.accept(project, variant, variantResolvableConfiguration, null);
            } else {
                Provider<ByteBuddyAndroidService> byteBuddyAndroidServiceProvider = project.getGradle().getSharedServices().registerIfAbsent(
                        variantName + "ByteBuddyAndroidService",
                        ByteBuddyAndroidService.class,
                        new ByteBuddyAndroidService.ConfigurationAction(project.getExtensions().getByType(BaseExtension.class)));
                FileCollection classPath = RuntimeClassPathResolver.INSTANCE.apply(variant);
                variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, new ByteBuddyTransformationConfiguration(project,
                        variantResolvableConfiguration,
                        byteBuddyAndroidServiceProvider,
                        classPath));
                TRANSFORMATION_DISPATCHER.accept(project, variant, variantResolvableConfiguration, classPath);
            }
        }
    }

    /**
     * A dispatcher for resolving the runtime class path.
     */
    protected abstract static class RuntimeClassPathResolver {

        /**
         * The runtime class path resolver to use.
         */
        protected static final RuntimeClassPathResolver INSTANCE;

        /*
         * Creates the runtime class path resolver to use.
         */
        static {
            RuntimeClassPathResolver instance;
            try {
                instance = new OfModernAgp(Variant.class.getMethod("getRuntimeConfiguration"));
            } catch (Throwable ignored) {
                instance = new OfLegacyAgp();
            }
            INSTANCE = instance;
        }

        /**
         * Resolves the runtime class path.
         *
         * @param variant The variant for which to resolve the runtime class path.
         * @return The runtime class path.
         */
        protected abstract FileCollection apply(Variant variant);

        /**
         * Before AGP 7.3, the {@code com.android.build.api.variant.Variant#getRuntimeConfiguration()} method is not available and an
         * internal cast must be used to resolve the runtime class path.
         */
        protected static class OfLegacyAgp extends RuntimeClassPathResolver {

            @Override
            protected FileCollection apply(Variant variant) {
                if (!(variant instanceof ComponentCreationConfig)) {
                    throw new GradleException("Cannot resolve runtime class path for " + variant);
                }
                return ((ComponentCreationConfig) variant).getVariantDependencies().getArtifactFileCollection(AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                    AndroidArtifacts.ArtifactScope.ALL,
                    AndroidArtifacts.ArtifactType.CLASSES_JAR);
            }
        }

        /**
         * From AGP 7.3, the runtime configuration can be queried from the {@link Variant}.
         */
        protected static class OfModernAgp extends RuntimeClassPathResolver implements Action<ArtifactView.ViewConfiguration> {

            /**
             * The {@code com.android.build.api.variant.Variant#getRuntimeConfiguration()} method.
             */
            private final Method getRuntimeConfiguration;

            /**
             * Creates a new resolver.
             *
             * @param getRuntimeConfiguration The {@code com.android.build.api.variant.Variant#getRuntimeConfiguration()} method.
             */
            protected OfModernAgp(Method getRuntimeConfiguration) {
                this.getRuntimeConfiguration = getRuntimeConfiguration;
            }

            @Override
            protected FileCollection apply(Variant variant) {
                try {
                    return ((Configuration) getRuntimeConfiguration.invoke(variant)).getIncoming()
                        .artifactView(this)
                        .getArtifacts()
                        .getArtifactFiles();
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Failed to access runtime configuration", exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Failed to resolve runtime configuration", exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public void execute(ArtifactView.ViewConfiguration configuration) {
                configuration.setLenient(false);
                configuration.getAttributes().attribute(ARTIFACT_TYPE_ATTRIBUTE, "android-classes-jar");
            }
        }
    }

    /**
     * A function to register Byte Buddy instrumentation parameters into the current execution.
     */
    protected static class ByteBuddyTransformationConfiguration implements Function1<ByteBuddyInstrumentationParameters, Unit> {

        /**
         * The current Gradle project.
         */
        private final Project project;

        /**
         * The current variant Byte Buddy configuration.
         */
        private final Configuration configuration;

        /**
         * A provider for a {@link ByteBuddyAndroidService}.
         */
        private final Provider<ByteBuddyAndroidService> byteBuddyAndroidServiceProvider;

        /**
         * The current variant's runtime classpath.
         */
        private final FileCollection classPath;

        /**
         * Creates a new Byte Buddy transformation configuration.
         *
         * @param project                         The current Gradle project.
         * @param configuration                   The current variant Byte Buddy configuration.
         * @param byteBuddyAndroidServiceProvider A provider for a {@link ByteBuddyAndroidService}.
         * @param classPath                       The current variant's runtime classpath.
         */
        protected ByteBuddyTransformationConfiguration(Project project,
                                                       Configuration configuration,
                                                       Provider<ByteBuddyAndroidService> byteBuddyAndroidServiceProvider,
                                                       FileCollection classPath) {
            this.project = project;
            this.configuration = configuration;
            this.byteBuddyAndroidServiceProvider = byteBuddyAndroidServiceProvider;
            this.classPath = classPath;
        }

        /**
         * {@inheritDoc}
         */
        public Unit invoke(ByteBuddyInstrumentationParameters parameters) {
            parameters.getByteBuddyClasspath().from(ByteBuddyViewConfiguration.toClassPath(project, configuration));
            parameters.getAndroidBootClasspath().from(project.getExtensions().getByType(BaseExtension.class).getBootClasspath());
            parameters.getRuntimeClasspath().from(classPath);
            parameters.getByteBuddyService().set(byteBuddyAndroidServiceProvider);
            return Unit.INSTANCE;
        }
    }

    /**
     * A configuration action for a variant {@link Configuration}.
     */
    protected static class VariantConfigurationConfigurationAction implements Action<Configuration> {

        /**
         * The current Gradle project.
         */
        private final Project project;

        /**
         * The configuration to extend from.
         */
        private final Configuration configuration;

        /**
         * The name of the build type.
         */
        private final String buildType;

        /**
         * Creates a new variant configuration for a {@link Configuration}.
         *
         * @param project       The current Gradle project.
         * @param configuration The configuration to extend from.
         * @param buildType     The name of the build type.
         */
        protected VariantConfigurationConfigurationAction(Project project, Configuration configuration, String buildType) {
            this.project = project;
            this.configuration = configuration;
            this.buildType = buildType;
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Configuration configuration) {
            configuration.setCanBeResolved(true);
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(this.configuration);
            configuration.attributes(new AttributeContainerConfigurationAction(project, buildType));
        }
    }

    /**
     * A configuration action for an {@link AttributeContainer}.
     */
    protected static class AttributeContainerConfigurationAction implements Action<AttributeContainer> {

        /**
         * The current Gradle project.
         */
        private final Project project;

        /**
         * The name of the build type.
         */
        private final String buildType;

        /**
         * Creates a new configuration for an {@link AttributeContainer}.
         *
         * @param project   The current Gradle project.
         * @param buildType The name of the build type.
         */
        protected AttributeContainerConfigurationAction(Project project, String buildType) {
            this.project = project;
            this.buildType = buildType;
        }

        /**
         * {@inheritDoc}
         */
        public void execute(AttributeContainer attributes) {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
            attributes.attribute(BuildTypeAttr.ATTRIBUTE, project.getObjects().named(BuildTypeAttr.class, buildType));
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        }
    }

    /**
     * A configuration for a {@link AttributeMatchingStrategy}.
     */
    protected static class AttributeMatchingStrategyConfigurationAction implements Action<AttributeMatchingStrategy<String>> {

        /**
         * {@inheritDoc}
         */
        public void execute(AttributeMatchingStrategy<String> stringAttributeMatchingStrategy) {
            stringAttributeMatchingStrategy.getCompatibilityRules().add(ByteBuddyDependencyRule.class);
        }
    }

    /**
     * A configuration action for a {@link Configuration}.
     */
    protected static class ConfigurationConfigurationAction implements Action<Configuration> {

        /**
         * {@inheritDoc}
         */
        public void execute(Configuration configuration) {
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(false);
        }
    }

    /**
     * A rule to check for jar compatibility.
     */
    public abstract static class ByteBuddyDependencyRule implements AttributeCompatibilityRule<String> {

        /**
         * {@inheritDoc}
         */
        public void execute(CompatibilityCheckDetails<String> details) {
            if (BYTE_BUDDY_CLASSES_TYPE.equals(details.getConsumerValue())) {
                String producerValue = details.getProducerValue();
                if ("java-classes-directory".equals(producerValue) || "android-classes-directory".equals(producerValue)) {
                    details.compatible();
                }
            } else if (BYTE_BUDDY_RESOURCES_TYPE.equals(details.getConsumerValue())) {
                String producerValue = details.getProducerValue();
                if ("java-resources-directory".equals(producerValue) || "android-java-res".equals(producerValue)) {
                    details.compatible();
                }
            }
        }
    }

    /**
     * Used to wire the local transformation task depending on the API being used in the host project.
     */
    protected interface TransformationDispatcher {

        /**
         * A dispatcher that is used for Android versions that do not support APK version 7.4 or newer.
         */
        enum ForLegacyAndroid implements TransformationDispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public void accept(Project project, Variant variant, Configuration configuration, FileCollection classPath) {
                TaskProvider<LegacyByteBuddyLocalClassesEnhancerTask> provider = project.getTasks().register(variant.getName() + "BytebuddyLocalTransform",
                    LegacyByteBuddyLocalClassesEnhancerTask.class,
                    new LegacyByteBuddyLocalClassesEnhancerTask.ConfigurationAction(ByteBuddyViewConfiguration.toClassPath(project, configuration),
                        project.getExtensions().getByType(BaseExtension.class),
                        classPath));
                variant.getArtifacts()
                    .use(provider)
                    .wiredWith(GetLocalClassesFunction.INSTANCE, GetOutputDirFunction.INSTANCE)
                    .toTransform(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE);
            }

            /**
             * A function representation of getting the local classes.
             */
            protected enum GetLocalClassesFunction implements Function1<LegacyByteBuddyLocalClassesEnhancerTask, ListProperty<Directory>> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public ListProperty<Directory> invoke(LegacyByteBuddyLocalClassesEnhancerTask task) {
                    return task.getLocalClassesDirs();
                }
            }

            /**
             * A function representation of getting the output directory.
             */
            protected enum GetOutputDirFunction implements Function1<LegacyByteBuddyLocalClassesEnhancerTask, DirectoryProperty> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public DirectoryProperty invoke(LegacyByteBuddyLocalClassesEnhancerTask task) {
                    return task.getOutputDir();
                }
            }
        }

        /**
         * A dispatcher that is used for Android versions that do support APK version 7.4 or newer.
         */
        class ForApk74CompatibleAndroid implements TransformationDispatcher {

            /**
             * The {@code com.android.build.api.variant.ScopedArtifacts$Scope#forScope} method.
             */
            private final Method forScope;

            /**
             * The {@code com.android.build.api.variant.ScopedArtifacts#use} method.
             */
            private final Method use;

            /**
             * The {@code com.android.build.api.variant.ScopedArtifactsOperation¤toTransform} method.
             */
            private final Method toTransform;

            /**
             * The {@code com.android.build.api.variant.ScopedArtifacts$Scope#PROJECT} value.
             */
            private final Object scope;

            /**
             * The {@code com.android.build.api.artifact.ScopedArtifact$CLASSES#INSTANCE} value.
             */
            private final Artifact<FileSystemLocation> artifact;

            /**
             * Creates a new dispatcher.
             *
             * @param forScope    The {@code com.android.build.api.variant.ScopedArtifacts$Scope#forScope} method.
             * @param use         The {@code com.android.build.api.variant.ScopedArtifacts#use} method.
             * @param toTransform The {@code com.android.build.api.variant.ScopedArtifactsOperation¤toTransform} method.
             * @param scope       The {@code com.android.build.api.variant.ScopedArtifacts$Scope#PROJECT} value.
             * @param artifact    The {@code com.android.build.api.artifact.ScopedArtifact$CLASSES#INSTANCE} value.
             */
            protected ForApk74CompatibleAndroid(Method forScope, Method use, Method toTransform, Object scope, Artifact<FileSystemLocation> artifact) {
                this.forScope = forScope;
                this.use = use;
                this.toTransform = toTransform;
                this.scope = scope;
                this.artifact = artifact;
            }

            /**
             * {@inheritDoc}
             */
            public void accept(Project project, Variant variant, Configuration configuration, FileCollection classPath) {
                if (configuration.getAllDependencies().isEmpty()) {
                    return;
                }
                TaskProvider<ByteBuddyLocalClassesEnhancerTask> provider = project.getTasks().register(variant.getName() + "BytebuddyTransform",
                    ByteBuddyLocalClassesEnhancerTask.class,
                    new ByteBuddyLocalClassesEnhancerTask.ConfigurationAction(ByteBuddyViewConfiguration.toClassPath(project, configuration),
                            project.getExtensions().getByType(BaseExtension.class),
                            project.getExtensions().getByType(ByteBuddyAndroidTaskExtension.class)));
                try {
                    toTransform.invoke(use.invoke(forScope.invoke(variant.getArtifacts(), scope), provider),
                        artifact,
                        GetProjectJarsFunction.INSTANCE,
                        GetLocalClassesDirsFunction.INSTANCE,
                        GetOutputFileFunction.INSTANCE);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Failed to variant scope", exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Failed to resolve runtime scope", exception.getCause());
                }
            }

            /**
             * A function representation of resolving local and dependencies jars.
             */
            protected enum GetProjectJarsFunction implements Function1<ByteBuddyLocalClassesEnhancerTask, ListProperty<RegularFile>> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public ListProperty<RegularFile> invoke(ByteBuddyLocalClassesEnhancerTask task) {
                    return task.getInputJars();
                }
            }

            /**
             * A function representation of getting the local classes directory.
             */
            protected enum GetLocalClassesDirsFunction implements Function1<ByteBuddyLocalClassesEnhancerTask, ListProperty<Directory>> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public ListProperty<Directory> invoke(ByteBuddyLocalClassesEnhancerTask task) {
                    return task.getLocalClassesDirs();
                }
            }

            /**
             * A function representation of getting the output file.
             */
            protected enum GetOutputFileFunction implements Function1<ByteBuddyLocalClassesEnhancerTask, RegularFileProperty> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public RegularFileProperty invoke(ByteBuddyLocalClassesEnhancerTask task) {
                    return task.getOutputFile();
                }
            }
        }

        /**
         * Applies this dispatcher.
         *
         * @param project       The current project.
         * @param variant       The variant to use.
         * @param configuration The configuration to use.
         * @param classPath     The class path to use.
         */
        void accept(Project project, Variant variant, Configuration configuration, @MaybeNull FileCollection classPath);
    }

    /**
     * A view configuration for creating the Byte Buddy class path.
     */
    protected enum ByteBuddyViewConfiguration implements Action<ArtifactView.ViewConfiguration> {

        /**
         * A view configuration for classes.
         */
        FOR_CLASSES(BYTE_BUDDY_CLASSES_TYPE),

        /**
         * A view configuration for resources.
         */
        FOR_RESOURCES(BYTE_BUDDY_RESOURCES_TYPE);

        /**
         * The type of the configuration attribute.
         */
        private final String type;

        /**
         * Creates a new view configuration.
         *
         * @param type The type of the configuration attribute.
         */
        ByteBuddyViewConfiguration(String type) {
            this.type = type;
        }

        /**
         * For external dependencies, it provides their JAR files. For local project's dependencies, it provides their local
         * build dirs for both classes and resources. The latter allows for faster and more reliable (up-to-date) compilation processes
         * when using local plugins.
         */
        protected static FileCollection toClassPath(Project project, Configuration configuration) {
            return project.files(
                configuration.getIncoming().artifactView(FOR_CLASSES).getFiles(),
                configuration.getIncoming().artifactView(FOR_RESOURCES).getFiles());
        }

        /**
         * {@inheritDoc}
         */
        public void execute(ArtifactView.ViewConfiguration configuration) {
            configuration.lenient(false);
            configuration.getAttributes().attribute(ARTIFACT_TYPE_ATTRIBUTE, type);
        }
    }
}
