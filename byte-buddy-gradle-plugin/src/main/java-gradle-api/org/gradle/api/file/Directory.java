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
package org.gradle.api.file;

import java.io.File;

/**
 * A placeholder representation of Gradle's {@code org.gradle.api.file.Directory} type.
 */
public interface Directory {

    /**
     * A placeholder representation of Gradle's {@code org.gradle.api.file.Directory#getAsFile} method.
     *
     * @return The method's return value.
     */
    File getAsFile();

    /**
     * A placeholder representation of Gradle's {@code org.gradle.api.file.Directory#dir} method.
     *
     * @param path The method's argument.
     * @return The method's return value.
     */
    Directory dir(String path);
}
