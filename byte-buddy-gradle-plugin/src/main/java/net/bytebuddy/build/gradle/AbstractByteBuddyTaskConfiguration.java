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
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

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
     * Creates a new abstract Byte Buddy task configuration.
     *
     * @param name      The name of the task.
     * @param sourceSet The source set for which the task chain is being configured.
     */
    protected AbstractByteBuddyTaskConfiguration(String name, SourceSet sourceSet) {
        this.name = name;
        this.sourceSet = sourceSet;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(final Project project) {
        @SuppressWarnings("unchecked") final S extension = (S) project.getExtensions().getByName(name);
        if (extension.getTransformations().isEmpty()) {
            project.getLogger().debug("Not configuring task for source set '{}' as no transformations are defined", sourceSet.getName());
        } else {
            project.getLogger().debug("Configuring Byte Buddy task for source set '{}' as '{}'", sourceSet.getName(), name);
            final JavaCompile compileTask = (JavaCompile) project.getTasks().getByName(sourceSet.getCompileJavaTaskName());
            final T byteBuddyTask = project.getTasks().create(name, extension.toType());
            byteBuddyTask.setGroup("Byte Buddy");
            byteBuddyTask.setDescription("Transforms the classes compiled by " + compileTask.getName());
            byteBuddyTask.dependsOn(compileTask);
            extension.configure(byteBuddyTask);
            configureDirectories(sourceSet.getJava(), compileTask, byteBuddyTask);
            Action<TaskExecutionGraph> action = new TaskExecutionGraphAdjustmentAction(project,
                    name,
                    extension.getAdjustment(),
                    extension.isStrict(),
                    byteBuddyTask,
                    compileTask);
            if (extension.isLazy()) {
                project.getGradle().getTaskGraph().whenReady(action);
            } else {
                action.execute(project.getGradle().getTaskGraph());
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

    /**
     * An action to adjust the task execution graph to depend on the injected Byte Buddy task if a task
     * depends on the compile task that is being enhanced.
     */
    protected static class TaskExecutionGraphAdjustmentAction implements Action<TaskExecutionGraph> {

        /**
         * The current project.
         */
        private final Project project;

        /**
         * The name of the task.
         */
        private final String name;

        /**
         * The adjustment to apply.
         */
        private final Adjustment adjustment;

        /**
         * If {@code true}, dependency resolution errors should result in a build error.
         */
        private final boolean strict;

        /**
         * The Byte Buddy task that is injected.
         */
        private final Task byteBuddyTask;

        /**
         * The compile task to which the Byte Buddy task is appended to.
         */
        private final Task compileTask;

        /**
         * Creates a new task execution graph adjustment action.
         *
         * @param project       The current project.
         * @param name          The name of the task.
         * @param adjustment    The adjustment to apply.
         * @param strict        If {@code true}, dependency resolution errors should result in a build error.
         * @param byteBuddyTask The Byte Buddy task that is injected.
         * @param compileTask   The compile task to which the Byte Buddy task is appended to.
         */
        protected TaskExecutionGraphAdjustmentAction(Project project,
                                                     String name,
                                                     Adjustment adjustment,
                                                     boolean strict,
                                                     Task byteBuddyTask,
                                                     Task compileTask) {
            this.project = project;
            this.name = name;
            this.adjustment = adjustment;
            this.byteBuddyTask = byteBuddyTask;
            this.compileTask = compileTask;
            this.strict = strict;
        }

        /**
         * {@inheritDoc}
         */
        public void execute(TaskExecutionGraph graph) {
            for (Task task : adjustment.resolve(project, graph)) {
                try {
                    if (!(task.getName().equals(name)
                            && task.getProject().equals(project))
                            && task.getTaskDependencies().getDependencies(task).contains(compileTask)) {
                        task.dependsOn(byteBuddyTask);
                        project.getLogger().debug("Altered task '{}' of project '{}' to depend on '{}' of project '{}'",
                                task.getName(),
                                task.getProject().getName(),
                                name,
                                project.getName());
                    }
                } catch (RuntimeException exception) {
                    if (strict) {
                        throw exception;
                    } else {
                        project.getLogger().warn("Failed to resolve potential dependency for task '{}' of project '{}' on '{}' of project '{}' - dependency must be declared manually if appropriate",
                                task.getName(),
                                task.getProject().getName(),
                                name,
                                project.getName(),
                                exception);
                    }
                }
            }
        }
    }
}
