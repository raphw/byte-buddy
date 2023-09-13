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
package net.bytebuddy.build.gradle.android.wiring;

import com.android.build.api.variant.Variant;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;

/**
 * Used to wire the local transformation task depending on the API being used in the host project.
 */
public interface LocalTransformationTaskWiring {

    void wireTask(Variant variant, Configuration configuration, FileCollection classPath);
}
