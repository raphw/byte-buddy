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
import net.bytebuddy.utility.nullability.UnknownNull;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.io.File;

/**
 * A Byte Buddy jars task extension.
 */
public class ByteBuddyJarsTaskExtension extends AbstractByteBuddyTaskExtension<ByteBuddyJarsTask> {

    /**
     * A set of classes that is used for discovery of plugins.
     */
    @MaybeNull
    private Iterable<File> discoverySet;

    /**
     * Creates a new Byte Buddy dir extension.
     *
     * @param project The current Gradle project.
     */
    @Inject
    public ByteBuddyJarsTaskExtension(@UnknownNull Project project) {
        super(project);
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
    protected boolean isEmptyDiscovery() {
        return discoverySet == null || !discoverySet.iterator().hasNext();
    }

    @Override
    protected void doConfigure(ByteBuddyJarsTask task) {
        task.setDiscoverySet(discoverySet);
    }

    @Override
    protected Class<ByteBuddyJarsTask> toType() {
        return ByteBuddyJarsTask.class;
    }

    @Override
    protected void discoverySet(FileCollection fileCollection) {
        discoverySet = fileCollection;
    }
}
