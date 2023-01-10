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
