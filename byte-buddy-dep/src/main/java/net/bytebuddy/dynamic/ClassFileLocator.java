package net.bytebuddy.dynamic;

import net.bytebuddy.utility.StreamDrainer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.*;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * Locates a class file or its byte array representation when it is given its type description.
 */
public interface ClassFileLocator {

    /**
     * The file extension for a Java class file.
     */
    String CLASS_FILE_EXTENSION = ".class";

    /**
     * Locates the class file for a given type and returns the binary data of the class file.
     *
     * @param typeName The name of the type to locate a class file representation for.
     * @return Any binary representation of the type which might be illegal.
     * @throws java.io.IOException If reading a class file causes an error.
     */
    Resolution locate(String typeName) throws IOException;

    /**
     * Represents a class file as binary data.
     */
    interface Resolution {

        /**
         * Checks if this binary representation is valid.
         *
         * @return {@code true} if this binary representation is valid.
         */
        boolean isResolved();

        /**
         * Finds the data of this binary representation. Calling this method is only legal for resolved instances.
         * For non-resolved instances, an exception is thrown.
         *
         * @return The requested binary data. The returned array must not be altered.
         */
        byte[] resolve();

        /**
         * A canonical representation of an illegal binary representation.
         */
        enum Illegal implements Resolution {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isResolved() {
                return false;
            }

            @Override
            public byte[] resolve() {
                throw new IllegalStateException("Could not read binary data");
            }

            @Override
            public String toString() {
                return "ClassFileLocator.Resolution.Illegal." + name();
            }
        }

        /**
         * Represents a byte array as binary data.
         */
        class Explicit implements Resolution {

            /**
             * The represented data.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new explicit resolution of a given array of binary data.
             *
             * @param binaryRepresentation The binary data to represent.
             */
            public Explicit(byte[] binaryRepresentation) {
                this.binaryRepresentation = binaryRepresentation;
            }

            /**
             * Attemts to create a binary representation of a loaded type by requesting data from its
             * {@link java.lang.ClassLoader}.
             *
             * @param type The type of interest.
             * @return The binary data to this type which might be illegal.
             */
            public static Resolution of(Class<?> type) {
                InputStream inputStream = (type.getClassLoader() == null
                        ? ClassLoader.getSystemClassLoader()
                        : type.getClassLoader()).getResourceAsStream(type.getName().replace('.', '/') + CLASS_FILE_EXTENSION);
                if (inputStream == null) {
                    return Illegal.INSTANCE;
                } else {
                    try {
                        return new Explicit(new StreamDrainer().drain(inputStream));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }

            @Override
            public boolean isResolved() {
                return true;
            }

            @Override
            public byte[] resolve() {
                return binaryRepresentation;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Arrays.equals(binaryRepresentation, ((Explicit) other).binaryRepresentation);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(binaryRepresentation);
            }

            @Override
            public String toString() {
                return "ClassFileLocator.Resolution.Explicit{" +
                        "binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                        '}';
            }
        }
    }

    /**
     * A class file locator that queries a class loader for binary representations of class files.
     */
    class ForClassLoader implements ClassFileLocator {

        /**
         * The class loader to query.
         */
        private final ClassLoader classLoader;

        /**
         * Creates a new class file locator for the given class loader.
         *
         * @param classLoader The class loader to query which must not be the bootstrap class loader, i.e. {@code null}.
         */
        protected ForClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        /**
         * Creates a class file locator that queries the system class loader.
         *
         * @return A class file locator that queries the system class loader.
         */
        public static ClassFileLocator ofClassPath() {
            return new ForClassLoader(ClassLoader.getSystemClassLoader());
        }

        /**
         * Creates a class file locator for a given class loader.
         *
         * @param classLoader The class loader to be used. If this class loader represents the bootstrap class
         *                    loader which is represented by the {@code null} value, this system class loader
         *                    is used instead.
         * @return A corresponding source locator.
         */
        public static ClassFileLocator of(ClassLoader classLoader) {
            return new ForClassLoader(classLoader == null
                    ? ClassLoader.getSystemClassLoader()
                    : classLoader);
        }

        @Override
        public Resolution locate(String typeName) throws IOException {
            InputStream inputStream = classLoader.getResourceAsStream(typeName.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (inputStream != null) {
                try {
                    return new Resolution.Explicit(new StreamDrainer().drain(inputStream));
                } finally {
                    inputStream.close();
                }
            } else {
                return Resolution.Illegal.INSTANCE;
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && classLoader.equals(((ForClassLoader) other).classLoader);
        }

        @Override
        public int hashCode() {
            return classLoader.hashCode();
        }

        @Override
        public String toString() {
            return "ClassFileLocator.ForClassLoader{" +
                    "classLoader=" + classLoader +
                    '}';
        }
    }

    /**
     * A Java agent that allows the location of class files by emulating a retransformation. Note that this class file
     * locator causes a class to be loaded in order to look up its class file. Also, this locator does deliberately not
     * support the look-up of classes that represent lambda expressions.
     */
    class AgentBased implements ClassFileLocator {

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
         * The delegate to load a class by its name.
         */
        private final ClassLoadingDelegate classLoadingDelegate;

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation The instrumentation to be used.
         * @param classLoader     The class loader to read a class from.
         */
        public AgentBased(Instrumentation instrumentation, ClassLoader classLoader) {
            this(instrumentation, ClassLoadingDelegate.Default.of(classLoader));
        }

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation      The instrumentation to be used.
         * @param classLoadingDelegate The delegate responsible for class loading.
         */
        public AgentBased(Instrumentation instrumentation, ClassLoadingDelegate classLoadingDelegate) {
            if (!instrumentation.isRetransformClassesSupported()) {
                throw new IllegalArgumentException(instrumentation + " does not support retransformation");
            }
            this.instrumentation = instrumentation;
            this.classLoadingDelegate = nonNull(classLoadingDelegate);
        }

        /**
         * Returns an agent-based class file locator for the given class loader and an already installed
         * Byte Buddy-agent.
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
        public Resolution locate(String typeName) {
            try {
                ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(classLoadingDelegate.getClassLoader(), typeName);
                try {
                    instrumentation.addTransformer(classFileTransformer, true);
                    instrumentation.retransformClasses(classLoadingDelegate.locate(typeName));
                    byte[] binaryRepresentation = classFileTransformer.getBinaryRepresentation();
                    return binaryRepresentation == null
                            ? Resolution.Illegal.INSTANCE
                            : new Resolution.Explicit(binaryRepresentation);
                } finally {
                    instrumentation.removeTransformer(classFileTransformer);
                }
            } catch (Exception ignored) {
                return Resolution.Illegal.INSTANCE;
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && classLoadingDelegate.equals(((AgentBased) other).classLoadingDelegate)
                    && instrumentation.equals(((AgentBased) other).instrumentation);
        }

        @Override
        public int hashCode() {
            return 31 * instrumentation.hashCode() + classLoadingDelegate.hashCode();
        }

        @Override
        public String toString() {
            return "ClassFileLocator.AgentBased{" +
                    "instrumentation=" + instrumentation +
                    ", classLoadingDelegate=" + classLoadingDelegate +
                    '}';
        }

        /**
         * A delegate that is queried for loading a class.
         */
        public interface ClassLoadingDelegate {

            /**
             * Loads a class by its name.
             *
             * @param name The name of the type.
             * @return The class with the given name.
             * @throws ClassNotFoundException If a class cannot be found.
             */
            Class<?> locate(String name) throws ClassNotFoundException;

            /**
             * Returns the underlying class loader.
             *
             * @return The underlying class loader.
             */
            ClassLoader getClassLoader();

            /**
             * A default implementation of a class loading delegate.
             */
            class Default implements ClassLoadingDelegate {

                /**
                 * The underlying class loader.
                 */
                protected final ClassLoader classLoader;

                /**
                 * Creates a default class loading delegate.
                 *
                 * @param classLoader The class loader to be queried.
                 */
                protected Default(ClassLoader classLoader) {
                    this.classLoader = classLoader;
                }

                /**
                 * Creates a class loading delegate for the given class loader.
                 *
                 * @param classLoader The class loader for which to create a delegate.
                 * @return The class loading delegate for the provided class loader.
                 */
                public static ClassLoadingDelegate of(ClassLoader classLoader) {
                    return ForDelegatingClassLoader.isDelegating(classLoader)
                            ? new ForDelegatingClassLoader(classLoader)
                            : new Default(classLoader);
                }

                @Override
                public Class<?> locate(String name) throws ClassNotFoundException {
                    return classLoader.loadClass(name);
                }

                @Override
                public ClassLoader getClassLoader() {
                    return classLoader;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Default aDefault = (Default) other;
                    return !(classLoader != null ? !classLoader.equals(aDefault.classLoader) : aDefault.classLoader != null);
                }

                @Override
                public int hashCode() {
                    return classLoader != null ? classLoader.hashCode() : 0;
                }

                @Override
                public String toString() {
                    return "ClassFileLocator.AgentBased.ClassLoadingDelegate.Default{" +
                            "classLoader=" + classLoader +
                            '}';
                }
            }

            /**
             * A class loading delegate that accounts for a {@code sun.reflect.DelegatingClassLoader} which
             * cannot load its own classes by name.
             */
            class ForDelegatingClassLoader extends Default {

                /**
                 * The name of the delegating class loader.
                 */
                private static final String DELEGATING_CLASS_LOADER_NAME = "sun.reflect.DelegatingClassLoader";

                /**
                 * An index indicating the first element of a collection.
                 */
                private static final int ONLY = 0;

                /**
                 * The class loader's field that contains all loaded classes.
                 */
                private static final JavaField CLASSES_FIELD;

                /**
                 * Locates the {@link java.lang.ClassLoader}'s field that contains all loaded classes.
                 */
                static {
                    JavaField classesField;
                    try {
                        Field field = ClassLoader.class.getDeclaredField("classes");
                        field.setAccessible(true);
                        classesField = new JavaField.ForResolvedField(field);
                    } catch (Exception e) {
                        classesField = new JavaField.ForNonResolvedField(e);
                    }
                    CLASSES_FIELD = classesField;
                }

                /**
                 * Creates a class loading delegate for a delegating class loader.
                 *
                 * @param classLoader The delegating class loader.
                 */
                protected ForDelegatingClassLoader(ClassLoader classLoader) {
                    super(classLoader);
                }

                /**
                 * Checks if a class loader is a delegating class loader.
                 *
                 * @param classLoader The class loader to inspect.
                 * @return {@code true} if the class loader is a delegating class loader.
                 */
                protected static boolean isDelegating(ClassLoader classLoader) {
                    return classLoader != null && classLoader.getClass().getName().equals(DELEGATING_CLASS_LOADER_NAME);
                }

                @Override
                @SuppressWarnings("unchecked")
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Vector<Class<?>> classes;
                    try {
                        classes = (Vector<Class<?>>) CLASSES_FIELD.readValue(classLoader);
                    } catch (Exception ignored) {
                        return super.locate(name);
                    }
                    if (classes.size() != 1) {
                        return super.locate(name);
                    }
                    Class<?> type = classes.get(ONLY);
                    return type.getName().equals(name)
                            ? type
                            : super.locate(name);
                }

                @Override
                public String toString() {
                    return "ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader{" +
                            "classLoader=" + classLoader +
                            '}';
                }

                /**
                 * Representation of a Java {@link java.lang.reflect.Field}.
                 */
                protected interface JavaField {

                    /**
                     * Reads a value from the underlying field.
                     *
                     * @param instance The instance to read from.
                     * @return The field's value.
                     * @throws Exception If the field's value cannot be read.
                     */
                    Object readValue(Object instance) throws Exception;

                    /**
                     * Represents a field that could be located.
                     */
                    class ForResolvedField implements JavaField {

                        /**
                         * The represented field.
                         */
                        private final Field field;

                        /**
                         * Creates a new resolved field.
                         *
                         * @param field the represented field.l
                         */
                        public ForResolvedField(Field field) {
                            this.field = field;
                        }

                        @Override
                        public Object readValue(Object instance) throws Exception {
                            return field.get(instance);
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && field.equals(((ForResolvedField) other).field);
                        }

                        @Override
                        public int hashCode() {
                            return field.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader.JavaField.ForResolvedField{" +
                                    "field=" + field +
                                    '}';
                        }
                    }

                    /**
                     * Represents a field that could not be located.
                     */
                    class ForNonResolvedField implements JavaField {

                        /**
                         * The exception that occurred when attempting to locate the field.
                         */
                        private final Exception exception;

                        /**
                         * Creates a representation of a non-resolved field.
                         *
                         * @param exception The exception that occurred when attempting to locate the field.
                         */
                        public ForNonResolvedField(Exception exception) {
                            this.exception = exception;
                        }

                        @Override
                        public Object readValue(Object instance) throws Exception {
                            throw exception;
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && exception.equals(((ForNonResolvedField) other).exception);
                        }

                        @Override
                        public int hashCode() {
                            return exception.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader.JavaField.ForNonResolvedField{" +
                                    "exception=" + exception +
                                    '}';
                        }
                    }
                }
            }

            /**
             * A class loading delegate that allows the location of explicitly registered classes that cannot
             * be located by a class loader directly. This allows for locating classes that are loaded by
             * an anonymous class loader which does not register its classes in a system dictionary.
             */
            class Explicit implements ClassLoadingDelegate {

                /**
                 * A class loading delegate that is queried for classes that are not registered explicitly.
                 */
                private final ClassLoadingDelegate fallbackDelegate;

                /**
                 * The map of registered classes mapped by their name.
                 */
                private final Map<String, Class<?>> types;

                /**
                 * Creates a new class loading delegate with a possibility of looking up explicitly
                 * registered classes.
                 *
                 * @param classLoader The class loader to be used for looking up classes.
                 * @param types       A collection of classes that cannot be looked up explicitly.
                 */
                public Explicit(ClassLoader classLoader, Collection<Class<?>> types) {
                    this(new Default(classLoader), types);
                }

                /**
                 * Creates a new class loading delegate with a possibility of looking up explicitly
                 * registered classes.
                 *
                 * @param fallbackDelegate The class loading delegate to query for any class that is not
                 *                         registered explicitly.
                 * @param types            A collection of classes that cannot be looked up explicitly.
                 */
                public Explicit(ClassLoadingDelegate fallbackDelegate, Collection<Class<?>> types) {
                    this.fallbackDelegate = fallbackDelegate;
                    this.types = new HashMap<String, Class<?>>(types.size());
                    for (Class<?> type : types) {
                        this.types.put(type.getName(), type);
                    }
                }

                @Override
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Class<?> type = types.get(name);
                    return type == null
                            ? fallbackDelegate.locate(name)
                            : type;
                }

                @Override
                public ClassLoader getClassLoader() {
                    return fallbackDelegate.getClassLoader();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fallbackDelegate.equals(((Explicit) other).fallbackDelegate)
                            && types.equals(((Explicit) other).types);
                }

                @Override
                public int hashCode() {
                    int result = fallbackDelegate.hashCode();
                    result = 31 * result + types.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "ClassFileLocator.AgentBased.ClassLoadingDelegate.Explicit{" +
                            "fallbackDelegate=" + fallbackDelegate +
                            ", types=" + types +
                            '}';
                }
            }
        }

        /**
         * A non-operational class file transformer that remembers the binary format of a given class.
         */
        protected static class ExtractionClassFileTransformer implements ClassFileTransformer {

            /**
             * An indicator that an attempted class file transformation did not alter the handed class file.
             */
            private static final byte[] DO_NOT_TRANSFORM = null;

            /**
             * The class loader that is expected to have loaded the looked-up a class.
             */
            private final ClassLoader classLoader;

            /**
             * The name of the type to look up.
             */
            private final String typeName;

            /**
             * The binary representation of the looked-up class.
             */
            private volatile byte[] binaryRepresentation;

            /**
             * Creates a class file transformer for the purpose of extraction.
             *
             * @param classLoader The class loader that is expected to have loaded the looked-up a class.
             * @param typeName    The name of the type to look up.
             */
            protected ExtractionClassFileTransformer(ClassLoader classLoader, String typeName) {
                this.classLoader = classLoader;
                this.typeName = typeName;
            }

            @Override
            public byte[] transform(ClassLoader classLoader,
                                    String internalName,
                                    Class<?> redefinedType,
                                    ProtectionDomain protectionDomain,
                                    byte[] classFile) throws IllegalClassFormatException {
                if (isChild(classLoader) && typeName.equals(redefinedType.getName())) {
                    this.binaryRepresentation = classFile;
                }
                return DO_NOT_TRANSFORM;
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
            protected byte[] getBinaryRepresentation() {
                return binaryRepresentation;
            }

            @Override
            public String toString() {
                return "ClassFileLocator.AgentBased.ExtractionClassFileTransformer{" +
                        "classLoader=" + classLoader +
                        ", typeName=" + typeName +
                        ", binaryRepresentation=" +
                        (binaryRepresentation != null
                                ? "<" + binaryRepresentation.length + " bytes>"
                                : "null") +
                        '}';
            }
        }
    }

    /**
     * A compound {@link ClassFileLocator} that chains several locators.
     * Any class file locator is queried in the supplied order until one locator is able to provide an input
     * stream of the class file.
     */
    class Compound implements ClassFileLocator {

        /**
         * The {@link ClassFileLocator}s which are represented by this compound
         * class file locator  in the order of their application.
         */
        private final ClassFileLocator[] classFileLocator;

        /**
         * Creates a new compound class file locator.
         *
         * @param classFileLocator The {@link ClassFileLocator}s to be
         *                         represented by this compound class file locator in the order of their application.
         */
        public Compound(ClassFileLocator... classFileLocator) {
            this.classFileLocator = classFileLocator;
        }

        @Override
        public Resolution locate(String typeName) throws IOException {
            for (ClassFileLocator classFileLocator : this.classFileLocator) {
                Resolution resolution = classFileLocator.locate(typeName);
                if (resolution.isResolved()) {
                    return resolution;
                }
            }
            return Resolution.Illegal.INSTANCE;
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
