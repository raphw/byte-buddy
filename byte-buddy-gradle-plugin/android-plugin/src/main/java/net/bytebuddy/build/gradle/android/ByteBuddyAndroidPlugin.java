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
import com.android.build.api.artifact.MultipleArtifact;
import com.android.build.api.attributes.BuildTypeAttr;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.component.ComponentCreationConfig;
import com.android.build.gradle.internal.publishing.AndroidArtifacts;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.*;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A Byte Buddy plugin-variant to use in combination with Gradle's support for Android.
 */
public class ByteBuddyAndroidPlugin implements Plugin<Project> {

    /**
     * The name of the artifact type attribute.
     */
    protected static final Attribute<String> ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String.class);

    /**
     * The name of the Byte Buddy jar type.
     */
    private static final String BYTE_BUDDY_JAR_TYPE = "bytebuddy-jar";

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
        project.getDependencies().registerTransform(AarGradleTransformAction.class, new AarGradleTransformAction.ConfigurationAction());
        project.getDependencies().getAttributesSchema().attribute(ARTIFACT_TYPE_ATTRIBUTE, new AttributeMatchingStrategyConfigurationAction());
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
            Provider<ByteBuddyAndroidService> byteBuddyAndroidServiceProvider = project.getGradle().getSharedServices().registerIfAbsent(variant.getName() + "ByteBuddyAndroidService",
                    ByteBuddyAndroidService.class,
                    new ByteBuddyAndroidService.ConfigurationAction(project.getExtensions().getByType(BaseExtension.class)));
            if (variant.getBuildType() == null) {
                throw new GradleException("Build type for " + variant + " was null");
            }
            Configuration configuration = configurations.get(variant.getBuildType());
            if (configuration == null) {
                configuration = project.getConfigurations().create(variant.getBuildType() + "ByteBuddy", new VariantConfigurationConfigurationAction(project,
                        this.configuration,
                        variant.getBuildType()));
                Configuration previous = configurations.putIfAbsent(variant.getBuildType(), configuration);
                if (previous != null) {
                    configuration = previous;
                }
            }
            FileCollection classPath = RuntimeClassPathResolver.INSTANCE.apply(variant);
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, new ByteBuddyTransformationConfiguration(project,
                    configuration,
                    byteBuddyAndroidServiceProvider,
                    classPath));
            TaskProvider<ByteBuddyLocalClassesEnhancerTask> localClassesTransformation = project.getTasks().register(variant.getName() + "BytebuddyLocalTransform",
                    ByteBuddyLocalClassesEnhancerTask.class,
                    new ByteBuddyLocalClassesEnhancerTask.ConfigurationAction(configuration, project.getExtensions().getByType(BaseExtension.class), classPath));
            variant.getArtifacts().use(localClassesTransformation)
                    .wiredWith(ByteBuddyLocalClassesEnhancerTask::getLocalClassesDirs, ByteBuddyLocalClassesEnhancerTask::getOutputDir)
                    .toTransform(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE);
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
            parameters.getByteBuddyClasspath().from(configuration);
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
         * The general Byte Buddy configuration.
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
         * @param configuration The general Byte Buddy configuration.
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
            attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, BYTE_BUDDY_JAR_TYPE);
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
            stringAttributeMatchingStrategy.getCompatibilityRules().add(ByteBuddyJarRule.class);
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
    public abstract static class ByteBuddyJarRule implements AttributeCompatibilityRule<String> {

        /**
         * {@inheritDoc}
         */
        public void execute(CompatibilityCheckDetails<String> details) {
            if (BYTE_BUDDY_JAR_TYPE.equals(details.getConsumerValue()) && "jar".equals(details.getProducerValue())) {
                details.compatible();
            }
        }
    }
}