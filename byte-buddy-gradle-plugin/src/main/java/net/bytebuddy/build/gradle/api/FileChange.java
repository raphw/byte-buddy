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
package net.bytebuddy.build.gradle.api;

import java.io.File;

/**
 * A placeholder representation of Gradle's {@code org.gradle.work.FileChange} type.
 */
@GradleType("org.gradle.work.FileChange")
public interface FileChange {

    /**
     * A placeholder representation of Gradle's {@code org.gradle.work.FileChange#getFile} method.
     *
     * @return The method's return value.
     */
    File getFile();

    /**
     * A placeholder representation of Gradle's {@code org.gradle.work.FileChange#getChangeType} method.
     *
     * @return The method's return value.
     */
    ChangeType getChangeType();
}
