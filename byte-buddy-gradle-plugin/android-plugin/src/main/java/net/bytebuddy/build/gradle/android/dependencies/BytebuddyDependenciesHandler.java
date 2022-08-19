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
package net.bytebuddy.build.gradle.android.dependencies;

import com.android.build.api.attributes.BuildTypeAttr;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.Usage;

import java.util.Objects;

public class BytebuddyDependenciesHandler {
    private final Project project;
    private Configuration bucket;
    private static final String BYTEBUDDY_CONFIGURATION_NAME_FORMAT = "%sBytebuddy";

    private static final Attribute<String> ARTIFACT_TYPE_ATTR = Attribute.of("artifactType", String.class);
    private static final String BYTEBUDDY_JAR_TYPE = "bytebuddy-jar";

    public BytebuddyDependenciesHandler(Project project) {
        this.project = project;
    }

    public void init() {
        initBucketConfig();
        registerAarToJarTransformation();
        registerBytebuddyJarRule();
    }

    public Configuration getConfigurationForBuildType(String buildType) {
        return project.getConfigurations().create(getNameFor(buildType), configuration -> {
            configuration.setCanBeResolved(true);
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(bucket);
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

    private void initBucketConfig() {
        bucket = project.getConfigurations().create("bytebuddy", configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(false);
        });
    }

    private void registerAarToJarTransformation() {
        project.getDependencies().registerTransform(AarGradleTransform.class, it -> {
            it.getFrom().attribute(ARTIFACT_TYPE_ATTR, "aar");
            it.getTo().attribute(ARTIFACT_TYPE_ATTR, BYTEBUDDY_JAR_TYPE);
        });
    }

    private void registerBytebuddyJarRule() {
        project.getDependencies().getAttributesSchema().attribute(ARTIFACT_TYPE_ATTR, stringAttributeMatchingStrategy ->
                stringAttributeMatchingStrategy.getCompatibilityRules().add(BytebuddyJarsRule.class));
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
