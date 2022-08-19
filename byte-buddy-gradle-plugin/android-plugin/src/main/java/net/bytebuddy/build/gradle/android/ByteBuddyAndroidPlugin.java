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
import net.bytebuddy.build.gradle.android.service.BytebuddyService;
import net.bytebuddy.build.gradle.android.utils.AarGradleTransform;
import net.bytebuddy.build.gradle.android.utils.BytebuddyDependenciesHandler;
import net.bytebuddy.build.gradle.android.utils.LocalClassesSync;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.util.Objects;

import static net.bytebuddy.build.gradle.android.utils.BytebuddyDependenciesHandler.ARTIFACT_TYPE_ATTR;
import static net.bytebuddy.build.gradle.android.utils.BytebuddyDependenciesHandler.BYTEBUDDY_JAR_TYPE;

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
        registerAarToJarTransformation();
        registerBytebuddyJarRule();
    }

    private void registerBytebuddyAsmFactory(BytebuddyDependenciesHandler dependenciesHandler) {
        androidComponentsExtension.onVariants(androidComponentsExtension.selector().all(), variant -> {
            TaskProvider<LocalClassesSync> localClasses = registerLocalClassesSyncTask(variant);
            Provider<BytebuddyService> serviceProvider = project.getGradle().getSharedServices().registerIfAbsent(variant.getName() + "BytebuddyService", BytebuddyService.class, spec -> {
                BytebuddyService.Params parameters = spec.getParameters();
                parameters.getByteBuddyClasspath().from(dependenciesHandler.getConfigurationForBuildType(variant.getBuildType()));
                parameters.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
                parameters.getRuntimeClasspath().from(getRuntimeClasspath(variant));
                parameters.getLocalClassesDirs().from(localClasses);
            });
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, params -> {
                params.getBytebuddyService().set(serviceProvider);
                return Unit.INSTANCE;
            });
        });
    }

    private TaskProvider<LocalClassesSync> registerLocalClassesSyncTask(Variant variant) {
        return project.getTasks().register(variant.getName() + "ByteBuddyLocalClasses", LocalClassesSync.class, classesSync -> {
            classesSync.getLocalClasspath().from(variant.getArtifacts().getAll(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE));
            classesSync.getOutputDir().set(project.getLayout().getBuildDirectory().dir("intermediates/incremental/" + classesSync.getName()));
        });
    }

    private FileCollection getRuntimeClasspath(Variant variant) {
        ComponentImpl component = (ComponentImpl) variant;
        return component.getVariantDependencies().getRuntimeClasspath();
    }

    private void registerAarToJarTransformation() {
        project.getDependencies().registerTransform(AarGradleTransform.class, it -> {
            it.getFrom().attribute(ARTIFACT_TYPE_ATTR, "aar");
            it.getTo().attribute(ARTIFACT_TYPE_ATTR, BYTEBUDDY_JAR_TYPE);
        });
    }

    private void registerBytebuddyJarRule() {
        project.getDependencies().getAttributesSchema().attribute(ARTIFACT_TYPE_ATTR, stringAttributeMatchingStrategy -> {
            stringAttributeMatchingStrategy.getCompatibilityRules().add(BytebuddyJarsRule.class);
        });
    }

    public abstract static class BytebuddyJarsRule implements AttributeCompatibilityRule<String> {
        @Override
        public void execute(CompatibilityCheckDetails<String> details) {
            if (Objects.equals(details.getConsumerValue(), BYTEBUDDY_JAR_TYPE) && Objects.equals(details.getProducerValue(), "jar")) {
                details.compatible();
            }
        }
    }

    private void verifyValidAndroidPlugin() {
        AndroidPluginVersion currentVersion = androidComponentsExtension.getPluginVersion();
        AndroidPluginVersion minimumVersion = new AndroidPluginVersion(7, 2);
        if (currentVersion.compareTo(minimumVersion) < 0) {
            throw new IllegalStateException("ByteBuddy is supported from Android Gradle Plugin version 7.2+");
        }
    }
}