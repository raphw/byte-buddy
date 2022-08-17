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
package net.bytebuddy.build.gradle.android.connector;

import com.android.build.api.AndroidPluginVersion;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.gradle.BaseExtension;
import net.bytebuddy.build.gradle.android.connector.adapter.TransformationAdapter;
import net.bytebuddy.build.gradle.android.connector.adapter.current.CurrentAdapter;
import net.bytebuddy.build.gradle.android.connector.adapter.legacy.LegacyAdapter;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.ExtensionContainer;

/**
 * The Android build gradle plugin has 2 different ways of registering a bytecode processing tool, the right one to be
 * used depends on the version of the android gradle plugin set in the host android project. This class takes care
 * of choosing the right one and calling the corresponding adapter.
 */
public class AndroidPluginConnector {
    private final Project androidProject;
    private final Configuration bytebuddyDependenciesConfiguration;
    private final ExtensionContainer extensions;
    private final BaseExtension androidExtension;

    public AndroidPluginConnector(Project androidProject, Configuration bytebuddyDependenciesConfiguration) {
        this.androidProject = androidProject;
        this.bytebuddyDependenciesConfiguration = bytebuddyDependenciesConfiguration;
        extensions = androidProject.getExtensions();
        androidExtension = extensions.getByType(BaseExtension.class);
    }

    public void connect(AndroidTransformation transformation) {
        TransformationAdapter adapter = getAdapter(androidExtension);
        adapter.adapt(transformation);
    }

    private TransformationAdapter getAdapter(BaseExtension androidExtension) {
        AndroidComponentsExtension<?, ?, ?> androidComponentsExtension = findComponentsExtension();
        if (androidComponentsExtension == null) {
            return createLegacyAdapter(androidExtension);
        } else {
            AndroidPluginVersion currentVersion = androidComponentsExtension.getPluginVersion();
            AndroidPluginVersion versionWithNewApi = new AndroidPluginVersion(7, 2);
            if (Compare.of(currentVersion).isLessThan(versionWithNewApi)) {
                return createLegacyAdapter(androidExtension);
            } else {
                return createCurrentAdapter(androidComponentsExtension);
            }
        }
    }

    private CurrentAdapter createCurrentAdapter(
            AndroidComponentsExtension<?, ?, ?> androidComponentsExtension
    ) {
        return new CurrentAdapter(
                androidExtension,
                androidComponentsExtension,
                bytebuddyDependenciesConfiguration,
                androidProject.getTasks(),
                androidProject);
    }

    private LegacyAdapter createLegacyAdapter(
            BaseExtension androidExtensionn
    ) {
        return new LegacyAdapter(androidExtension, bytebuddyDependenciesConfiguration);
    }

    static class Compare {
        private final AndroidPluginVersion version;

        public static Compare of(AndroidPluginVersion version) {
            return new Compare(version);
        }

        private Compare(AndroidPluginVersion version) {
            this.version = version;
        }

        public boolean isLessThan(AndroidPluginVersion otherVersion) {
            return version.compareTo(otherVersion) < 0;
        }

    }

    /**
     * This is only available on the newer versions of the Android Gradle plugin, starting from version 7.
     */
    private AndroidComponentsExtension<?, ?, ?> findComponentsExtension() {
        try {
            return extensions.findByType(AndroidComponentsExtension.class);
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }
}