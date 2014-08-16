package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Locates a class file by its type description in order to process it for redefinition.
 */
public interface ClassFileLocator {

    /**
     * The file extension for a Java class file.
     */
    static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * Locates a class file from the class path.
     */
    static enum ForClassPath implements ClassFileLocator {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public InputStream classFileFor(TypeDescription typeDescription) {
            return ClassLoader.getSystemResourceAsStream(typeDescription.getInternalName() + CLASS_FILE_EXTENSION);
        }
    }

    static enum ForAttachedClassLoader implements ClassFileLocator {

        INSTANCE;

        @Override
        public InputStream classFileFor(TypeDescription typeDescription) {
            ClassLoader classLoader = typeDescription.getClassLoader();
            return classLoader != null
                    ? classLoader.getResourceAsStream(typeDescription.getInternalName() + CLASS_FILE_EXTENSION)
                    : null;
        }
    }

    static class Compound implements ClassFileLocator {

        public static ClassFileLocator makeDefault() {
            return new Compound(ForAttachedClassLoader.INSTANCE, ForClassPath.INSTANCE);
        }

        private final ClassFileLocator[] classFileLocator;

        public Compound(ClassFileLocator... classFileLocator) {
            this.classFileLocator = classFileLocator;
        }

        @Override
        public InputStream classFileFor(TypeDescription typeDescription) {
            for (ClassFileLocator classFileLocator : this.classFileLocator) {
                InputStream inputStream = classFileLocator.classFileFor(typeDescription);
                if (inputStream != null) {
                    return inputStream;
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(classFileLocator, ((Compound) other).classFileLocator);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(classFileLocator);
        }

        @Override
        public String toString() {
            return "ClassFileLocator.Compound{classFileLocator=" + Arrays.toString(classFileLocator) + '}';
        }
    }

    /**
     * Locates the class file for a given type and returns the file as an input stream. The input stream is
     * closed automatically after it is processed. If no class file can be located, {@code null} is returned.
     *
     * @param typeDescription The description of the type for which a class file is to be located.
     * @return An input stream representing the given type.
     */
    InputStream classFileFor(TypeDescription typeDescription);
}
