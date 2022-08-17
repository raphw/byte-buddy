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
package net.bytebuddy.build.gradle.android.connector.adapter.current;

import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.api.ApplicationVariant;
import kotlin.Unit;
import net.bytebuddy.build.gradle.android.connector.adapter.TransformationAdapter;
import net.bytebuddy.build.gradle.android.connector.adapter.current.asm.ByteBuddyAsmClassVisitorFactory;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskContainer;

import java.util.Objects;

/**
 * Adapter for host projects using Android Gradle plugin version >= 7.2
 */
public class CurrentAdapter implements TransformationAdapter {

    private final BaseExtension androidExtension;
    private final AndroidComponentsExtension<?, ?, ?> androidComponentsExtension;
    private final Configuration byteBuddyDependenciesConfiguration;
    private final TaskContainer tasks;
    private final Project project;

    public CurrentAdapter(BaseExtension androidExtension, AndroidComponentsExtension<?, ?, ?> androidComponentsExtension, Configuration byteBuddyDependenciesConfiguration, TaskContainer tasks, Project project) {
        this.androidExtension = androidExtension;
        this.androidComponentsExtension = androidComponentsExtension;
        this.byteBuddyDependenciesConfiguration = byteBuddyDependenciesConfiguration;
        this.tasks = tasks;
        this.project = project;
    }

    @Override
    public void adapt(AndroidTransformation transformation) {
        androidComponentsExtension.onVariants(androidComponentsExtension.selector().all(), variant -> {
            variant.getInstrumentation().transformClassesWith(ByteBuddyAsmClassVisitorFactory.class, InstrumentationScope.ALL, params -> {
                params.getByteBuddyClasspath().from(byteBuddyDependenciesConfiguration);
                params.getAndroidBootClasspath().from(androidExtension.getBootClasspath());
                params.getClasspath().from(project.provider(() -> getRuntimeClasspath(variant.getName())));
                return Unit.INSTANCE;
            });
        });
    }

    private Configuration getRuntimeClasspath(String variantName) {
        AppExtension extension = (AppExtension) androidExtension;

        for (ApplicationVariant applicationVariant : extension.getApplicationVariants()) {
            if (Objects.equals(variantName, applicationVariant.getName())) {
                return applicationVariant.getRuntimeConfiguration();
            }
        }
        throw new RuntimeException("No runtimeconfig found");
    }
}