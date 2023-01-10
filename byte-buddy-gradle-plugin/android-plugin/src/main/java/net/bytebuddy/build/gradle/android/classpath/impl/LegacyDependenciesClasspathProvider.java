package net.bytebuddy.build.gradle.android.classpath.impl;

import com.android.build.api.component.impl.ComponentImpl;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.internal.publishing.AndroidArtifacts;
import net.bytebuddy.build.gradle.android.classpath.DependenciesClasspathProvider;
import org.gradle.api.file.FileCollection;

/**
 * This implementation is needed for projects running AGP version < 7.3, since the method {@link Variant#getRuntimeConfiguration()}
 * was added in AGP 7.3.0. So this legacy implementation uses a workaround due to the missing "getRuntimeConfiguration" method.
 */
public class LegacyDependenciesClasspathProvider implements DependenciesClasspathProvider {

    @Override
    public FileCollection getRuntimeClasspath(Variant variant) {
        return ((ComponentImpl) variant).getVariantDependencies().getArtifactFileCollection(AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                AndroidArtifacts.ArtifactScope.ALL,
                AndroidArtifacts.ArtifactType.CLASSES_JAR);
    }
}
