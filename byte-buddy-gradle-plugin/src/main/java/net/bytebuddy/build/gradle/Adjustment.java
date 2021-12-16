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

import net.bytebuddy.utility.nullability.MaybeNull;
import java.util.*;

/**
 * Determines what tasks are considered for resolving compile task dependencies. If a compile task is a
 * direct dependency within several modules, it might be necessary to inspect a larger section of the
 * dependency graph. This is however not always legal depending on the configuration.
 */
public enum Adjustment {

    /**
     * Resolves all tasks recursively with basis on the root project.
     */
    FULL {
        @Override
        protected Iterable<Task> resolve(Project project, TaskExecutionGraph graph) {
            return new CompoundIterable(project.getRootProject().getAllTasks(true).values());
        }
    },

    /**
     * Resolves all tasks recursively with basis on the compile task's project, including its subprojects.
     */
    SUB {
        @Override
        protected Iterable<Task> resolve(Project project, TaskExecutionGraph graph) {
            return new CompoundIterable(project.getAllTasks(true).values());
        }
    },

    /**
     * Resolves only tasks declared by the compile task's project, excluding its subprojects.
     */
    SELF {
        @Override
        protected Iterable<Task> resolve(Project project, TaskExecutionGraph graph) {
            return new CompoundIterable(project.getAllTasks(false).values());
        }
    },

    /**
     * Resolves only active tasks of the current execution as determined by the {@link TaskExecutionGraph}.
     */
    ACTIVE {
        @Override
        protected Iterable<Task> resolve(Project project, TaskExecutionGraph graph) {
            return graph.getAllTasks();
        }
    },

    /**
     * Does not resolve any tasks.
     */
    NONE {
        @Override
        protected Iterable<Task> resolve(Project project, TaskExecutionGraph graph) {
            return Collections.<Task>emptySet();
        }
    };

    /**
     * Resolves the task dependencies per project.
     *
     * @param project The project of the compile task.
     * @param graph   The task execution graph.
     * @return An iterable of all tasks to adjust.
     */
    protected abstract Iterable<Task> resolve(Project project, TaskExecutionGraph graph);

    /**
     * An error handler to react on failures to adjust the task graph.
     */
    public enum ErrorHandler {

        /**
         * An error handler that fails the build with the original exception upon an adjustment error.
         */
        FAIL {
            @Override
            protected void apply(Project project, String name, Task task, RuntimeException exception) {
                throw exception;
            }
        },

        /**
         * An error handler that logs a warning upon an adjustment error.
         */
        WARN {
            @Override
            protected void apply(Project project, String name, Task task, RuntimeException exception) {
                project.getLogger().warn("Failed to resolve potential dependency for task '{}' of project '{}' on '{}' of project '{}' - dependency must be declared manually if appropriate",
                        task.getName(),
                        task.getProject().getName(),
                        name,
                        project.getName(),
                        exception);
            }
        },

        /**
         * An error handler that ignores any adjustment errors.
         */
        IGNORE {
            @Override
            protected void apply(Project project, String name, Task task, RuntimeException exception) {
                /* do nothing */
            }
        };

        /**
         * Applies this handler upon an exception caused by an adjustment.
         *
         * @param project   The project that applies the Byte Buddy task.
         * @param name      The name of the Byte Buddy task.
         * @param task      The task being adjusted or being investigated for adjustment.
         * @param exception The exception that was yielded during adjustment.
         */
        protected abstract void apply(Project project, String name, Task task, RuntimeException exception);
    }

    /**
     * An {@link Iterable} that concatenates multiple iterables of {@link Task}s.
     */
    protected static class CompoundIterable implements Iterable<Task> {

        /**
         * The iterables to consider.
         */
        private final Collection<? extends Iterable<? extends Task>> iterables;

        /**
         * Creates a compound iterable.
         *
         * @param iterables The iterables to consider.
         */
        protected CompoundIterable(Collection<? extends Iterable<? extends Task>> iterables) {
            this.iterables = iterables;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<Task> iterator() {
            return new CompoundIterator(new ArrayList<Iterable<? extends Task>>(iterables));
        }

        /**
         * An {@link Iterator} that concatenates multiple iterables of {@link Task}s.
         */
        protected static class CompoundIterator implements Iterator<Task> {

            /**
             * The current iterator or {@code null} if no such iterator is defined.
             */
            @MaybeNull
            private Iterator<? extends Task> current;

            /**
             * A backlog of iterables to still consider.
             */
            private final List<Iterable<? extends Task>> backlog;

            /**
             * Creates a compound iterator.
             *
             * @param iterables The iterables to consider.
             */
            protected CompoundIterator(List<Iterable<? extends Task>> iterables) {
                backlog = iterables;
                forward();
            }

            /**
             * {@inheritDoc}
             */
            public boolean hasNext() {
                return current != null && current.hasNext();
            }

            /**
             * {@inheritDoc}
             */
            public Task next() {
                try {
                    if (current != null) {
                        return current.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                } finally {
                    forward();
                }
            }

            /**
             * Forwards the iterator to the next relevant iterable.
             */
            private void forward() {
                while ((current == null || !current.hasNext()) && !backlog.isEmpty()) {
                    current = backlog.remove(0).iterator();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        }
    }

    /**
     * A non-operational post processor.
     */
    protected enum NoOpPostProcessor implements Action<Task> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public void execute(Task task) {
            /* do nothing */
        }
    }
}
