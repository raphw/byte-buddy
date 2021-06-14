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

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.Map;
import java.util.Set;

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
