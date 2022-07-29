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

import com.android.build.api.artifact.MultipleArtifact;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import net.bytebuddy.build.gradle.android.connector.adapter.TransformationAdapter;
import net.bytebuddy.build.gradle.android.connector.adapter.current.task.TransformAppTask;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * Adapter for host projects using Android Gradle plugin version >= 7.2
 */
public class CurrentAdapter implements TransformationAdapter {

    private final BaseExtension androidExtension;
    private final AndroidComponentsExtension<?, ?, ?> androidComponentsExtension;
    private final Configuration byteBuddyDependenciesConfiguration;
    private final TaskContainer tasks;

    public CurrentAdapter(BaseExtension androidExtension, AndroidComponentsExtension<?, ?, ?> androidComponentsExtension, Configuration byteBuddyDependenciesConfiguration, TaskContainer tasks) {
        this.androidExtension = androidExtension;
        this.androidComponentsExtension = androidComponentsExtension;
        this.byteBuddyDependenciesConfiguration = byteBuddyDependenciesConfiguration;
        this.tasks = tasks;
    }

    @Override
    public void adapt(AndroidTransformation transformation) {
        androidComponentsExtension.onVariants(androidComponentsExtension.selector().all(), variant -> {
            TaskProvider<TransformAppTask> taskProvider = registerByteBuddyTransformTask(variant, transformation);
            variant.getArtifacts().use(taskProvider)
                    .wiredWith(TransformAppTask::getAllClasses, TransformAppTask::getOutput)
                    .toTransform(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE);
        });
    }

    private TaskProvider<TransformAppTask> registerByteBuddyTransformTask(
            Variant variant,
            AndroidTransformation transformation
    ) {
        TaskProvider<TransformAppTask> taskProvider = tasks.register(
                variant.getName() + "ByteBuddyTransformation",
                TransformAppTask.class,
                new TransformAppTask.Args(transformation, (AppExtension) androidExtension)
        );
        taskProvider.configure(it -> it.getBytebuddyDependencies().from(byteBuddyDependenciesConfiguration));
        return taskProvider;
    }
}