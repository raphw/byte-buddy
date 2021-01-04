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

/**
 * A Byte Buddy task extension.
 */
public class ByteBuddyTaskExtension extends AbstractByteBuddyTaskExtension<ByteBuddyTask> {

    /**
     * The incremental builder to apply or {@code null} if no incremental build should be applied.
     */
    private IncrementalResolver incrementalResolver;

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
    public IncrementalResolver getIncrementalResolver() {
        return incrementalResolver;
    }

    /**
     * Sets the incremental builder to apply or {@code null} if no incremental build should be applied.
     *
     * @param incrementalResolver The incremental builder to apply or {@code null} if no incremental build should be applied.
     */
    public void setIncrementalResolver(IncrementalResolver incrementalResolver) {
        this.incrementalResolver = incrementalResolver;
    }

    @Override
    protected void doConfigure(ByteBuddyTask task) {
        task.setIncrementalResolver(getIncrementalResolver());
    }
}
