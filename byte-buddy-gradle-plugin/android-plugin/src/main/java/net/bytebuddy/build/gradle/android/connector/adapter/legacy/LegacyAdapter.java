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
package net.bytebuddy.build.gradle.android.connector.adapter.legacy;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import net.bytebuddy.build.gradle.android.connector.adapter.TransformationAdapter;
import net.bytebuddy.build.gradle.android.connector.adapter.legacy.transform.LegacyAndroidAppTransform;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import org.gradle.api.artifacts.Configuration;

/**
 * Adapter used when the host android project uses Android Gradle plugin version < 7.2
 */
public class LegacyAdapter implements TransformationAdapter {

    private final BaseExtension androidExtension;
    private final Configuration byteBuddyDependenciesConfiguration;

    public LegacyAdapter(BaseExtension androidExtension, Configuration byteBuddyDependenciesConfiguration) {
        this.androidExtension = androidExtension;
        this.byteBuddyDependenciesConfiguration = byteBuddyDependenciesConfiguration;
    }

    @Override
    public void adapt(AndroidTransformation transformation) {
        androidExtension.registerTransform(
                new LegacyAndroidAppTransform(
                        transformation,
                        (AppExtension) androidExtension,
                        byteBuddyDependenciesConfiguration
                )
        );
    }
}