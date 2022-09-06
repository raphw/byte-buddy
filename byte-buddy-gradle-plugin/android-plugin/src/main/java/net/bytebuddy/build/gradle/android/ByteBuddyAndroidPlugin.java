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
import com.android.build.gradle.internal.publishing.AndroidArtifacts;
import kotlin.Unit;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public class ByteBuddyAndroidPlugin implements Plugin<Project> {

    @MaybeNull
    private AndroidComponentsExtension<?, ?, ?> androidComponentsExtension;
    private BaseExtension androidExtension;
    private Project project;

    /**
     * {@inheritDoc}
     */
    public void apply(Project project) {
        this.project = project;
        androidExtension = project.getExtensions().getByType(BaseExtension.class);
        androidComponentsExtension = project.getExtensions().findByType(AndroidComponentsExtension.class);
        verifyValidAndroidPlugin();
        ByteBuddyDependenciesHandler dependenciesHandler = ByteBuddyDependenciesHandler.of(project);
        registerBytebuddyAsmFactory(dependenciesHandler);
    }

    private void registerBytebuddyAsmFactory(ByteBuddyDependenciesHandler dependenciesHandler) {
        androidComponentsExtension.onVariants(androidComponentsExtension.selector().all(), variant -> {
            TaskProvider<ByteBuddyCopyOutputTask> localClasses = registerLocalClassesSyncTask(variant);
            Provider<ByteBuddyService> serviceProvider = registerService(variant);
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, params -> {
                params.getByteBuddyClasspath().from(dependenciesHandler.getConfigurationForBuildType(variant.getBuildType()));
                params.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
                params.getRuntimeClasspath().from(getRuntimeClasspath(variant));
                params.getLocalClassesDirectories().from(localClasses);
                params.getByteBuddyService().set(serviceProvider);
                return Unit.INSTANCE;
            });
        });
    }

    private Provider<ByteBuddyService> registerService(Variant variant) {
        return project.getGradle().getSharedServices().registerIfAbsent(variant.getName() + "BytebuddyService", ByteBuddyService.class, spec -> {
            spec.getParameters().getJavaTargetCompatibilityVersion().set(androidExtension.getCompileOptions().getTargetCompatibility());
        });
    }

    private TaskProvider<ByteBuddyCopyOutputTask> registerLocalClassesSyncTask(Variant variant) {
        return project.getTasks().register(variant.getName() + "ByteBuddyLocalClasses", ByteBuddyCopyOutputTask.class, classesSync -> {
            classesSync.getLocalClasspath().from(variant.getArtifacts().getAll(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE));
            classesSync.getOutputDir().set(project.getLayout().getBuildDirectory().dir("intermediates/incremental/" + classesSync.getName()));
        });
    }

    private FileCollection getRuntimeClasspath(Variant variant) {
        ComponentImpl component = (ComponentImpl) variant;
        return component.getVariantDependencies().getArtifactFileCollection(
                AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                AndroidArtifacts.ArtifactScope.ALL,
                AndroidArtifacts.ArtifactType.CLASSES_JAR
        );
    }

    private void verifyValidAndroidPlugin() {
        AndroidPluginVersion currentVersion = androidComponentsExtension.getPluginVersion();
        AndroidPluginVersion minimumVersion = new AndroidPluginVersion(7, 2);
        if (currentVersion.compareTo(minimumVersion) < 0) {
            throw new IllegalStateException("ByteBuddy is supported from Android Gradle Plugin version 7.2+");
        }
    }
}