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

import com.android.build.api.attributes.BuildTypeAttr;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.artifacts.transform.TransformSpec;
import org.gradle.api.attributes.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Creates Byte Buddy's dependency configuration which is needed to declare libraries
 * with Byte Buddy plugins that are going to be used at compile time.
 */
public class ByteBuddyDependenciesHandler {

    private static final String BYTEBUDDY_CONFIGURATION_NAME_FORMAT = "%sBytebuddy";
    private static final Attribute<String> ARTIFACT_TYPE_ATTR = Attribute.of("artifactType", String.class);
    private static final String BYTEBUDDY_JAR_TYPE = "bytebuddy-jar";

    /**
     * The targeted Gradle project.
     */
    private final Project project;

    /**
     * The class paths being used.
     */
    private final Map<String, Configuration> classpaths = new HashMap<>();

    /**
     * The configuration to use.
     */
    private final Configuration configuration;

    /**
     * Creates a new dependencies handler for Byte Buddy.
     *
     * @param project       The targeted Gradle project.
     * @param configuration The configuration to use.
     */
    protected ByteBuddyDependenciesHandler(Project project, Configuration configuration) {
        this.project = project;
        this.configuration = configuration;
    }

    /**
     * Resolves a dependencies handler for Byte Buddy.
     *
     * @param project The targeted Gradle project.
     * @return An appropriate dependencies handler.
     */
    protected static ByteBuddyDependenciesHandler of(Project project) {
        project.getDependencies().registerTransform(AarGradleTransformAction.class, new TransformActionConfiguration());
        project.getDependencies().getAttributesSchema().attribute(ARTIFACT_TYPE_ATTR, new AttributeMatchingStrategyConfiguration());
        return new ByteBuddyDependenciesHandler(project, project.getConfigurations().create("byteBuddy", new BucketConfiguration()));
    }

    protected static class BucketConfiguration implements Action<Configuration> {

        /**
         * {@inheritDoc}
         */
        public void execute(Configuration configuration) {
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(false);
        }
    }

    protected static class TransformActionConfiguration implements Action<TransformSpec<TransformParameters.None>> {

        /**
         * {@inheritDoc}
         */
        public void execute(TransformSpec<TransformParameters.None> spec) {
            spec.getFrom().attribute(ARTIFACT_TYPE_ATTR, "aar");
            spec.getTo().attribute(ARTIFACT_TYPE_ATTR, BYTEBUDDY_JAR_TYPE);
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

    public Configuration getConfigurationForBuildType(String buildType) {
        Configuration configuration = classpaths.get(buildType);

        if (configuration == null) {
            configuration = createClasspathConfiguration(buildType);
            classpaths.put(buildType, configuration);
        }

        return configuration;
    }

    private Configuration createClasspathConfiguration(String buildType) {
        return project.getConfigurations().create(getNameFor(buildType), configuration -> {
            configuration.setCanBeResolved(true);
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(this.configuration);
            configuration.attributes(attrs -> {
                attrs.attribute(ARTIFACT_TYPE_ATTR, BYTEBUDDY_JAR_TYPE);
                attrs.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.getObjects().named(Category.class, Category.LIBRARY)
                );
                attrs.attribute(BuildTypeAttr.ATTRIBUTE, project.getObjects().named(BuildTypeAttr.class, buildType));
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
            });
        });
    }

    private String getNameFor(String buildType) {
        return String.format(BYTEBUDDY_CONFIGURATION_NAME_FORMAT, buildType);
    }

    public abstract static class BytebuddyJarsRule implements AttributeCompatibilityRule<String> {
        @Override
        public void execute(CompatibilityCheckDetails<String> details) {
            if (Objects.equals(details.getConsumerValue(), BYTEBUDDY_JAR_TYPE) && Objects.equals(details.getProducerValue(), "jar")) {
                details.compatible();
            }
        }
    }
}
