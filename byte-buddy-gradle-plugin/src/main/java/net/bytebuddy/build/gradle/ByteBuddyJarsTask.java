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
import net.bytebuddy.utility.QueueFactory;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Queue;

/**
 * A Byte Buddy task implementation that instruments multiple jars within a folder.
 */
public class ByteBuddyJarsTask extends AbstractByteBuddyTask {

    /**
     * The source directory containing the jars.
     */
    private File source;

    /**
     * The target directory to write the instrumented jars to.
     */
    private File target;

    /**
     * The class path to supply to the plugin engine.
     */
    private Iterable<File> classPath;

    /**
     * A set of classes that is used for discovery of plugins.
     */
    @MaybeNull
    private Iterable<File> discoverySet;

    /**
     * Creates a new task for instrumenting multiple jars.
     */
    @Inject
    @SuppressWarnings("this-escape")
    public ByteBuddyJarsTask() {
        new ByteBuddyJarsTaskExtension(null).configure(this);
    }

    /**
     * Returns the source directory containing the jars.
     *
     * @return The source directory containing the jars.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public File getSource() {
        return source;
    }

    /**
     * Sets the source directory containing the jars.
     *
     * @param source Returns the source directory containing the jars.
     */
    public void setSource(File source) {
        this.source = source;
    }

    /**
     * Returns the target directory to write the instrumented jars to.
     *
     * @return The target directory to write the instrumented jars to.
     */
    @OutputDirectory
    public File getTarget() {
        return target;
    }

    /**
     * Sets the target directory to write the instrumented jars to.
     *
     * @param target The target directory to write the instrumented jars to.
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

    /**
     * Returns the source set to resolve plugin names from or {@code null} if no such source set is used.
     *
     * @return The source set to resolve plugin names from or {@code null} if no such source set is used.
     */
    @MaybeNull
    @InputFiles
    @Optional
    public Iterable<File> getDiscoverySet() {
        return discoverySet;
    }

    /**
     * Defines the source set to resolve plugin names from or {@code null} if no such source set is used.
     *
     * @param discoverySet The source set to resolve plugin names from or {@code null} if no such source set is used.
     */
    public void setDiscoverySet(@MaybeNull Iterable<File> discoverySet) {
        this.discoverySet = discoverySet;
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

    @Override
    @MaybeNull
    protected Iterable<File> discoverySet() {
        return discoverySet;
    }

    /**
     * Applies this task.
     *
     * @throws IOException If an I/O exception is thrown.
     */
    @TaskAction
    public void apply() throws IOException {
        File source = getSource().getAbsoluteFile(), target = getTarget().getAbsoluteFile();
        if (!source.equals(getTarget()) && deleteRecursively(getTarget())) {
            getLogger().debug("Deleted target directory {}", getTarget());
        }
        Queue<File> queue = QueueFactory.make(Collections.singletonList(source));
        while (!queue.isEmpty()) {
            File candidate = queue.remove();
            File[] file = candidate.listFiles();
            if (file != null) {
                queue.addAll(Arrays.asList(file));
            } else {
                if (!candidate.getAbsoluteFile().toString().startsWith(source.toString())) {
                    throw new IllegalStateException(candidate + " is not a subdirectory of " + source);
                }
                File resolved = new File(target, candidate.toString().substring(source.toString().length()));
                if (resolved.getParentFile().mkdirs()) {
                    getLogger().debug("Created host directory for {}", resolved);
                }
                getLogger().debug("Transforming {} to {}", candidate, resolved);
                doApply(new Plugin.Engine.Source.ForJarFile(candidate), new Plugin.Engine.Target.ForJarFile(resolved));
            }
        }
    }
}
