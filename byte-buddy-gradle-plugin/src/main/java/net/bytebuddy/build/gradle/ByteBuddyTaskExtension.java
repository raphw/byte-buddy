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

/**
 * A Byte Buddy task extension.
 */
public class ByteBuddyTaskExtension extends AbstractByteBuddyTaskExtension<ByteBuddyTask> {

    /**
     * The incremental builder to apply or {@code null} if no incremental build should be applied.
     */
    @MaybeNull
    private IncrementalResolver incrementalResolver;

    /**
     * {@code true} if the class path is considered to be incremental.
     */
    private boolean incrementalClassPath;

    /**
     * Creates a new Byte Buddy task extension.
     */
    public ByteBuddyTaskExtension() {
        incrementalResolver = IncrementalResolver.ForChangedFiles.INSTANCE;
    }

    /**
     * Returns the incremental builder to apply or {@code null} if no incremental build should be applied.
     *
     * @return The incremental builder to apply or {@code null} if no incremental build should be applied.
     */
    @MaybeNull
    public IncrementalResolver getIncrementalResolver() {
        return incrementalResolver;
    }

    /**
     * Sets the incremental builder to apply or {@code null} if no incremental build should be applied.
     *
     * @param incrementalResolver The incremental builder to apply or {@code null} if no incremental build should be applied.
     */
    public void setIncrementalResolver(@MaybeNull IncrementalResolver incrementalResolver) {
        this.incrementalResolver = incrementalResolver;
    }

    /**
     * Returns {@code true} if the class path should be considered incremental. By default, the class path
     * is considered non-incremental where all classes are retransformed if the class path changes.
     *
     * @return {@code true} if the class path should be considered incremental.
     */
    public boolean isIncrementalClassPath() {
        return incrementalClassPath;
    }

    /**
     * Sets the class path to be incremental or non-incremental.
     *
     * @param incrementalClassPath {@code true} if the class path is to be considered incremental.
     */
    public void setIncrementalClassPath(boolean incrementalClassPath) {
        this.incrementalClassPath = incrementalClassPath;
    }

    @Override
    protected void doConfigure(ByteBuddyTask task) {
        task.setIncrementalResolver(getIncrementalResolver());
    }

    @Override
    protected Class<? extends ByteBuddyTask> toType() {
        return incrementalClassPath
                ? ByteBuddyTask.WithIncrementalClassPath.class
                : ByteBuddyTask.class;
    }
}
