package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Locates a class file or its byte array representation when it is given its type description.
 */
public interface ClassFileLocator {

    /**
     * The file extension for a Java class file.
     */
    static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * Locates the class file for a given type and returns the file as an input stream. Any requested
     * {@link java.io.InputStream} is closed automatically after it is processed. If no class file can be located,
     * {@code null} is returned.
     *
     * @param typeDescription The description of the type for which a class file is to be located.
     * @return An input stream representing the given type.
     */
    InputStream classFileFor(TypeDescription typeDescription);

    /**
     * Default implementations for a {@link net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator}.
     */
    static enum Default implements ClassFileLocator {

        /**
         * Locates a class file from the class path.
         */
        CLASS_PATH {
            @Override
            public InputStream classFileFor(TypeDescription typeDescription) {
                return ClassLoader.getSystemResourceAsStream(typeDescription.getInternalName() + CLASS_FILE_EXTENSION);
            }
        },

        /**
         * Locates a class file from a {@link java.lang.ClassLoader}'s resource lookup. This is only possible if a
         * type is described by a loaded {@link java.lang.Class}.
         */
        ATTACHED {
            @Override
            public InputStream classFileFor(TypeDescription typeDescription) {
                ClassLoader classLoader = typeDescription.getClassLoader();
                return classLoader != null
                        ? classLoader.getResourceAsStream(typeDescription.getInternalName() + CLASS_FILE_EXTENSION)
                        : null;
            }
        }
    }

    /**
     * A compound {@link net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator} that chains several locators.
     * Any class file locator is queried in the supplied order until one locator is able to provide an input
     * stream of the class file.
     */
    static class Compound implements ClassFileLocator {

        /**
         * The {@link net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator}s which are represented by this compound
         * class file locator  in the order of their application.
         */
        private final ClassFileLocator[] classFileLocator;

        /**
         * Creates a new compound class file locator.
         *
         * @param classFileLocator The {@link net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator}s to be
         *                         represented by this compound class file locator in the order of their application.
         */
        public Compound(ClassFileLocator... classFileLocator) {
            this.classFileLocator = classFileLocator;
        }

        /**
         * Creates a default class file locator by chaining the
         * {@link net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator.Default#ATTACHED} and the
         * {@link net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator.Default#CLASS_PATH} class file locator.
         *
         * @return A default class file locator.
         */
        public static ClassFileLocator makeDefault() {
            return new Compound(Default.ATTACHED, Default.CLASS_PATH);
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
}
