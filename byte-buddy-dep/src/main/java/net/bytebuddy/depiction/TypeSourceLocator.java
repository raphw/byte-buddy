package net.bytebuddy.depiction;

import net.bytebuddy.utility.StreamDrainer;

import java.io.IOException;
import java.io.InputStream;

public interface TypeSourceLocator {

    static class ForClassLoader implements TypeSourceLocator {

        private static final String CLASS_FILE_SUFFIX = ".class";

        private final ClassLoader classLoader;

        public ForClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public byte[] locate(String typeName) {
            InputStream resource = classLoader.getResourceAsStream(typeName.replace('.', '/') + CLASS_FILE_SUFFIX);
            if (resource == null) {
                return null;
            }
            try {
                try {
                    return new StreamDrainer().drain(resource);
                } finally {
                    resource.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading resource stream for " + typeName, e);
            }
        }
    }

    byte[] locate(String typeName);
}
