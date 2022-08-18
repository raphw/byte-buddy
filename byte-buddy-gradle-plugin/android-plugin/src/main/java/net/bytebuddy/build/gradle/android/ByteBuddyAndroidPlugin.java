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

import com.android.build.api.AndroidPluginVersion;
import com.android.build.api.artifact.MultipleArtifact;
import com.android.build.api.component.impl.ComponentImpl;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.BaseExtension;
import kotlin.Unit;
import net.bytebuddy.build.gradle.android.asm.ByteBuddyAsmClassVisitorFactory;
import net.bytebuddy.build.gradle.android.utils.BytebuddyDependenciesHandler;
import net.bytebuddy.build.gradle.android.utils.LocalClassesSync;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskProvider;

public class ByteBuddyAndroidPlugin implements Plugin<Project> {

    private AndroidComponentsExtension<?, ?, ?> androidComponentsExtension;
    private BaseExtension androidExtension;
    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;
        androidExtension = project.getExtensions().getByType(BaseExtension.class);
        androidComponentsExtension = project.getExtensions().findByType(AndroidComponentsExtension.class);
        verifyValidAndroidPlugin();
        BytebuddyDependenciesHandler dependenciesHandler = new BytebuddyDependenciesHandler(project);
        dependenciesHandler.init();
        registerBytebuddyAsmFactory(dependenciesHandler);
    }

    private void registerBytebuddyAsmFactory(BytebuddyDependenciesHandler dependenciesHandler) {
        androidComponentsExtension.onVariants(androidComponentsExtension.selector().all(), variant -> {
            TaskProvider<LocalClassesSync> localClasses = registerLocalClassesSyncTask(variant);
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, params -> {
                params.getByteBuddyClasspath().from(dependenciesHandler.getConfigurationForBuildType(variant.getBuildType()));
                params.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
                params.getRuntimeClasspath().from(getRuntimeClasspath(variant));
                params.getLocalClassesDirs().from(localClasses);
                return Unit.INSTANCE;
            });
        });
    }

    private TaskProvider<LocalClassesSync> registerLocalClassesSyncTask(Variant variant) {
        return project.getTasks().register(variant.getName() + "ByteBuddyLocalClasses", LocalClassesSync.class, classesSync -> {
            classesSync.getLocalClasspath().from(variant.getArtifacts().getAll(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE));
            classesSync.getOutputDir().set(project.getLayout().getBuildDirectory().dir("incremental/" + classesSync.getName()));
        });
    }

    private FileCollection getRuntimeClasspath(Variant variant) {
        ComponentImpl component = (ComponentImpl) variant;
        return component.getVariantDependencies().getRuntimeClasspath();
    }

    private void verifyValidAndroidPlugin() {
        AndroidPluginVersion currentVersion = androidComponentsExtension.getPluginVersion();
        AndroidPluginVersion minimumVersion = new AndroidPluginVersion(7, 2);
        if (currentVersion.compareTo(minimumVersion) < 0) {
            throw new IllegalStateException("ByteBuddy is supported from Android Gradle Plugin version 7.2+");
        }
    }
}