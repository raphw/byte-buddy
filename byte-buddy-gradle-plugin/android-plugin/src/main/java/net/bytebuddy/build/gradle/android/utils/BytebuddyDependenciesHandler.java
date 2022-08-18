package net.bytebuddy.build.gradle.android.utils;

import com.android.build.api.attributes.BuildTypeAttr;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.ArtifactAttributes;

public class BytebuddyDependenciesHandler {
    private final Project project;
    private Configuration bucket;
    private static final String BYTEBUDDY_CONFIGURATION_NAME_FORMAT = "%sBytebuddy";

    private static final Attribute<String> ARTIFACT_TYPE_ATTR = getArtifactTypeAttr();

    public BytebuddyDependenciesHandler(Project project) {
        this.project = project;
    }

    public Configuration getConfigurationForBuildType(String buildType) {
        return project.getConfigurations().create(getNameFor(buildType), configuration -> {
            configuration.setCanBeResolved(true);
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(getBucketConfiguration());
            configuration.attributes(attrs -> {
                attrs.attribute(ARTIFACT_TYPE_ATTR, "android-java-res");
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

    private Configuration getBucketConfiguration() {
        if (bucket == null) {
            bucket = project.getConfigurations().create("bytebuddy", configuration -> {
                configuration.setCanBeConsumed(false);
                configuration.setCanBeResolved(false);
            });
        }
        return bucket;
    }

    private static Attribute<String> getArtifactTypeAttr() {
        try {
            return ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE;
        } catch (NoSuchFieldError e) {
            return ArtifactAttributes.ARTIFACT_FORMAT;
        }
    }
}
