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
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ByteBuddyAndroidPlugin implements Plugin<Project> {

    protected static final Attribute<String> ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String.class);


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
        project.getDependencies().getAttributesSchema().attribute(ARTIFACT_TYPE_ATTRIBUTE, new AttributeMatchingStrategyConfiguration());
        extension.onVariants(extension.selector().all(), new VariantAction(project,
                extension,
                project.getConfigurations().create("byteBuddy", new ConfigurationConfiguration())));
    }

    protected static class VariantAction implements Action<Variant> {

        private final Project project;

        private final AndroidComponentsExtension<?, ?, ?> extension;

        private final Configuration configuration;

        protected VariantAction(Project project, AndroidComponentsExtension<?, ?, ?> extension, Configuration configuration) {
            this.project = project;
            this.extension = extension;
            this.configuration = configuration;
        }

        @Override
        public void execute(Variant variant) {
            TaskProvider<ByteBuddyCopyOutputTask> localClasses = project.getTasks().register(variant.getName() + "ByteBuddyLocalClasses",
                    ByteBuddyCopyOutputTask.class,
                    new ByteBuddyCopyOutputTask.ConfigurationAction(project, variant));
            Provider<ByteBuddyAndroidService> serviceProvider = project.getGradle().getSharedServices().registerIfAbsent(variant.getName() + "ByteBuddyAndroidService",
                    ByteBuddyAndroidService.class,
                    new ByteBuddyAndroidService.ConfigurationAction(extension));
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, new ByteBuddyTransformationConfiguration(project,
                    configuration,
                    localClasses,
                    serviceProvider,
                    variant));
        }
    }

    protected static class ByteBuddyTransformationConfiguration implements Function1<ByteBuddyInstrumentationParameters, Unit> {

        private final Project project;

        private final Configuration configuration;

        private final TaskProvider<ByteBuddyCopyOutputTask> localClasses;

        private final Provider<ByteBuddyAndroidService> serviceProvider;

        private final Variant variant;

        private final ConcurrentHashMap<String, Configuration> configurations;

        public ByteBuddyTransformationConfiguration(Project project,
                                                    Configuration configuration,
                                                    TaskProvider<ByteBuddyCopyOutputTask> localClasses,
                                                    Provider<ByteBuddyAndroidService> serviceProvider,
                                                    Variant variant) {
            this.project = project;
            this.configuration = configuration;
            this.localClasses = localClasses;
            this.serviceProvider = serviceProvider;
            this.variant = variant;
            configurations = new ConcurrentHashMap<String, Configuration>();
        }

        @Override
        public Unit invoke(ByteBuddyInstrumentationParameters parameters) {
            if (variant.getBuildType() == null) {
                throw new IllegalStateException();
            }
            Configuration configuration = configurations.get(variant.getBuildType());
            if (configuration == null) {
                configuration = project.getConfigurations().create(variant.getBuildType() + "ByteBuddy", new VariantConfiguration(project,
                        this.configuration,
                        variant.getBuildType()));
                configurations.put(variant.getBuildType(), configuration);
            }
            parameters.getByteBuddyClasspath().from(configuration);
            parameters.getAndroidBootClasspath().from(project.getExtensions().getByType(BaseExtension.class).getBootClasspath());
            ComponentImpl component = (ComponentImpl) variant;
            parameters.getRuntimeClasspath().from(component.getVariantDependencies().getArtifactFileCollection(AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                    AndroidArtifacts.ArtifactScope.ALL,
                    AndroidArtifacts.ArtifactType.CLASSES_JAR));
            parameters.getLocalClassesDirectories().from(localClasses);
            parameters.getByteBuddyService().set(serviceProvider);
            return Unit.INSTANCE;
        }
    }

    protected static class VariantConfiguration implements Action<Configuration> {

        private final Project project;

        private final Configuration configuration;

        private final String buildType;

        public VariantConfiguration(Project project, Configuration configuration, String buildType) {
            this.project = project;
            this.configuration = configuration;
            this.buildType = buildType;
        }

        @Override
        public void execute(Configuration configuration) {
            configuration.setCanBeResolved(true);
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(this.configuration);
            configuration.attributes(new AttributeContainerConfiguration(project, buildType));
        }
    }

    protected static class AttributeContainerConfiguration implements Action<AttributeContainer> {

        private final Project project;

        private final String buildType;

        protected AttributeContainerConfiguration(Project project, String buildType) {
            this.project = project;
            this.buildType = buildType;
        }

        @Override
        public void execute(AttributeContainer attributes) {
            attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, BYTE_BUDDY_JAR_TYPE);
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
            attributes.attribute(BuildTypeAttr.ATTRIBUTE, project.getObjects().named(BuildTypeAttr.class, buildType));
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));

        }
    }

    protected static class AttributeMatchingStrategyConfiguration implements Action<AttributeMatchingStrategy<String>> {

        /**
         * {@inheritDoc}
         */
        public void execute(AttributeMatchingStrategy<String> stringAttributeMatchingStrategy) {
            stringAttributeMatchingStrategy.getCompatibilityRules().add(BytebuddyJarsRule.class);
        }
    }

    protected static class ConfigurationConfiguration implements Action<Configuration> {

        /**
         * {@inheritDoc}
         */
        public void execute(Configuration configuration) {
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(false);
        }
    }

    public abstract static class BytebuddyJarsRule implements AttributeCompatibilityRule<String> {
        @Override
        public void execute(CompatibilityCheckDetails<String> details) {
            if (Objects.equals(details.getConsumerValue(), BYTE_BUDDY_JAR_TYPE) && Objects.equals(details.getProducerValue(), "jar")) {
                details.compatible();
            }
        }
    }
}