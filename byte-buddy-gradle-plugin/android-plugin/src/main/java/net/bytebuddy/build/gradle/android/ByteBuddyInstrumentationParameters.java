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
package net.bytebuddy.build.gradle.android;

import com.android.build.api.instrumentation.InstrumentationParameters;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

/**
 * The parameters provided to the Byte Buddy instrumentation.
 */
public interface ByteBuddyInstrumentationParameters extends InstrumentationParameters {

    /**
     * Returns the boot class path of Android.
     *
     * @return The boot class path of Android.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getAndroidBootClasspath();

    /**
     * Returns Byte Buddy's class path.
     *
     * @return Byte Buddy's class path.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getByteBuddyClasspath();

    /**
     * Returns the runtime class path.
     *
     * @return The runtime class path.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getRuntimeClasspath();

    /**
     * Returns the Byte Buddy service to use.
     *
     * @return The Byte Buddy service to use.
     */
    @Internal
    Property<ByteBuddyAndroidService> getByteBuddyService();
}
