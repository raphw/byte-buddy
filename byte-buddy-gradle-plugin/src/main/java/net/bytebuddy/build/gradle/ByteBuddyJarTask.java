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

import net.bytebuddy.build.Plugin;
import net.bytebuddy.build.gradle.api.CompileClasspath;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 * A Byte Buddy task implementation that does not use modern Gradle APIs.
 */
public class ByteBuddyJarTask extends AbstractByteBuddyTask {

    /**
     * The source jar.
     */
    private File source;

    /**
     * The target jar.
     */
    private File target;

    /**
     * The class path to supply to the plugin engine.
     */
    private Iterable<File> classPath;

    /**
     * Creates a new simple Byte Buddy task.
     */
    @Inject
    public ByteBuddyJarTask() {
        new ByteBuddyJarTaskExtension().configure(this);
    }

    /**
     * Returns the task's source jar.
     *
     * @return The task's source jar.
     */
    @InputFile
    public File getSource() {
        return source;
    }

    /**
     * Sets the task's source jar.
     *
     * @param source The task's source jar.
     */
    public void setSource(File source) {
        this.source = source;
    }

    /**
     * Returns the task's target jar.
     *
     * @return The task's target jar.
     */
    @OutputFile
    public File getTarget() {
        return target;
    }

    /**
     * Sets the task's target jar.
     *
     * @param target The task's target jar.
     */
    public void setTarget(File target) {
        this.target = target;
    }

    /**
     * Returns the class path to supply to the plugin engine.
     *
     * @return The class path to supply to the plugin engine.
     */
    @InputFiles
    @CompileClasspath
    public Iterable<File> getClassPath() {
        return classPath;
    }

    /**
     * Sets the class path to supply to the plugin engine.
     *
     * @param classPath The class path to supply to the plugin engine.
     */
    public void setClassPath(Iterable<File> classPath) {
        this.classPath = classPath;
    }

    @Override
    protected File source() {
        return getSource();
    }

    @Override
    protected File target() {
        return getTarget();
    }

    @Override
    protected Iterable<File> classPath() {
        return getClassPath();
    }

    /**
     * Applies this task.
     * @throws IOException If an I/O exception is thrown.
     */
    @TaskAction
    public void apply() throws IOException {
        if (!getSource().equals(getTarget()) && getProject().delete(getTarget())) {
            getLogger().debug("Deleted target jar {}", getTarget());
        }
        doApply(new Plugin.Engine.Source.ForJarFile(getSource()), new Plugin.Engine.Target.ForJarFile(getTarget()));
    }
}
