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

import groovy.lang.Closure;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.UnknownNull;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract Byte Buddy task extension.
 *
 * @param <T> The type of the task this extension represents.
 */
public abstract class AbstractByteBuddyTaskExtension<T extends AbstractByteBuddyTask> {

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
     * Determines what tasks are considered when adjusting the task dependency graph to include the Byte Buddy task.
     * By default, only the altered task's project's task is considered but the adjustment can include subprojects or
     * the entire project graph. Note that it might not always be legal to resolve such recursive dependencies.
     */
    private Adjustment adjustment;

    /**
     * Determines the reaction upon a failed task dependency resolution.
     */
    private Adjustment.ErrorHandler adjustmentErrorHandler;

    /**
     * The adjustment post processor that is applied after the graph dependencies are is resolved.
     */
    private Action<Task> adjustmentPostProcessor;

    /**
     * The number of threads to use for transforming or {@code 0} if the transformation should be applied in the main thread.
     */
    private int threads;

    /**
     * If {@code true}, task dependencies are only adjusted when the task graph is fully resolved.
     */
    private boolean lazy;

    /**
     * The class file version to use for creating auxiliary types or {@code null} if the
     * version is determined implicitly.
     */
    @MaybeNull
    private ClassFileVersion classFileVersion;

    /**
     * The class file version to use for resolving multi-release jar files or {@code null} if
     * {@link #classFileVersion} or the implicit version should be used.
     */
    @MaybeNull
    private ClassFileVersion multiReleaseClassFileVersion;

    /**
     * Creates a new abstract Byte Buddy task extension.
     *
     * @param project The current Gradle project.
     */
    protected AbstractByteBuddyTaskExtension(@UnknownNull Project project) {
        this.project = project;
        transformations = new ArrayList<Transformation>();
        entryPoint = EntryPoint.Default.REBASE;
        suffix = "";
        failOnLiveInitializer = true;
        warnOnEmptyTypeSet = true;
        failFast = true;
        discovery = Discovery.EMPTY;
        adjustment = Adjustment.FULL;
        adjustmentErrorHandler = Adjustment.ErrorHandler.WARN;
        adjustmentPostProcessor = Adjustment.NoOpPostProcessor.INSTANCE;
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
        Transformation transformation = ObjectFactory.newInstance(project, Transformation.class, project);
        if (transformation == null) {
            transformation = new Transformation(project);
        }
        transformations.add((Transformation) project.configure(transformation, closure));
    }

    /**
     * Adds an additional transformation.
     *
     * @param action The action to configure the transformation.
     */
    public void transformation(Action<Transformation> action) {
        Transformation transformation = ObjectFactory.newInstance(project, Transformation.class, project);
        if (transformation == null) {
            transformation = new Transformation(project);
        }
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
     * Determines the adjustment for tasks that might depend on post-processed compile tasks.
     *
     * @return The adjustment for tasks that might depend on post-processed compile tasks.
     */
    public Adjustment getAdjustment() {
        return adjustment;
    }

    /**
     * Determines the adjustment for tasks that might depend on post-processed compile tasks.
     *
     * @param adjustment The adjustment for tasks that might depend on post-processed compile tasks.
     */
    public void setAdjustment(Adjustment adjustment) {
        this.adjustment = adjustment;
    }

    /**
     * Returns the error handler to be used when a task dependency cannot be resolved.
     *
     * @return The error handler to be used when a task dependency cannot be resolved.
     */
    public Adjustment.ErrorHandler getAdjustmentErrorHandler() {
        return adjustmentErrorHandler;
    }

    /**
     * Sets the error handler to be used when a task dependency cannot be resolved.
     *
     * @param adjustmentErrorHandler The error handler to be used when a task dependency cannot be resolved.
     */
    public void setAdjustmentErrorHandler(Adjustment.ErrorHandler adjustmentErrorHandler) {
        this.adjustmentErrorHandler = adjustmentErrorHandler;
    }

    /**
     * Returns the adjustment post processor that is applied after the graph dependencies are is resolved.
     *
     * @return The adjustment post processor to apply.
     */
    public Action<Task> getAdjustmentPostProcessor() {
        return adjustmentPostProcessor;
    }

    /**
     * Sets the adjustment post processor that is applied after the graph dependencies are resolved.
     *
     * @param adjustmentPostProcessor The adjustment post processor to apply.
     */
    public void setAdjustmentPostProcessor(Action<Task> adjustmentPostProcessor) {
        this.adjustmentPostProcessor = adjustmentPostProcessor;
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
     * Returns {@code true}, task dependencies are only adjusted when the task graph is fully resolved.
     *
     * @return {@code true}, task dependencies are only adjusted when the task graph is fully resolved.
     */
    public boolean isLazy() {
        return lazy;
    }

    /**
     * If set to {@code true}, task dependencies are only adjusted when the task graph is fully resolved.
     *
     * @param lazy {@code true}, task dependencies are only adjusted when the task graph is fully resolved.
     */
    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * Returns the class file version to use for creating auxiliary types or {@code null} if the
     * version is determined implicitly.
     *
     * @return The class file version to use for creating auxiliary types.
     */
    @MaybeNull
    public ClassFileVersion getClassFileVersion() {
        return classFileVersion;
    }

    /**
     * Sets the class file version to use for creating auxiliary types or {@code null} if the
     * version is determined implicitly.
     *
     * @param classFileVersion The class file version to use for creating auxiliary types.
     */
    public void setClassFileVersion(@MaybeNull ClassFileVersion classFileVersion) {
        this.classFileVersion = classFileVersion;
    }

    /**
     * Returns the class file version to use for resolving multi-release jar files or {@code null} if the
     * explicit or implicit class file version of this task should be used.
     *
     * @return The class file version to use for resolving multi-release jar files.
     */
    @MaybeNull
    public ClassFileVersion getMultiReleaseClassFileVersion() {
        return multiReleaseClassFileVersion;
    }

    /**
     * Sets the class file version to use for resolving multi-release jar files.
     *
     * @param multiReleaseClassFileVersion The class file version to use for resolving multi-release jar files.
     */
    public void setMultiReleaseClassFileVersion(@MaybeNull ClassFileVersion multiReleaseClassFileVersion) {
        this.multiReleaseClassFileVersion = multiReleaseClassFileVersion;
    }

    /**
     * Resolves default properties and considers the contextual Java version.
     *
     * @param version The Java version to resolve as a fallback if no explicit version is set.
     */
    protected void resolve(JavaVersion version) {
        if (classFileVersion == null) {
            classFileVersion = ClassFileVersion.ofJavaVersion(Integer.parseInt(version.getMajorVersion()));
        }
    }

    /**
     * Returns {@code true} if this extension defines an empty discovery.
     *
     * @return {@code true} if this extension defines an empty discovery.
     */
    protected abstract boolean isEmptyDiscovery();

    /**
     * Applies any extension-specific properties.
     *
     * @param task The task to configure.
     */
    protected abstract void doConfigure(T task);

    /**
     * Applies this extension's properties.
     *
     * @param task The task to configure.
     */
    protected void configure(T task) {
        task.getTransformations().addAll(getTransformations());
        task.setEntryPoint(getEntryPoint());
        task.setSuffix(getSuffix());
        task.setFailOnLiveInitializer(isFailOnLiveInitializer());
        task.setWarnOnEmptyTypeSet(isWarnOnEmptyTypeSet());
        task.setFailFast(isFailFast());
        task.setExtendedParsing(isExtendedParsing());
        task.setDiscovery(getDiscovery());
        task.setThreads(getThreads());
        task.setClassFileVersion(getClassFileVersion());
        task.setMultiReleaseClassFileVersion(getMultiReleaseClassFileVersion());
        doConfigure(task);
    }

    /**
     * Defines the discovery set to use.
     *
     * @param fileCollection The file collection that represents the discovery set.
     */
    protected abstract void discoverySet(FileCollection fileCollection);

    /**
     * Returns the type of the Byte Buddy task.
     *
     * @return The type of the Byte Buddy task.
     */
    protected abstract Class<? extends T> toType();
}
