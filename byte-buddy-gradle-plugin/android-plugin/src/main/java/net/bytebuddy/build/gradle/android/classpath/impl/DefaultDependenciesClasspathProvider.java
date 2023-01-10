package net.bytebuddy.build.gradle.android.classpath.impl;

import com.android.build.api.variant.Variant;
import net.bytebuddy.build.gradle.android.classpath.DependenciesClasspathProvider;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.file.FileCollection;

import static net.bytebuddy.build.gradle.android.ByteBuddyAndroidPlugin.ARTIFACT_TYPE_ATTRIBUTE;

/**
 * This implementation uses the method {@link Variant#getRuntimeConfiguration()} which was added in AGP version 7.3.0.
 */
public class DefaultDependenciesClasspathProvider implements DependenciesClasspathProvider {

    @Override
    public FileCollection getRuntimeClasspath(Variant variant) {
        return variant.getRuntimeConfiguration().getIncoming()
                .artifactView(new JarsViewAction())
                .getArtifacts()
                .getArtifactFiles();
    }

    /**
     * Needed to query ".jar" files from both, plain Java libraries and Android libraries too. Android libraries
     * are files of type ".aar" which contain a ".jar" file inside, without this filter, we'd get Android libraries
     * as raw ".aar" files, which cannot be used for Java classpath purposes.
     */
    protected static class JarsViewAction implements Action<ArtifactView.ViewConfiguration> {

        @Override
        public void execute(ArtifactView.ViewConfiguration configuration) {
            configuration.setLenient(false);
            configuration.getAttributes().attribute(ARTIFACT_TYPE_ATTRIBUTE, "android-classes-jar");
        }
    }
}