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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.GradleException;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.lang.reflect.Method;

/**
 * Implements a configuration of a Byte Buddy task.
 */
public class ByteBuddyTaskConfiguration extends AbstractByteBuddyTaskConfiguration<ByteBuddyTask, ByteBuddyTaskExtension> {

    /**
     * The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
     */
    private final Method getDestinationDirectorySource;

    /**
     * The {@code org.gradle.api.file.AbstractCompile#getDestinationDirectory} method.
     */
    private final Method getDestinationDirectoryTarget;

    /**
     * Creates a new Byte Buddy task configuration.
     *
     * @param name                          The name of the task.
     * @param sourceSet                     The source set for which the task chain is being configured.
     * @param getDestinationDirectorySource The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
     * @param getDestinationDirectoryTarget The {@code org.gradle.api.file.AbstractCompile#getDestinationDirectory} method.
     */
    public ByteBuddyTaskConfiguration(String name, SourceSet sourceSet, Method getDestinationDirectorySource, Method getDestinationDirectoryTarget) {
        super(name, sourceSet);
        this.getDestinationDirectorySource = getDestinationDirectorySource;
        this.getDestinationDirectoryTarget = getDestinationDirectoryTarget;
    }

    @Override
    protected void configureDirectories(SourceDirectorySet source, AbstractCompile compileTask, ByteBuddyTask byteBuddyTask) {
        try {
            DirectoryProperty directory = (DirectoryProperty) getDestinationDirectorySource.invoke(source);
            ((DirectoryProperty) getDestinationDirectoryTarget.invoke(compileTask)).set(directory.dir("../"
                    + source.getName()
                    + RAW_FOLDER_SUFFIX));
            byteBuddyTask.getSource().set(directory.dir("../" + source.getName() + RAW_FOLDER_SUFFIX));
            byteBuddyTask.getTarget().set(directory);
            byteBuddyTask.getClassPath().from(compileTask.getClasspath());
        } catch (Exception exception) {
            throw new GradleException("Could not adjust directories for tasks", exception);
        }
    }
}
