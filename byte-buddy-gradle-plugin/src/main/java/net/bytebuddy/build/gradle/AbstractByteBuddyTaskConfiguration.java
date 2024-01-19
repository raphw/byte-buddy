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

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;

import net.bytebuddy.utility.nullability.MaybeNull;

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
     * The folder name suffix for untransformed classes.
     */
    protected static final String RAW_FOLDER_SUFFIX = "ByteBuddyRaw";

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
    public void execute(Project project) {
        @SuppressWarnings("unchecked")
        S extension = (S) project.getExtensions().getByName(name);
        if (extension.getTransformations().isEmpty() && (extension.getDiscovery() == Discovery.NONE || extension.isEmptyDiscovery())) {
            project.getLogger().debug("Not configuring task for source set '{}' as no transformations are defined and discovery is disabled", sourceSet.getName());
        } else {
            project.getLogger().debug("Configuring Byte Buddy task for source set '{}' as '{}'", sourceSet.getName(), name);
            AbstractCompile compileTask = (AbstractCompile) project.getTasks().getByName(sourceSet.getCompileJavaTaskName());
            T byteBuddyTask = project.getTasks().create(name, extension.toType());
            byteBuddyTask.setGroup("Byte Buddy");
            byteBuddyTask.setDescription("Transforms the classes compiled by " + compileTask.getName());
            byteBuddyTask.dependsOn(compileTask);
            extension.configure(byteBuddyTask);
            configureDirectories(sourceSet.getJava(), compileTask, byteBuddyTask);
            Action<TaskExecutionGraph> action = new TaskExecutionGraphAdjustmentAction(project,
                    name,
                    extension.getAdjustment(),
                    extension.getAdjustmentErrorHandler(),
                    extension.getAdjustmentPostProcessor(),
                    byteBuddyTask,
                    compileTask);
            if (extension.isLazy()) {
                project.getGradle().getTaskGraph().whenReady(new TaskExecutionGraphClosure(action, project.getGradle().getTaskGraph()));
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
    protected abstract void configureDirectories(SourceDirectorySet source, AbstractCompile compileTask, T byteBuddyTask);

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
         * An error handler if an adjustment cannot be applied.
         */
        private final Adjustment.ErrorHandler adjustmentErrorHandler;

        /**
         * A post processor to adjust the task graph.
         */
        private final Action<Task> adjustmentPostProcessor;

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
         * @param project                 The current project.
         * @param name                    The name of the task.
         * @param adjustment              The adjustment to apply.
         * @param adjustmentErrorHandler  An error handler if an adjustment cannot be applied.
         * @param adjustmentPostProcessor A post processor to adjust the task graph.
         * @param byteBuddyTask           The Byte Buddy task that is injected.
         * @param compileTask             The compile task to which the Byte Buddy task is appended to.
         */
        protected TaskExecutionGraphAdjustmentAction(Project project,
                                                     String name,
                                                     Adjustment adjustment,
                                                     Adjustment.ErrorHandler adjustmentErrorHandler,
                                                     Action<Task> adjustmentPostProcessor,
                                                     Task byteBuddyTask,
                                                     Task compileTask) {
            this.project = project;
            this.name = name;
            this.adjustment = adjustment;
            this.adjustmentErrorHandler = adjustmentErrorHandler;
            this.adjustmentPostProcessor = adjustmentPostProcessor;
            this.byteBuddyTask = byteBuddyTask;
            this.compileTask = compileTask;
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
                    adjustmentErrorHandler.apply(project, name, task, exception);
                }
            }
            adjustmentPostProcessor.execute(byteBuddyTask);
        }
    }

    /**
     * A closure to execute an action on the {@link TaskExecutionGraph}. Older Gradle versions do not offer an overloaded method that accepts an
     * action such that a dispatch requires an explicit wrapping with a {@link Closure}.
     */
    @SuppressWarnings("serial")
    protected static class TaskExecutionGraphClosure extends Closure<Void> {

        /**
         * The serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The action to execute.
         */
        private final Action<TaskExecutionGraph> action;

        /**
         * The task execution graph to use.
         */
        private final TaskExecutionGraph taskExecutionGraph;

        /**
         * Creates a new closure for executing an action on the task execution graph.
         *
         * @param action             The action to execute.
         * @param taskExecutionGraph The task execution graph to use.
         */
        protected TaskExecutionGraphClosure(Action<TaskExecutionGraph> action, TaskExecutionGraph taskExecutionGraph) {
            super(null);
            this.action = action;
            this.taskExecutionGraph = taskExecutionGraph;
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        public Void call(Object... argument) {
            action.execute(taskExecutionGraph);
            return null;
        }
    }
}
