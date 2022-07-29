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
package net.bytebuddy.build.gradle.android.transformation;

import java.io.File;
import java.util.Set;

public interface AndroidTransformation {

    void transform(Input input, File outputDir);

    class Input {
        public final Set<File> targetClasspath;
        public final Set<File> referenceClasspath;
        public final Set<File> androidBootClasspath;
        public final Set<File> bytebuddyDiscoveryClasspath;
        public final int jvmTargetVersion;

        /**
         * Instrumentation input.
         *
         * @param targetClasspath             - The target project's classpath, could be project only classes or project + dependencies
         *                                    classes.
         * @param referenceClasspath          - All non-target project's classes, could be dependencies that cannot be instrumented
         *                                    depending on the type of android project. This could be empty if all dependencies are part of the target.
         * @param androidBootClasspath        - Android immutable classes needed for context.
         * @param bytebuddyDiscoveryClasspath - The classpath where bytebuddy plugins are found.
         * @param jvmTargetVersion            - The java target version configured for the android build.
         */
        public Input(Set<File> targetClasspath, Set<File> referenceClasspath, Set<File> androidBootClasspath, Set<File> bytebuddyDiscoveryClasspath, int jvmTargetVersion) {
            this.targetClasspath = targetClasspath;
            this.referenceClasspath = referenceClasspath;
            this.androidBootClasspath = androidBootClasspath;
            this.bytebuddyDiscoveryClasspath = bytebuddyDiscoveryClasspath;
            this.jvmTargetVersion = jvmTargetVersion;
        }
    }
}