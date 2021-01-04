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
import net.bytebuddy.build.gradle.api.*;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * A Byte Buddy task implementation that supports incremental compilation.
 */
public abstract class ByteBuddyTask extends AbstractByteBuddyTask {

    /**
     * The incremental builder to apply or {@code null} if no incremental build should be applied.
     */
    private IncrementalResolver incrementalResolver;

    /**
     * Creates a new Byte Buddy task.
     */
    @Inject
    public ByteBuddyTask() {
        new ByteBuddyTaskExtension().configure(this);
    }

    /**
     * Returns the source directory.
     *
     * @return The source directory.
     */
    @Incremental
    @InputDirectory
    public abstract DirectoryProperty getSource();

    /**
     * Returns the target directory.
     *
     * @return The target directory.
     */
    @OutputDirectory
    public abstract DirectoryProperty getTarget();

    /**
     * Returns the class path to supply to the plugin engine.
     *
     * @return The class path to supply to the plugin engine.
     */
    @InputFiles
    @CompileClasspath
    public abstract ConfigurableFileCollection getClassPath();

    /**
     * Returns the incremental builder to apply or {@code null} if no incremental build should be applied.
     *
     * @return The incremental builder to apply or {@code null} if no incremental build should be applied.
     */
    @Internal
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
    protected File source() {
        return getSource().getAsFile().get();
    }

    @Override
    protected File target() {
        return getTarget().getAsFile().get();
    }

    @Override
    protected Iterable<File> classPath() {
        return getClassPath();
    }

    /**
     * Applies this task.
     *
     * @param inputChanges The input changes to consider.
     * @throws IOException If an I/O exception occurs.
     */
    @TaskAction
    public void apply(InputChanges inputChanges) throws IOException {
        Plugin.Engine.Source source;
        if (inputChanges.isIncremental() && getIncrementalResolver() != null) {
            getLogger().debug("Applying incremental build");
            source = new IncrementalSource(source(), getIncrementalResolver().apply(getProject(),
                    inputChanges.getFileChanges(getSource()),
                    source(),
                    target(),
                    classPath()));
        } else {
            getLogger().debug("Applying non-incremental build");
            if (getProject().delete(getTarget().getAsFileTree())) {
                getLogger().debug("Deleted all target files in {}", getTarget());
            }
            source = new Plugin.Engine.Source.ForFolder(getSource().getAsFile().get());
        }
        doApply(source, new Plugin.Engine.Target.ForFolder(getTarget().getAsFile().get()));
    }

    /**
     * A source for an incrementally changed source folder.
     */
    protected static class IncrementalSource extends Plugin.Engine.Source.ForFolder {

        /**
         * The root folder.
         */
        private final File root;

        /**
         * A list of files that requires retransformation.
         */
        private final List<File> files;

        /**
         * Creates a new incremental source.
         *
         * @param root  The root folder.
         * @param files A list of files that requires retransformation.
         */
        protected IncrementalSource(File root, List<File> files) {
            super(root);
            this.root = root;
            this.files = files;
        }

        @Override
        public Iterator<Element> iterator() {
            return new DelegationIterator(root, files.iterator());
        }

        /**
         * An iterator that delegates to an iterator of files.
         */
        private static class DelegationIterator implements Iterator<Element> {

            /**
             * The root folder.
             */
            private final File root;

            /**
             * The iterator to delegate to.
             */
            private final Iterator<File> delegate;

            /**
             * Creates a new delegation iterator.
             *
             * @param root     The root folder.
             * @param delegate The iterator to delegate to.
             */
            public DelegationIterator(File root, Iterator<File> delegate) {
                this.root = root;
                this.delegate = delegate;
            }

            /**
             * {@inheritDoc}
             */
            public boolean hasNext() {
                return delegate.hasNext();
            }

            /**
             * {@inheritDoc}
             */
            public Element next() {
                return new Element.ForFile(root, delegate.next());
            }

            /**
             * {@inheritDoc}
             */
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        }
    }
}
