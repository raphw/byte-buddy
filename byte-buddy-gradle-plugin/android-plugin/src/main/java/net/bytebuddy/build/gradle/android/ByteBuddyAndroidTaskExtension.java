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
package net.bytebuddy.build.gradle.android;

import groovy.lang.Closure;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * A Byte Buddy task extension for Android.
 */
public class ByteBuddyAndroidTaskExtension {

    /**
     * The current Gradle project.
     */
    private final Project project;

    /**
     * The transformations to apply.
     */
    private final List<Transformation> transformations;

    /**
     * The entry point to use.
     */
    private EntryPoint entryPoint;

    /**
     * The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
    private String suffix;

    /**
     * {@code true} if the transformation should fail if a live initializer is used.
     */
    private boolean failOnLiveInitializer;

    /**
     * {@code true} if a warning should be issued for an empty type set.
     */
    private boolean warnOnEmptyTypeSet;

    /**
     * {@code true} if the transformation should fail fast.
     */
    private boolean failFast;

    /**
     * {@code true} if extended parsing should be used.
     */
    private boolean extendedParsing;

    /**
     * Determines if the build should discover Byte Buddy build plugins on this Maven plugin's class loader.
     * Discovered plugins are stored by their name in the <i>/META-INF/net.bytebuddy/build.plugins</i> file
     * where each line contains the fully qualified class name. Discovered plugins are not provided with any
     * explicit constructor arguments.
     */
    private Discovery discovery;

    /**
     * The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    private int threads;

    /**
     * A set of classes that is used for discovery of plugins.
     */
    @MaybeNull
    private FileCollection discoverySet;

    /**
     * Creates a new abstract Byte Buddy task extension.
     *
     * @param project The current Gradle project.
     */
    public ByteBuddyAndroidTaskExtension(Project project) {
        this.project = project;
        transformations = new ArrayList<Transformation>();
        entryPoint = new EntryPoint.Unvalidated(EntryPoint.Default.DECORATE);
        suffix = "";
        failOnLiveInitializer = true;
        warnOnEmptyTypeSet = true;
        failFast = true;
        discovery = Discovery.EMPTY;
    }

    /**
     * Returns the transformations to apply.
     *
     * @return The transformations to apply.
     */
    public List<Transformation> getTransformations() {
        return transformations;
    }

    /**
     * Adds an additional transformation.
     *
     * @param closure The closure to configure the transformation.
     */
    public void transformation(Closure<Transformation> closure) {
        transformations.add((Transformation) project.configure(new Transformation(project), closure));
    }

    /**
     * Adds an additional transformation.
     *
     * @param action The action to configure the transformation.
     */
    public void transformation(Action<Transformation> action) {
        Transformation transformation = new Transformation(project);
        action.execute(transformation);
        transformations.add(transformation);
    }

    /**
     * Returns the entry point to use.
     *
     * @return The entry point to use.
     */
    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    /**
     * Sets the entry point to use.
     *
     * @param entryPoint The entry point to use.
     */
    public void setEntryPoint(EntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    /**
     * Returns the suffix to use for rebased methods or the empty string if a random suffix should be used.
     *
     * @return The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Sets the suffix to use for rebased methods or the empty string if a random suffix should be used.
     *
     * @param suffix The suffix to use for rebased methods or the empty string if a random suffix should be used.
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Returns {@code true} if the transformation should fail if a live initializer is used.
     *
     * @return {@code true} if the transformation should fail if a live initializer is used.
     */
    public boolean isFailOnLiveInitializer() {
        return failOnLiveInitializer;
    }

    /**
     * Determines if the transformation should fail if a live initializer is used.
     *
     * @param failOnLiveInitializer {@code true} if the transformation should fail if a live initializer is used.
     */
    public void setFailOnLiveInitializer(boolean failOnLiveInitializer) {
        this.failOnLiveInitializer = failOnLiveInitializer;
    }

    /**
     * Returns {@code true} if a warning should be issued for an empty type set.
     *
     * @return {@code true} if a warning should be issued for an empty type set.
     */
    public boolean isWarnOnEmptyTypeSet() {
        return warnOnEmptyTypeSet;
    }

    /**
     * Returns {@code true} if a warning should be issued for an empty type set.
     *
     * @param warnOnEmptyTypeSet {@code true} if a warning should be issued for an empty type set.
     */
    public void setWarnOnEmptyTypeSet(boolean warnOnEmptyTypeSet) {
        this.warnOnEmptyTypeSet = warnOnEmptyTypeSet;
    }

    /**
     * Returns {@code true} if a warning should be issued for an empty type set.
     *
     * @return {@code true} if a warning should be issued for an empty type set.
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Determines if a warning should be issued for an empty type set.
     *
     * @param failFast {@code true} if a warning should be issued for an empty type set.
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Returns {@code true} if extended parsing should be used.
     *
     * @return {@code true} if extended parsing should be used.
     */
    public boolean isExtendedParsing() {
        return extendedParsing;
    }

    /**
     * Determines if extended parsing should be used.
     *
     * @param extendedParsing {@code true} if extended parsing should be used.
     */
    public void setExtendedParsing(boolean extendedParsing) {
        this.extendedParsing = extendedParsing;
    }

    /**
     * Determines the discovery for finding plugins on the class path.
     *
     * @return The discovery for finding plugins on the class path.
     */
    public Discovery getDiscovery() {
        return discovery;
    }

    /**
     * Determines the discovery for finding plugins on the class path.
     *
     * @param discovery The discovery for finding plugins on the class path.
     */
    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    /**
     * Returns the number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     *
     * @return The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    public int getThreads() {
        return threads;
    }

    /**
     * Sets the number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     *
     * @param threads The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Returns the source set to resolve plugin names from or {@code null} if no such source set is used.
     *
     * @return The source set to resolve plugin names from or {@code null} if no such source set is used.
     */
    @MaybeNull
    public FileCollection getDiscoverySet() {
        return discoverySet;
    }

    /**
     * Defines the source set to resolve plugin names from or {@code null} if no such source set is used.
     *
     * @param discoverySet The source set to resolve plugin names from or {@code null} if no such source set is used.
     */
    public void setDiscoverySet(@MaybeNull FileCollection discoverySet) {
        this.discoverySet = discoverySet;
    }

    /**
     * Applies this extension's properties.
     *
     * @param task The task to configure.
     */
    protected void configure(ByteBuddyLocalClassesEnhancerTask task) {
        task.getTransformations().convention(getTransformations());
        task.getEntryPoint().convention(getEntryPoint());
        task.getSuffix().convention(getSuffix());
        task.getFailOnLiveInitializer().convention(isFailOnLiveInitializer());
        task.getWarnOnEmptyTypeSet().convention(isWarnOnEmptyTypeSet());
        task.getFailFast().convention(isFailFast());
        task.getExtendedParsing().convention(isExtendedParsing());
        task.getDiscovery().convention(getDiscovery());
        task.getThreads().convention(getThreads());
        task.getDiscoverySet().setFrom(getDiscoverySet());
    }
}
