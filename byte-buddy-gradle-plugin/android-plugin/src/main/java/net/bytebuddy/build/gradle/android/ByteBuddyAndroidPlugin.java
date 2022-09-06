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
import com.android.build.api.component.impl.ComponentImpl;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.publishing.AndroidArtifacts;
import kotlin.Unit;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public class ByteBuddyAndroidPlugin implements Plugin<Project> {

    /**
     * {@inheritDoc}
     */
    public void apply(Project project) {
        BaseExtension baseExtension = project.getExtensions().getByType(BaseExtension.class);
        AndroidComponentsExtension<?, ?, ?> extension = project.getExtensions().getByType(AndroidComponentsExtension.class);
        if (extension.getPluginVersion().compareTo(new AndroidPluginVersion(7, 2)) < 0) {
            throw new IllegalStateException("Byte Buddy requires at least Gradle Plugin version 7.2+, but found " + extension.getPluginVersion());
        }
        ByteBuddyDependenciesHandler dependenciesHandler = ByteBuddyDependenciesHandler.of(project);
        extension.onVariants(extension.selector().all(), new VariantAction(project, extension));
    }

    protected static class VariantAction implements Action<Variant> {

        private final Project project;

        private final AndroidComponentsExtension<?, ?, ?> extension;

        protected VariantAction(Project project, AndroidComponentsExtension<?, ?, ?> extension) {
            this.project = project;
            this.extension = extension;
        }

        @Override
        public void execute(Variant variant) {
            TaskProvider<ByteBuddyCopyOutputTask> localClasses = project.getTasks().register(variant.getName() + "ByteBuddyLocalClasses",
                    ByteBuddyCopyOutputTask.class,
                    new ByteBuddyCopyOutputTask.ConfigurationAction(project, variant));
            Provider<ByteBuddyService> serviceProvider = project.getGradle().getSharedServices().registerIfAbsent(variant.getName() + "ByteBuddyService",
                    ByteBuddyService.class,
                    new ByteBuddyService.ConfigurationAction(extension));
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, params -> {
                params.getByteBuddyClasspath().from(dependenciesHandler.getConfigurationForBuildType(variant.getBuildType()));
                params.getAndroidBootClasspath().from(extension.getBootClasspath());
                ComponentImpl component = (ComponentImpl) variant;
                params.getRuntimeClasspath().from(component.getVariantDependencies().getArtifactFileCollection(
                        AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
                        AndroidArtifacts.ArtifactScope.ALL,
                        AndroidArtifacts.ArtifactType.CLASSES_JAR
                ));
                params.getLocalClassesDirectories().from(localClasses);
                params.getByteBuddyService().set(serviceProvider);
                return Unit.INSTANCE;
            });
        }
    }
}