package net.bytebuddy.build.gradle.android;

import net.bytebuddy.build.gradle.android.connector.AndroidPluginConnector;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import net.bytebuddy.build.gradle.android.transformation.impl.DefaultAndroidTransformation;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.ArtifactAttributes;

class ByteBuddyAndroidPlugin implements Plugin<Project> {

    private static final Attribute<String> ARTIFACT_TYPE_ATTR = getArtifactTypeAttr();

    private static Attribute<String> getArtifactTypeAttr() {
        try {
            return ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE;
        } catch (NoSuchFieldError e) {
            return ArtifactAttributes.ARTIFACT_FORMAT;
        }
    }

    @Override
    public void apply(Project project) {
        Configuration byteBuddyClasspath = createBytebuddyDependenciesConfiguration(project);
        AndroidTransformation byteBuddyTransformation = new DefaultAndroidTransformation(project.getLogger());
        AndroidPluginConnector connector = new AndroidPluginConnector(project, byteBuddyClasspath);

        connector.connect(byteBuddyTransformation);
    }

    private Configuration createBytebuddyDependenciesConfiguration(Project project) {
        Configuration bucket = project.getConfigurations().create("bytebuddy", it -> {
            it.setCanBeResolved(false);
            it.setCanBeConsumed(false);
        });

        return project.getConfigurations().create("bytebuddyClasspath", it -> {
            it.setCanBeConsumed(false);
            it.setCanBeResolved(true);
            it.extendsFrom(bucket);
            it.attributes(attrs -> {
                attrs.attribute(ARTIFACT_TYPE_ATTR, "android-classes");
                attrs.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.getObjects().named(Category.class, Category.LIBRARY)
                );
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_API));
            });
        });
    }
}