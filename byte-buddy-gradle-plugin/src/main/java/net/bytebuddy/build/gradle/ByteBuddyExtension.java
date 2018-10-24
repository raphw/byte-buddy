/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Representation of a Gradle configuration for Byte Buddy.
 */
public class ByteBuddyExtension {

    /**
     * The current project.
     */
    private final Project project;

    /**
     * A list of registered transformations.
     */
    private final List<Transformation> transformations;

    /**
     * The initialization or {@code null} if no initialization was defined by a user.
     */
    private Initialization initialization;

    /**
     * The suffix to use for rebased methods or {@code null} if a random suffix should be used.
     */
    private String suffix;

    /**
     * {@code true} if the plugin should fail upon discovering a live runtime initializer.
     */
    private boolean failOnLiveInitializer;

    /**
     * {@code true} if this plugin should fail fast upon a plugin's failure.
     */
    private boolean failFast;

    /**
     * {@code true} if the debug information of class files should be parsed to extract parameter names.
     */
    private boolean extendedParsing;

    /**
     * A list of task names for which to apply a transformation or {@code null} if the task should apply to all tasks.
     */
    private Set<String> tasks;

    /**
     * Creates a new Byte Buddy extension.
     *
     * @param project The current project.
     */
    public ByteBuddyExtension(Project project) {
        this.project = project;
        transformations = new ArrayList<Transformation>();
        failOnLiveInitializer = true;
        failFast = true;
    }

    /**
     * Adds a transformation to apply.
     *
     * @param closure The closure for configuring the transformation.
     */
    public void transformation(Closure<?> closure) {
        transformations.add((Transformation) project.configure(new Transformation(project), closure));
    }

    /**
     * Adds an initialization to apply.
     *
     * @param closure The closure for configuring the initialization.
     */
    public void initialization(Closure<?> closure) {
        if (initialization != null) {
            throw new GradleException("Initialization is already set");
        }
        initialization = (Initialization) project.configure(new Initialization(), closure);
    }

    /**
     * Returns a list of transformations to apply.
     *
     * @return A list of transformations to apply.
     */
    public List<Transformation> getTransformations() {
        return transformations;
    }

    /**
     * Returns the initialization to use.
     *
     * @return The initialization to use.
     */
    public Initialization getInitialization() {
        return initialization == null
                ? Initialization.makeDefault()
                : initialization;
    }

    /**
     * Returns the method name transformer to use.
     *
     * @return The method name transformer to use.
     */
    public MethodNameTransformer getMethodNameTransformer() {
        return suffix == null || suffix.length() == 0
                ? MethodNameTransformer.Suffixing.withRandomSuffix()
                : new MethodNameTransformer.Suffixing(suffix);
    }

    /**
     * Sets the suffix to apply upon rebased methods.
     *
     * @param suffix The suffix to apply upon rebased methods.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Returns {@code true} if the build should fail upon discovering a live runtime initializer.
     *
     * @return {@code true} if the build should fail upon discovering a live runtime initializer.
     */
    public boolean isFailOnLiveInitializer() {
        return failOnLiveInitializer;
    }

    /**
     * Determines if the build should fail upon discovering a live runtime initializer.
     *
     * @param failOnLiveInitializer {@code true} if the build should fail upon discovering a live runtime initializer.
     */
    public void setFailOnLiveInitializer(boolean failOnLiveInitializer) {
        this.failOnLiveInitializer = failOnLiveInitializer;
    }

    /**
     * Returns {@code true} if this plugin should fail fast upon a plugin's failure.
     *
     * @return {@code true} if this plugin should fail fast upon a plugin's failure.
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Determines if this plugin should fail fast upon a plugin's failure.
     *
     * @param failFast {@code true} if this plugin should fail fast upon a plugin's failure.
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Returns {@code true} if the debug information of class files should be parsed to extract parameter names.
     *
     * @return {@code true} if the debug information of class files should be parsed to extract parameter names.
     */
    public boolean isExtendedParsing() {
        return extendedParsing;
    }

    /**
     * Determines if the debug information of class files should be parsed to extract parameter names.
     *
     * @param extendedParsing {@code true} if the debug information of class files should be parsed to extract parameter names.
     */
    public void setExtendedParsing(boolean extendedParsing) {
        this.extendedParsing = extendedParsing;
    }

    /**
     * Sets the initialization that should be used.
     *
     * @param initialization The initialization to be used.
     */
    public void setInitialization(Initialization initialization) {
        this.initialization = initialization;
    }

    /**
     * Determines if a task is subject to transformation.
     *
     * @param task The task to consider.
     * @return {@code true} if this task should be followed up by a transformation.
     */
    public boolean implies(Task task) {
        return tasks == null || tasks.contains(task.getName());
    }

    /**
     * Sets an explicit list of tasks for which a transformation should be applied.
     *
     * @param tasks The tasks to explicitly append a transformation to.
     */
    public void setTasks(Set<String> tasks) {
        this.tasks = tasks;
    }
}
