package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

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
     * A Java agent that allows the location of class files by emulating a retransformation. Note that this class file
     * locator causes a class to be loaded in order to look up its class file. Also, this locator does deliberately not
     * support the look-up of classes that represent lambda expressions.
     */
    static class AgentBased implements ClassFileLocator {

        /**
         * The name of the Byte Buddy agent class.
         */
        private static final String BYTE_BUDDY_AGENT_TYPE = "net.bytebuddy.agent.ByteBuddyAgent";

        /**
         * The name of the {@code ByteBuddyAgent} class's method for obtaining an instrumentation.
         */
        private static final String GET_INSTRUMENTATION_METHOD = "getInstrumentation";

        /**
         * Base for access to a reflective member to make the code more readable.
         */
        private static final Object STATIC_METHOD = null;
        /**
         * The instrumentation instance to use for looking up the binary format of a type.
         */
        private final Instrumentation instrumentation;
        /**
         * The class loader which is expected to load a class of a given binary format.
         */
        private final ClassLoader classLoader;

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation The instrumentation to use for looking up a class file implementation.
         * @param classLoader     The class loader that is expected to load the looked-up a class.
         */
        public AgentBased(Instrumentation instrumentation, ClassLoader classLoader) {
            if (!instrumentation.isRetransformClassesSupported()) {
                throw new IllegalArgumentException(instrumentation + " does not support retransformation");
            }
            this.instrumentation = instrumentation;
            this.classLoader = nonNull(classLoader);
        }

        /**
         * Returns an agent-based class file locator for the given class loader and an already installed
         * Byte Buddy agent.
         *
         * @param classLoader The class loader that is expected to load the looked-up a class.
         * @return A class file locator for the given class loader based on a Byte Buddy agent.
         */
        public static ClassFileLocator fromInstalledAgent(ClassLoader classLoader) {
            try {
                return new AgentBased((Instrumentation) ClassLoader.getSystemClassLoader()
                        .loadClass(BYTE_BUDDY_AGENT_TYPE)
                        .getDeclaredMethod(GET_INSTRUMENTATION_METHOD)
                        .invoke(STATIC_METHOD), classLoader);
            } catch (Exception e) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", e);
            }
        }

        @Override
        public InputStream classFileFor(TypeDescription typeDescription) {
            try {
                ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(classLoader, typeDescription);
                try {
                    instrumentation.addTransformer(classFileTransformer, true);
                    instrumentation.retransformClasses(classLoader.loadClass(typeDescription.getName()));
                    byte[] binaryRepresentation = classFileTransformer.getClassFile();
                    return binaryRepresentation == null
                            ? null
                            : new ByteArrayInputStream(binaryRepresentation);
                } finally {
                    instrumentation.removeTransformer(classFileTransformer);
                }
            } catch (Exception ignored) {
                return null;
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && classLoader.equals(((AgentBased) other).classLoader)
                    && instrumentation.equals(((AgentBased) other).instrumentation);
        }

        @Override
        public int hashCode() {
            return 31 * instrumentation.hashCode() + classLoader.hashCode();
        }

        @Override
        public String toString() {
            return "ClassFileLocator.AgentBased{" +
                    "instrumentation=" + instrumentation +
                    ", classLoader=" + classLoader +
                    '}';
        }

        /**
         * A non-operational class file transformer that remembers the binary format of a given class.
         */
        protected static class ExtractionClassFileTransformer implements ClassFileTransformer {

            /**
             * The class loader that is expected to have loaded the looked-up a class.
             */
            private final ClassLoader classLoader;

            /**
             * A description of the type to look up.
             */
            private final TypeDescription typeDescription;

            /**
             * The binary representation of the looked-up class.
             */
            private volatile byte[] classFile;

            /**
             * Creates a class file transformer for the purpose of extraction.
             *
             * @param classLoader     The class loader that is expected to have loaded the looked-up a class.
             * @param typeDescription A description of the type to look up.
             */
            protected ExtractionClassFileTransformer(ClassLoader classLoader, TypeDescription typeDescription) {
                this.classLoader = classLoader;
                this.typeDescription = typeDescription;
            }

            @Override
            public byte[] transform(ClassLoader classLoader,
                                    String internalName,
                                    Class<?> redefinedType,
                                    ProtectionDomain protectionDomain,
                                    byte[] classFile) throws IllegalClassFormatException {
                if (isChild(classLoader) && typeDescription.represents(redefinedType)) {
                    this.classFile = classFile;
                }
                return classFile;
            }

            /**
             * Checks if the given class loader is a child of the specified class loader.
             *
             * @param classLoader The class loader that loaded the retransformed class.
             * @return {@code true} if te given class loader is a child of the specified class loader.
             */
            private boolean isChild(ClassLoader classLoader) {
                if (this.classLoader == null) {
                    return true; // The bootstrap class loader is any class loader's parent.
                }
                do {
                    if (classLoader == this.classLoader) {
                        return true;
                    }
                } while ((classLoader = classLoader.getParent()) != null);
                return false;
            }

            /**
             * Returns the binary representation of the class file that was looked up.
             *
             * @return The binary representation of the class file or {@code null} if no such class file could
             * be located.
             */
            protected byte[] getClassFile() {
                return classFile;
            }

            @Override
            public String toString() {
                return "ClassFileLocator.AgentBased.ExtractionClassFileTransformer{" +
                        "classLoader=" + classLoader +
                        ", typeDescription=" + typeDescription +
                        ", classFile=" + Arrays.toString(classFile) +
                        '}';
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
