package net.bytebuddy.build.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.Map;
import java.util.Set;

/**
 * Determines what tasks are considered for resolving compile task dependencies. If a compile task is a
 * direct dependency within several modules, it might be necessary to inspect a larger section of the
 * dependency graph. This is however not always legal depending on the configuration.
 */
public enum Resolution {

    /**
     * Resolves all tasks recursively with basis on the root project.
     */
    FULL {
        @Override
        protected Map<Project, Set<Task>> resolve(Project project) {
            return project.getRootProject().getAllTasks(true);
        }
    },

    /**
     * Resolves all tasks recursively with basis on the compile task's project, including its subprojects.
     */
    SUB {
        @Override
        protected Map<Project, Set<Task>> resolve(Project project) {
            return project.getAllTasks(true);
        }
    },

    /**
     * Resolves only tasks declared by the compile task's project, excluding its subprojects.
     */
    SELF {
        @Override
        protected Map<Project, Set<Task>> resolve(Project project) {
            return project.getAllTasks(false);
        }
    };

    /**
     * Resolves the task dependencies per project.
     *
     * @param project The project of the compile task.
     * @return A map of projects to their defined tasks.
     */
    protected abstract Map<Project, Set<Task>> resolve(Project project);
}
