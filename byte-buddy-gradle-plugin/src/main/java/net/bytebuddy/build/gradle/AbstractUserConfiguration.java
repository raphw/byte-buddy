/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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

import java.io.File;
import java.util.Iterator;

/**
 * An abstract base class for a user configuration implying a class path.
 */
public class AbstractUserConfiguration {

    /**
     * The class to use for this configuration path or {@code null} if no class path is specified.
     */
    private Iterable<File> classPath;

    /**
     * Returns the class path or builds a class path from the supplied arguments if no class path was set.
     *
     * @param root      The root directory of the project being built.
     * @param classPath The class path dependencies.
     * @return An iterable of all elements of the class path to be used.
     */
    public Iterable<? extends File> getClassPath(File root, Iterable<? extends File> classPath) {
        return this.classPath == null
                ? new PrefixIterable(root, classPath)
                : this.classPath;
    }

    /**
     * Sets the class path to use for this configuration.
     *
     * @param classPath The class path to use.
     */
    public void setClassPath(Iterable<File> classPath) {
        this.classPath = classPath;
    }

    /**
     * An iterable with a single {@link File} element prepended.
     */
    protected static class PrefixIterable implements Iterable<File> {

        /**
         * The prefixed file.
         */
        private final File file;

        /**
         * The iterable containing the remainder files.
         */
        private final Iterable<? extends File> files;

        /**
         * @param file  The prefixed file.
         * @param files The iterable containing the remainder files.
         */
        protected PrefixIterable(File file, Iterable<? extends File> files) {
            this.file = file;
            this.files = files;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<File> iterator() {
            return new PrefixIterator(file, files.iterator());
        }

        /**
         * An iterator with a single prefixed file.
         */
        protected static class PrefixIterator implements Iterator<File> {

            /**
             * The file being prefixed.
             */
            private final File file;

            /**
             * An iterator over the remainder files.
             */
            private final Iterator<? extends File> files;

            /**
             * {@code true} if the prefix was not yet returned from the iteration.
             */
            private boolean first;

            /**
             * Creates a prefix iterator.
             *
             * @param file  The file being prefixed.
             * @param files An iterator over the remainder files.
             */
            protected PrefixIterator(File file, Iterator<? extends File> files) {
                this.file = file;
                this.files = files;
                first = true;
            }

            /**
             * {@inheritDoc}
             */
            public boolean hasNext() {
                return first || files.hasNext();
            }

            /**
             * {@inheritDoc}
             */
            public File next() {
                if (first) {
                    first = false;
                    return file;
                } else {
                    return files.next();
                }
            }

            /**
             * {@inheritDoc}
             */
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove file from iterator");
            }
        }
    }
}
