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
package net.bytebuddy.build.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.Map;
import java.util.Set;

/**
 * An abstract configuration for a Byte Buddy task and extension.
 *
 * @param <T> The Byte Buddy task type.
 * @param <S> The Byte Buddy extension type.
 */
public abstract class AbstractByteBuddyTaskConfiguration<
        T extends AbstractByteBuddyTask,
        S extends AbstractByteBuddyTaskExtension<T>> implements Action<Project> {

    /**
     * The relative path to the raw folder.
     */
    protected static final String RAW_FOLDER = "../raw";

    /**
     * The name of the task.
     */
    private final String name;

    /**
     * The source set for which the task chain is being configured.
     */
    private final SourceSet sourceSet;

    /**
     * The type of the Byte Buddy task.
     */
    private final Class<T> type;

    /**
     * Creates a new abstract Byte Buddy task configuration.
     *
     * @param name      The name of the task.
     * @param sourceSet The source set for which the task chain is being configured.
     * @param type      The type of the Byte Buddy task.
     */
    protected AbstractByteBuddyTaskConfiguration(String name, SourceSet sourceSet, Class<T> type) {
        this.name = name;
        this.sourceSet = sourceSet;
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Project project) {
        @SuppressWarnings("unchecked")
        S extension = (S) project.getExtensions().getByName(name);
        if (extension.getTransformations().isEmpty()) {
            project.getLogger().debug("Not configuring task for source set '{}' as no transformations are defined", sourceSet.getName());
        } else {
            project.getLogger().debug("Configuring Byte Buddy task for source set '{}' as '{}'", sourceSet.getName(), name);
            JavaCompile compileTask = (JavaCompile) project.getTasks().getByName(sourceSet.getCompileJavaTaskName());
            T byteBuddyTask = project.getTasks().create(name, type);
            byteBuddyTask.setGroup("Byte Buddy");
            byteBuddyTask.setDescription("Transforms the classes compiled by " + compileTask.getName());
            byteBuddyTask.dependsOn(compileTask);
            extension.configure(byteBuddyTask);
            configureDirectories(sourceSet.getJava(), compileTask, byteBuddyTask);
            for (Map.Entry<Project, Set<Task>> entry : project.getRootProject().getAllTasks(true).entrySet()) {
                for (Task task : entry.getValue()) {
                    if (!(task.getName().equals(name)
                            && task.getProject().equals(project))
                            && task.getTaskDependencies().getDependencies(task).contains(compileTask)) {
                        task.dependsOn(byteBuddyTask);
                        project.getLogger().debug("Altered task '{}' of project '{}' to depend on '{}' of project '{}'",
                                task.getName(),
                                entry.getKey().getName(),
                                name,
                                project.getName());
                    }
                }
            }
        }
    }

    /**
     * Configures the directories of the compile and Byte Buddy tasks.
     *
     * @param source        The source directory set.
     * @param compileTask   The compile task.
     * @param byteBuddyTask The Byte Buddy task.
     */
    protected abstract void configureDirectories(SourceDirectorySet source, JavaCompile compileTask, T byteBuddyTask);
}
