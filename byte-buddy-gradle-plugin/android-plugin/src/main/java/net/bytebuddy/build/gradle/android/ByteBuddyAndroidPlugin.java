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
import com.android.build.api.component.impl.ComponentImpl;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.publishing.AndroidArtifacts;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.AttributeMatchingStrategy;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A Byte Buddy plugin-variant to use in combination with Gradle's support for Android.
 */
public class ByteBuddyAndroidPlugin implements Plugin<Project> {

    /**
     * The name of the artifact type attribute.
     */
    public static final Attribute<String> ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String.class);

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
        if (extension.getPluginVersion().compareTo(new AndroidPluginVersion(7, 2)) < 0) {
            throw new IllegalStateException("Byte Buddy requires at least Gradle Plugin version 7.2+, but found " + extension.getPluginVersion());
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
            if (!(variant instanceof ComponentImpl)) {
                throw new GradleException("Expected " + variant + " to be of type " + ComponentImpl.class.getName());
            }
            FileCollection classPath = ((ComponentImpl) variant).getVariantDependencies().getArtifactFileCollection(AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                    AndroidArtifacts.ArtifactScope.ALL,
                    AndroidArtifacts.ArtifactType.CLASSES_JAR);
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, new ByteBuddyTransformationConfiguration(project,
                    configuration,
                    byteBuddyAndroidServiceProvider,
                    classPath));
            TaskProvider<ByteBuddyLocalClassesEnhancerTask> localClassesTransformation = project.getTasks().register(variant.getName() + "BytebuddyLocalTransform", ByteBuddyLocalClassesEnhancerTask.class,
                    new ByteBuddyLocalClassesEnhancerTask.ConfigurationAction(
                            configuration,
                            project.getExtensions().getByType(BaseExtension.class),
                            classPath));
            variant.getArtifacts().use(localClassesTransformation)
                    .wiredWith(ByteBuddyLocalClassesEnhancerTask::getLocalClassesDirs, ByteBuddyLocalClassesEnhancerTask::getOutputDir)
                    .toTransform(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE);
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