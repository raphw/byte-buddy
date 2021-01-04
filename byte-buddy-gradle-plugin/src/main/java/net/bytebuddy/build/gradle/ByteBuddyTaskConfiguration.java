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

import net.bytebuddy.build.gradle.api.Directory;
import net.bytebuddy.build.gradle.api.DirectoryProperty;
import org.gradle.api.GradleException;
import org.gradle.api.Transformer;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Implements a configuration of a Byte Buddy task.
 */
public class ByteBuddyTaskConfiguration extends AbstractByteBuddyTaskConfiguration<ByteBuddyTask, ByteBuddyTaskExtension> {

    /**
     * The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
     */
    private final Method getDestinationDirectory;

    /**
     * The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
     */
    private final Method setDestinationDir;

    /**
     * Creates a new Byte Buddy task configuration.
     *
     * @param name                    The name of the task.
     * @param sourceSet               The source set for which the task chain is being configured.
     * @param getDestinationDirectory The {@code org.gradle.api.file.SourceSetDirectory#getDestinationDirectory} method.
     * @param setDestinationDir       The {@code org.gradle.api.tasks.compile.AbstractCompile#setDestinationDir} method.
     */
    public ByteBuddyTaskConfiguration(String name, SourceSet sourceSet, Method getDestinationDirectory, Method setDestinationDir) {
        super(name, sourceSet, ByteBuddyTask.class);
        this.getDestinationDirectory = getDestinationDirectory;
        this.setDestinationDir = setDestinationDir;
    }

    @Override
    protected void configureDirectories(SourceDirectorySet source, JavaCompile compileTask, ByteBuddyTask byteBuddyTask) {
        try {
            DirectoryProperty directory = (DirectoryProperty) getDestinationDirectory.invoke(source);
            setDestinationDir.invoke(compileTask, directory.dir(RAW_FOLDER).map(ToFileMapper.INSTANCE));
            byteBuddyTask.getSource().set(directory.dir(RAW_FOLDER));
            byteBuddyTask.getTarget().set(directory);
            byteBuddyTask.getClassPath().from(compileTask.getClasspath());
        } catch (Exception exception) {
            throw new GradleException("Could not adjust directories for tasks", exception);
        }
    }

    /**
     * Transforms a directory to a file.
     */
    protected enum ToFileMapper implements Transformer<File, Directory> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public File transform(Directory directory) {
            return directory.getAsFile();
        }
    }
}
