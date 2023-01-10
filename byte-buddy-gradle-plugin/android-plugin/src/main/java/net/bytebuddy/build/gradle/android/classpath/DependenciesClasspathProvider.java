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
package net.bytebuddy.build.gradle.android.classpath;

import com.android.build.api.AndroidPluginVersion;
import com.android.build.api.variant.Variant;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.lang.reflect.InvocationTargetException;

/**
 * Needed to query the runtime classpath for an Android project, which process has changed in recent versions of the
 * AGP plugin, so each method gets its own implementation.
 */
public interface DependenciesClasspathProvider {

    /**
     * Returns the appropriate {@link DependenciesClasspathProvider} implementation based on the AGP version that the host
     * project is running.
     *
     * @param currentVersion The current AGP version used in the host project.
     */
    static DependenciesClasspathProvider getInstance(AndroidPluginVersion currentVersion) {
        boolean isLowerThan73 = currentVersion.compareTo(new AndroidPluginVersion(7, 3)) < 0;
        Logger logger = Logging.getLogger(DependenciesClasspathProvider.class);
        try {
            if (isLowerThan73) {
                logger.debug("Using legacy classpath provider implementation");
                return (DependenciesClasspathProvider) Class.forName("net.bytebuddy.build.gradle.android.classpath.impl.LegacyDependenciesClasspathProvider").getDeclaredConstructor().newInstance();
            } else {
                logger.debug("Using default classpath provider implementation");
                return (DependenciesClasspathProvider) Class.forName("net.bytebuddy.build.gradle.android.classpath.impl.DefaultDependenciesClasspathProvider").getDeclaredConstructor().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    FileCollection getRuntimeClasspath(Variant variant);
}
