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