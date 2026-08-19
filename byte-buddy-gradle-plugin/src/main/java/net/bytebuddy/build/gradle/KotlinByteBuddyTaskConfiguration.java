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

import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Configures a Byte Buddy task that transforms the output of a Kotlin compilation. Reuses the
 * Byte Buddy extension registered under {@code extensionName} by
 * {@link ByteBuddyPlugin.JavaPluginConfigurationAction} so a single {@code byteBuddy} DSL block
 * applies to both Java and Kotlin outputs.
 *
 * <p>If the Kotlin Gradle plugin is not on the classpath, or the source set has no Kotlin sources
 * or no {@code compile{SourceSet}Kotlin} task, this action is a no-op. Depends on the Kotlin
 * Gradle plugin API only via reflection so the Byte Buddy Gradle plugin remains usable in
 * Java-only projects.</p>
 */
public class KotlinByteBuddyTaskConfiguration implements Action<Project> {

    /**
     * The {@code org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool} interface, or
     * {@code null} if the Kotlin Gradle plugin API is not on the classpath.
     */
    @MaybeNull
    private static final Class<?> KOTLIN_COMPILE_TOOL;

    /**
     * The {@code KotlinCompileTool#getDestinationDirectory} method, or {@code null}.
     */
    @MaybeNull
    private static final Method GET_DESTINATION_DIRECTORY;

    /**
     * The {@code KotlinCompileTool#getLibraries} method, or {@code null}.
     */
    @MaybeNull
    private static final Method GET_LIBRARIES;

    /**
     * The {@code org.gradle.api.tasks.SourceSet#getExtensions} method, or {@code null} on legacy Gradle.
     */
    @MaybeNull
    private static final Method GET_EXTENSIONS;

    /**
     * The {@code org.gradle.api.file.SourceDirectorySet#getDestinationDirectory} method, or {@code null} on legacy Gradle.
     */
    @MaybeNull
    private static final Method GET_DESTINATION_DIRECTORY_SOURCE;

    /**
     * The {@code org.gradle.api.tasks.compile.AbstractCompile#getDestinationDirectory} method, or {@code null} on legacy Gradle.
     */
    @MaybeNull
    private static final Method GET_DESTINATION_DIRECTORY_TARGET;

    /**
     * The {@code org.gradle.api.tasks.TaskContainer#named} method, or {@code null} on legacy Gradle.
     */
    @MaybeNull
    private static final Method NAMED;

    /**
     * The {@code org.gradle.api.file.SourceDirectorySet#compiledBy} method, or {@code null} on legacy Gradle.
     */
    @MaybeNull
    private static final Method COMPILED_BY;

    /**
     * The {@code java.util.function.Function} type, or {@code null} on legacy JVMs.
     */
    @MaybeNull
    private static final Class<?> FUNCTION;

    /*
     * Resolves Kotlin Gradle plugin API entry points, if available.
     */
    static {
        Class<?> kotlinCompileTool;
        Method getDestinationDirectory, getLibraries;
        try {
            kotlinCompileTool = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool");
            getDestinationDirectory = kotlinCompileTool.getMethod("getDestinationDirectory");
            getLibraries = kotlinCompileTool.getMethod("getLibraries");
        } catch (Throwable ignored) {
            kotlinCompileTool = null;
            getDestinationDirectory = null;
            getLibraries = null;
        }
        Method getExtensions, getDestinationDirectorySource, getDestinationDirectoryTarget;
        try {
            getExtensions = SourceSet.class.getMethod("getExtensions");
            getDestinationDirectorySource = SourceDirectorySet.class.getMethod("getDestinationDirectory");
            getDestinationDirectoryTarget = AbstractCompile.class.getMethod("getDestinationDirectory");
        } catch (Throwable ignored) {
            getExtensions = null;
            getDestinationDirectorySource = null;
            getDestinationDirectoryTarget = null;
        }
        Method named, compiledBy;
        Class<?> function;
        try {
            function = Class.forName("java.util.function.Function");
            named = TaskContainer.class.getMethod("named", String.class);
            compiledBy = SourceDirectorySet.class.getMethod("compiledBy", Class.forName("org.gradle.api.tasks.TaskProvider"), function);
        } catch (Throwable ignored) {
            named = null;
            compiledBy = null;
            function = null;
        }
        KOTLIN_COMPILE_TOOL = kotlinCompileTool;
        GET_DESTINATION_DIRECTORY = getDestinationDirectory;
        GET_LIBRARIES = getLibraries;
        GET_EXTENSIONS = getExtensions;
        GET_DESTINATION_DIRECTORY_SOURCE = getDestinationDirectorySource;
        GET_DESTINATION_DIRECTORY_TARGET = getDestinationDirectoryTarget;
        NAMED = named;
        COMPILED_BY = compiledBy;
        FUNCTION = function;
    }

    /**
     * Returns {@code true} if the Kotlin Gradle plugin API is available on the classpath and the
     * running Gradle version exposes {@code SourceSet#getExtensions}, {@code SourceDirectorySet#getDestinationDirectory}
     * and {@code AbstractCompile#getDestinationDirectory}.
     *
     * @return {@code true} if Kotlin support can be wired.
     */
    public static boolean isAvailable() {
        return KOTLIN_COMPILE_TOOL != null
                && GET_EXTENSIONS != null
                && GET_DESTINATION_DIRECTORY_SOURCE != null
                && GET_DESTINATION_DIRECTORY_TARGET != null;
    }

    /**
     * The name of the shared Byte Buddy extension, as registered by
     * {@link ByteBuddyPlugin.JavaPluginConfigurationAction}.
     */
    private final String extensionName;

    /**
     * The source set for which the Kotlin Byte Buddy task is being configured.
     */
    private final SourceSet sourceSet;

    /**
     * Creates a new Kotlin Byte Buddy task configuration.
     *
     * @param extensionName The name of the shared Byte Buddy extension.
     * @param sourceSet     The source set for which the task chain is being configured.
     */
    protected KotlinByteBuddyTaskConfiguration(String extensionName, SourceSet sourceSet) {
        this.extensionName = extensionName;
        this.sourceSet = sourceSet;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Project project) {
        if (!isAvailable()) {
            return;
        }
        String kotlinCompileTaskName = sourceSet.getCompileTaskName("kotlin");
        Task compileTask = project.getTasks().findByName(kotlinCompileTaskName);
        if (compileTask == null || !KOTLIN_COMPILE_TOOL.isInstance(compileTask)) {
            project.getLogger().debug("Skipping Kotlin Byte Buddy configuration for source set '{}': no compatible Kotlin compile task named '{}'",
                    sourceSet.getName(), kotlinCompileTaskName);
            return;
        }
        Object kotlinSources;
        try {
            kotlinSources = ((ExtensionContainer) GET_EXTENSIONS.invoke(sourceSet)).findByName("kotlin");
        } catch (Exception exception) {
            throw new GradleException("Could not resolve extensions of source set " + sourceSet.getName(), exception);
        }
        if (!(kotlinSources instanceof SourceDirectorySet)) {
            project.getLogger().debug("Skipping Kotlin Byte Buddy configuration for source set '{}': no 'kotlin' SourceDirectorySet extension",
                    sourceSet.getName());
            return;
        }
        AbstractByteBuddyTaskExtension extension = (AbstractByteBuddyTaskExtension) project.getExtensions().findByName(extensionName);
        if (extension == null) {
            project.getLogger().debug("Skipping Kotlin Byte Buddy configuration for source set '{}': extension '{}' not registered",
                    sourceSet.getName(), extensionName);
            return;
        }
        if (extension.getTransformations().isEmpty() && (extension.getDiscovery() == Discovery.NONE || extension.isEmptyDiscovery())) {
            project.getLogger().debug("Not configuring Kotlin Byte Buddy task for source set '{}' as no transformations are defined and discovery is disabled",
                    sourceSet.getName());
            return;
        }
        String taskName = extensionName + "Kotlin";
        project.getLogger().debug("Configuring Byte Buddy Kotlin task for source set '{}' as '{}'", sourceSet.getName(), taskName);
        ByteBuddyTask byteBuddyTask = (ByteBuddyTask) project.getTasks().create(taskName, extension.toType());
        byteBuddyTask.setGroup("Byte Buddy");
        byteBuddyTask.setDescription("Transforms the classes compiled by " + compileTask.getName());
        byteBuddyTask.dependsOn(compileTask);
        extension.configure(byteBuddyTask);
        configureDirectories(project, (SourceDirectorySet) kotlinSources, compileTask, byteBuddyTask);
        Task compileJavaTask = project.getTasks().findByName(sourceSet.getCompileJavaTaskName());
        if (compileJavaTask instanceof AbstractCompile) {
            byteBuddyTask.dependsOn(compileJavaTask);
            try {
                byteBuddyTask.getClassPath().from(GET_DESTINATION_DIRECTORY_TARGET.invoke(compileJavaTask));
            } catch (Exception exception) {
                throw new GradleException("Could not resolve destination directory of " + compileJavaTask.getName(), exception);
            }
        }
        Task classesTask = project.getTasks().findByName(sourceSet.getClassesTaskName());
        if (classesTask != null) {
            classesTask.dependsOn(byteBuddyTask);
        }
    }

    /**
     * Mirrors the raw-directory redirection performed for Java compilations onto the Kotlin
     * compile task: the Kotlin compile task writes to a sibling {@code kotlinByteBuddyRaw}
     * folder which the Byte Buddy task reads and then writes back to the original destination.
     *
     * <p>As the Kotlin Gradle plugin binds the source directory set's classes directory to the
     * Kotlin compile task's destination directory, this binding is redirected onto the Byte Buddy
     * task to avoid that consumers such as the jar task resolve the untransformed classes.</p>
     *
     * @param project       The current project.
     * @param source        The Kotlin source directory set.
     * @param compileTask   The Kotlin compile task (a {@code KotlinCompileTool}).
     * @param byteBuddyTask The Byte Buddy task consuming the compilation output.
     */
    private static void configureDirectories(Project project, SourceDirectorySet source, Task compileTask, ByteBuddyTask byteBuddyTask) {
        try {
            DirectoryProperty directory = (DirectoryProperty) GET_DESTINATION_DIRECTORY_SOURCE.invoke(source);
            String rawPath = "../kotlin" + AbstractByteBuddyTaskConfiguration.RAW_FOLDER_SUFFIX;
            ((DirectoryProperty) GET_DESTINATION_DIRECTORY.invoke(compileTask)).set(directory.dir(rawPath));
            byteBuddyTask.getSource().set(directory.dir(rawPath));
            byteBuddyTask.getTarget().set(directory);
            byteBuddyTask.getClassPath().from((FileCollection) GET_LIBRARIES.invoke(compileTask));
            if (NAMED == null || COMPILED_BY == null || FUNCTION == null) {
                project.getLogger().debug("Not rebinding the classes directory of the Kotlin source set as the current Gradle version does not support it");
            } else {
                COMPILED_BY.invoke(source,
                        NAMED.invoke(project.getTasks(), byteBuddyTask.getName()),
                        Proxy.newProxyInstance(KotlinByteBuddyTaskConfiguration.class.getClassLoader(),
                                new Class<?>[]{FUNCTION},
                                new TargetDirectoryFunction(byteBuddyTask.getTarget())));
            }
        } catch (Exception exception) {
            throw new GradleException("Could not adjust directories for Kotlin Byte Buddy task", exception);
        }
    }

    /**
     * An invocation handler that implements {@code java.util.function.Function} to resolve a Byte
     * Buddy task's target directory, without requiring a compile-time dependency on Java 8.
     */
    protected static class TargetDirectoryFunction implements InvocationHandler {

        /**
         * The target directory of the represented Byte Buddy task.
         */
        private final DirectoryProperty target;

        /**
         * Creates a new function that resolves a Byte Buddy task's target directory.
         *
         * @param target The target directory of the represented Byte Buddy task.
         */
        protected TargetDirectoryFunction(DirectoryProperty target) {
            this.target = target;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object proxy, Method method, @MaybeNull Object[] argument) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, argument);
            } else if (method.getName().equals("apply")) {
                return target;
            } else {
                throw new UnsupportedOperationException("Unexpected method: " + method);
            }
        }
    }

}
