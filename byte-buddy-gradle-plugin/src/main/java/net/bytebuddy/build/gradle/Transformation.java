package net.bytebuddy.build.gradle;

import java.io.File;
import java.util.Iterator;

public class Transformation {

    private String plugin;

    private Iterable<File> classPath;

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public Iterable<? extends File> getClassPath(File root, Iterable<? extends File> classPath) {
        return this.classPath == null
                ? new PrefixIterable(root, classPath)
                : this.classPath;
    }

    public void setClassPath(Iterable<File> classPath) {
        this.classPath = classPath;
    }

    protected static class PrefixIterable implements Iterable<File> {

        private final File file;

        private final Iterable<? extends File> files;

        public PrefixIterable(File file, Iterable<? extends File> files) {
            this.file = file;
            this.files = files;
        }

        @Override
        public Iterator<File> iterator() {
            return new PrefixIterator(file, files.iterator());
        }

        protected static class PrefixIterator implements Iterator<File> {

            private final File file;

            private final Iterator<? extends File> files;

            private boolean first;

            public PrefixIterator(File file, Iterator<? extends File> files) {
                this.file = file;
                this.files = files;
                first = true;
            }

            @Override
            public boolean hasNext() {
                return first || files.hasNext();
            }

            @Override
            public File next() {
                if (first) {
                    first = false;
                    return file;
                } else {
                    return files.next();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove from iterator");
            }
        }
    }
}
