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
import org.gradle.api.GradleException;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Implements a configuration of a simple Byte Buddy task.
 */
public class ByteBuddySimpleTaskConfiguration extends AbstractByteBuddyTaskConfiguration<ByteBuddySimpleTask, ByteBuddySimpleTaskExtension> {

    /**
     * The {@code org.gradle.api.tasks.compile.AbstractCompile#getDestinationDir} method or {@code null} if not available.
     */
    @MaybeNull
    private static final Method GET_DESTINATION_DIR;

    /**
     * The {@code org.gradle.api.tasks.compile.AbstractCompile#setDestinationDir(File)} method or {@code null} if not available.
     */
    @MaybeNull
    private static final Method SET_DESTINATION_DIR;

    /*
     * Resolves destination dir getter method if available.
     */
    static {
        Method getDestinationDir, setDestinationDir;
        try {
            getDestinationDir = AbstractCompile.class.getMethod("getDestinationDir");
            setDestinationDir = AbstractCompile.class.getMethod("setDestinationDir", File.class);
        } catch (Exception ignored) {
            getDestinationDir = null;
            setDestinationDir = null;
        }
        GET_DESTINATION_DIR = getDestinationDir;
        SET_DESTINATION_DIR = setDestinationDir;
    }

    /**
     * Creates a new simple Byte Buddy task configuration.
     *
     * @param name      The name of the task.
     * @param sourceSet The source set for which the task chain is being configured.
     */
    public ByteBuddySimpleTaskConfiguration(String name, SourceSet sourceSet) {
        super(name, sourceSet);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void configureDirectories(SourceDirectorySet source, AbstractCompile compileTask, ByteBuddySimpleTask byteBuddyTask) {
        if (GET_DESTINATION_DIR == null || SET_DESTINATION_DIR == null) {
            throw new GradleException("Cannot use simple configuration on Gradle version that does not support direct destination directory resolution");
        }
        try {
            File raw = new File((File) GET_DESTINATION_DIR.invoke(compileTask), "../" + source.getName() + RAW_FOLDER_SUFFIX).getCanonicalFile(), processed = (File) GET_DESTINATION_DIR.invoke(compileTask);
            SET_DESTINATION_DIR.invoke(compileTask, raw);
            byteBuddyTask.setSource(raw);
            byteBuddyTask.setTarget(processed);
            byteBuddyTask.setClassPath(compileTask.getClasspath());
        } catch (Exception exception) {
            throw new GradleException("Could not resolve raw class folder", exception);
        }
    }
}
