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
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * A Byte Buddy task implementation that does not use modern Gradle APIs.
 */
public class ByteBuddyDirTask extends AbstractByteBuddyTask {

    /**
     * The source dir.
     */
    private File source;

    /**
     * The target dir.
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
     * Creates a new simple Byte Buddy task.
     */
    @Inject
    @SuppressWarnings("this-escape")
    public ByteBuddyDirTask() {
        new ByteBuddyDirTaskExtension(null).configure(this);
    }

    /**
     * Returns the task's source dir.
     *
     * @return The task's source dir.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public File getSource() {
        return source;
    }

    /**
     * Sets the task's source dir.
     *
     * @param source The task's source dir.
     */
    public void setSource(File source) {
        this.source = source;
    }

    /**
     * Returns the task's target dir.
     *
     * @return The task's target dir.
     */
    @OutputDirectory
    public File getTarget() {
        return target;
    }

    /**
     * Sets the task's target dir.
     *
     * @param target The task's target dir.
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
        if (!getSource().equals(getTarget()) && deleteRecursively(getTarget())) {
            getLogger().debug("Deleted target dir {}", getTarget());
        }
        ArrayList<File> files = new ArrayList<File>(Collections.singleton(getSource()));
        File candidate;
        do {
            candidate = files.remove(0);
            File[] file = candidate.listFiles();
            if (file != null) {
                files.addAll(Arrays.asList(file));
            } else {
                Path relative = getSource().toPath().relativize(candidate.toPath());
                File targetCandidate = getTarget().toPath().resolve(relative).toFile();
                getLogger().debug("Created dir " + targetCandidate.getParent() + ": " + targetCandidate.getParentFile().mkdirs());
                getLogger().debug("Created file " + targetCandidate + ": " + targetCandidate.createNewFile());
                doApply(new Plugin.Engine.Source.ForJarFile(candidate), new Plugin.Engine.Target.ForJarFile(targetCandidate));
            }
        } while (!files.isEmpty());
    }
}
