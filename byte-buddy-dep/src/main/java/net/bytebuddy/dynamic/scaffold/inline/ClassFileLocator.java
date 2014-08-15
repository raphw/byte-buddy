package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.InputStream;

/**
 * Locates a class file by its type description in order to process it for redefinition.
 */
public interface ClassFileLocator {

    /**
     * Locates a class file from the class path.
     */
    static enum ForClassPathType implements ClassFileLocator {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * The extension for a class file.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        @Override
        public InputStream classFileFor(TypeDescription typeDescription) {
            InputStream classFileStream = ClassLoader.getSystemResourceAsStream(typeDescription.getInternalName() + CLASS_FILE_EXTENSION);
            if (classFileStream == null) {
                throw new IllegalArgumentException("Cannot locate type " + typeDescription + " on the class path");
            }
            return classFileStream;
        }
    }

    /**
     * Locates the class file for a given type and returns the file as an input stream. The input stream is
     * closed automatically after it is processed.
     *
     * @param typeDescription The description of the type for which a class file is to be located.
     * @return An input stream representing the given type.
     */
    InputStream classFileFor(TypeDescription typeDescription);
}
