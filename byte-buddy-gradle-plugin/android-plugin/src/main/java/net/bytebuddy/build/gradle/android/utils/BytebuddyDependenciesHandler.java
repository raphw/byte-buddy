package net.bytebuddy.build.gradle.android.utils;

import com.android.build.api.attributes.BuildTypeAttr;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;

public class BytebuddyDependenciesHandler {
    private final Project project;
    private Configuration bucket;
    private static final String BYTEBUDDY_CONFIGURATION_NAME_FORMAT = "%sBytebuddy";

    public static final Attribute<String> ARTIFACT_TYPE_ATTR = Attribute.of("artifactType", String.class);
    public static final String BYTEBUDDY_JAR_TYPE = "bytebuddy-jar";

    public BytebuddyDependenciesHandler(Project project) {
        this.project = project;
    }

    public void init() {
        initBucketConfig();
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
}
