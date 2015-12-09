package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.utility.StreamDrainer;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

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
             * @param binaryRepresentation The binary data to represent. The array must not be modified.
             */
            @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The received value is never modified by contract")
            public Explicit(byte[] binaryRepresentation) {
                this.binaryRepresentation = binaryRepresentation;
            }

            @Override
            public boolean isResolved() {
                return true;
            }

            @Override
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Return value must never be modified by contract")
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
     * A class file locator that cannot locate any class files.
     */
    enum NoOp implements ClassFileLocator {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolution locate(String typeName) {
            return Resolution.Illegal.INSTANCE;
        }

        @Override
        public String toString() {
            return "ClassFileLocator.NoOp." + name();
        }
    }

    /**
     * A simple class file locator that returns class files from a selection of given types.
     */
    class Simple implements ClassFileLocator {

        /**
         * The class files that are known to this class file locator mapped by their type name.
         */
        private final Map<String, byte[]> classFiles;

        /**
         * Creates a new simple class file locator.
         *
         * @param classFiles The class files that are known to this class file locator mapped by their type name.
         */
        public Simple(Map<String, byte[]> classFiles) {
            this.classFiles = classFiles;
        }

        /**
         * Creates a class file locator for a single known type.
         *
         * @param typeName             The name of the type.
         * @param binaryRepresentation The binary representation of the type.
         * @return An appropriate class file locator.
         */
        public static ClassFileLocator of(String typeName, byte[] binaryRepresentation) {
            return new Simple(Collections.singletonMap(typeName, binaryRepresentation));
        }

        /**
         * Creates a class file locator for a single known type with an additional fallback locator.
         *
         * @param typeName             The name of the type.
         * @param binaryRepresentation The binary representation of the type.
         * @param fallback             The class file locator to query in case that a lookup triggers any other type.
         * @return An appropriate class file locator.
         */
        public static ClassFileLocator of(String typeName, byte[] binaryRepresentation, ClassFileLocator fallback) {
            return new Compound(new Simple(Collections.singletonMap(typeName, binaryRepresentation)), fallback);
        }

        @Override
        public Resolution locate(String typeName) {
            byte[] binaryRepresentation = classFiles.get(typeName);
            return binaryRepresentation == null
                    ? Resolution.Illegal.INSTANCE
                    : new Resolution.Explicit(binaryRepresentation);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && classFiles.equals(((Simple) other).classFiles);
        }

        @Override
        public int hashCode() {
            return classFiles.hashCode();
        }

        @Override
        public String toString() {
            return "ClassFileLocator.Simple{" +
                    "classFiles=" + classFiles +
                    '}';
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

        /**
         * Attemts to create a binary representation of a loaded type by requesting data from its
         * {@link java.lang.ClassLoader}.
         *
         * @param type The type of interest.
         * @return The binary data to this type which might be illegal.
         */
        public static Resolution read(Class<?> type) {
            try {
                return ClassFileLocator.ForClassLoader.of(type.getClassLoader()).locate(type.getName());
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read class file for " + type, exception);
            }
        }

        @Override
        public Resolution locate(String typeName) throws IOException {
            InputStream inputStream = classLoader.getResourceAsStream(typeName.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (inputStream != null) {
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
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
     * A class file locator that locates classes within a Java <i>jar</i> file.
     */
    class ForJarFile implements ClassFileLocator, Closeable {

        /**
         * The jar file to read from.
         */
        private final JarFile jarFile;

        /**
         * Creates a new class file locator for the given jar file.
         *
         * @param jarFile The jar file to read from.
         */
        public ForJarFile(JarFile jarFile) {
            this.jarFile = jarFile;
        }

        @Override
        public Resolution locate(String typeName) throws IOException {
            ZipEntry zipEntry = jarFile.getEntry(typeName.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (zipEntry == null) {
                return Resolution.Illegal.INSTANCE;
            } else {
                InputStream inputStream = jarFile.getInputStream(zipEntry);
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            }
        }

        @Override
        public void close() throws IOException {
            jarFile.close();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof ForJarFile
                    && jarFile.equals(((ForJarFile) other).jarFile);
        }

        @Override
        public int hashCode() {
            return jarFile.hashCode();
        }

        @Override
        public String toString() {
            return "ClassFileLocator.ForJarFile{" +
                    "jarFile=" + jarFile +
                    '}';
        }
    }

    /**
     * A class file locator that finds files from a standardized Java folder structure with
     * folders donating packages and class files being saved as {@code <classname>.class} files
     * within their package folder.
     */
    class ForFolder implements ClassFileLocator {

        /**
         * The base folder of the package structure.
         */
        private final File folder;

        /**
         * Creates a new class file locator for a folder structure of class files.
         *
         * @param folder The base folder of the package structure.
         */
        public ForFolder(File folder) {
            this.folder = folder;
        }

        @Override
        public Resolution locate(String typeName) throws IOException {
            File file = new File(folder, typeName.replace('.', File.separatorChar) + CLASS_FILE_EXTENSION);
            if (file.exists()) {
                InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            } else {
                return Resolution.Illegal.INSTANCE;
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof ForFolder
                    && folder.equals(((ForFolder) other).folder);
        }

        @Override
        public int hashCode() {
            return folder.hashCode();
        }

        @Override
        public String toString() {
            return "ClassFileLocator.ForFolder{" +
                    "folder=" + folder +
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
         * The name of the Byte Buddy {@code net.bytebuddy.agent.Installer} class.
         */
        private static final String INSTALLER_TYPE = "net.bytebuddy.agent.Installer";

        /**
         * The name of the {@code net.bytebuddy.agent.Installer} field containing an installed {@link Instrumentation}.
         */
        private static final String INSTRUMENTATION_FIELD = "instrumentation";

        /**
         * Indicator for accessing a field using reflection to make the code more readable.
         */
        private static final Object STATIC_FIELD = null;

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
                Instrumentation instrumentation = (Instrumentation) ClassLoader.getSystemClassLoader()
                        .loadClass(INSTALLER_TYPE)
                        .getDeclaredField(INSTRUMENTATION_FIELD)
                        .get(STATIC_FIELD);
                if (instrumentation == null) {
                    throw new IllegalStateException("The Byte Buddy agent is not installed");
                }
                return new AgentBased(instrumentation, classLoader);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
            }
        }

        /**
         * Returns a class file locator that is capable of locating a class file for the given type using the given instrumentation instance.
         *
         * @param instrumentation The instrumentation instance to query for a retransformation.
         * @param type            The locatable type which class loader is used as a fallback.
         * @return A class file locator for locating the class file of the given type.
         */
        public static ClassFileLocator of(Instrumentation instrumentation, Class<?> type) {
            return new AgentBased(instrumentation, ClassLoadingDelegate.Explicit.of(type));
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
            } catch (RuntimeException exception) {
                throw exception;
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
                    return of(classLoader, AccessController.getContext());
                }

                /**
                 * Creates a class loading delegate for the given class loader.
                 *
                 * @param classLoader          The class loader for which to create a delegate.
                 * @param accessControlContext The access control context to use when reading a class from a delegating class loader.
                 * @return The class loading delegate for the provided class loader.
                 */
                public static ClassLoadingDelegate of(ClassLoader classLoader, AccessControlContext accessControlContext) {
                    return ForDelegatingClassLoader.isDelegating(classLoader)
                            ? new ForDelegatingClassLoader(classLoader, accessControlContext)
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
                 * A dispatcher for extracting a class loader's loaded classes.
                 */
                private static final Dispatcher.Initializable DISPATCHER;

                /*
                 * Locates the {@link java.lang.ClassLoader}'s field that contains all loaded classes.
                 */
                static {
                    Dispatcher.Initializable dispatcher;
                    try {
                        dispatcher = new Dispatcher.Resolved(ClassLoader.class.getDeclaredField("classes"));
                    } catch (Exception exception) {
                        dispatcher = new Dispatcher.Unresolved(exception);
                    }
                    DISPATCHER = dispatcher;
                }

                /**
                 * The access control context to use for accessing the field.
                 */
                private final AccessControlContext accessControlContext;

                /**
                 * Creates a class loading delegate for a delegating class loader.
                 *
                 * @param classLoader          The delegating class loader.
                 * @param accessControlContext The access control context to be used for accessing
                 */
                protected ForDelegatingClassLoader(ClassLoader classLoader, AccessControlContext accessControlContext) {
                    super(classLoader);
                    this.accessControlContext = accessControlContext;
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
                        classes = DISPATCHER.initialize(accessControlContext).extract(classLoader);
                    } catch (RuntimeException ignored) {
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
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    if (!super.equals(other)) return false;
                    ForDelegatingClassLoader that = (ForDelegatingClassLoader) other;
                    return accessControlContext.equals(that.accessControlContext);
                }

                @Override
                public int hashCode() {
                    int result = super.hashCode();
                    result = 31 * result + accessControlContext.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader{" +
                            "classLoader=" + classLoader +
                            ", accessControlContext=" + accessControlContext +
                            '}';
                }

                /**
                 * Representation of a Java {@link java.lang.reflect.Field}.
                 */
                protected interface Dispatcher {

                    /**
                     * Reads the classes of the represented collection.
                     *
                     * @param classLoader The class loader to read from.
                     * @return The class loader's loaded classes.
                     */
                    Vector<Class<?>> extract(ClassLoader classLoader);

                    /**
                     * An unitialized version of a dispatcher for extracting a class loader's loaded classes.
                     */
                    interface Initializable {

                        /**
                         * Initializes the dispatcher.
                         *
                         * @param accessControlContext The access control context to use for accessing the private API.
                         * @return An initialized dispatcher.
                         */
                        Dispatcher initialize(AccessControlContext accessControlContext);
                    }

                    /**
                     * Represents a field that could be located.
                     */
                    class Resolved implements Dispatcher, Initializable, PrivilegedAction<Dispatcher> {

                        /**
                         * The represented field.
                         */
                        private final Field field;

                        /**
                         * Creates a new resolved field.
                         *
                         * @param field the represented field.l
                         */
                        public Resolved(Field field) {
                            this.field = field;
                        }

                        @Override
                        public Dispatcher initialize(AccessControlContext accessControlContext) {
                            return AccessController.doPrivileged(this, accessControlContext);
                        }

                        @Override
                        public Dispatcher run() {
                            field.setAccessible(true);
                            return this;
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        public Vector<Class<?>> extract(ClassLoader classLoader) {
                            try {
                                return (Vector<Class<?>>) field.get(classLoader);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access field", exception);
                            }
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && field.equals(((Resolved) other).field);
                        }

                        @Override
                        public int hashCode() {
                            return field.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader.Dispatcher.Resolved{" +
                                    "field=" + field +
                                    '}';
                        }
                    }

                    /**
                     * Represents a field that could not be located.
                     */
                    class Unresolved implements Initializable {

                        /**
                         * The exception that occurred when attempting to locate the field.
                         */
                        private final Exception exception;

                        /**
                         * Creates a representation of a non-resolved field.
                         *
                         * @param exception The exception that occurred when attempting to locate the field.
                         */
                        public Unresolved(Exception exception) {
                            this.exception = exception;
                        }

                        @Override
                        public Dispatcher initialize(AccessControlContext accessControlContext) {
                            throw new IllegalStateException("Could not locate classes vector", exception);
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && exception.equals(((Unresolved) other).exception);
                        }

                        @Override
                        public int hashCode() {
                            return exception.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader.Dispatcher.Unresolved{" +
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
                public Explicit(ClassLoader classLoader, Collection<? extends Class<?>> types) {
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
                public Explicit(ClassLoadingDelegate fallbackDelegate, Collection<? extends Class<?>> types) {
                    this.fallbackDelegate = fallbackDelegate;
                    this.types = new HashMap<String, Class<?>>();
                    for (Class<?> type : types) {
                        this.types.put(type.getName(), type);
                    }
                }

                /**
                 * Creates an explicit class loading delegate for the given type.
                 *
                 * @param type The type that is explicitly locatable.
                 * @return A suitable class loading delegate.
                 */
                public static ClassLoadingDelegate of(Class<?> type) {
                    return new Explicit(type.getClassLoader(), Collections.singleton(type));
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
            @SuppressFBWarnings(value = "VO_VOLATILE_REFERENCE_TO_ARRAY", justification = "By contract, the referenced array is not to be modified")
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
            @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Return value is always null; received value is never modified")
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
             * Returns the binary representation of the class file that was looked up. The returned array must never be modified.
             *
             * @return The binary representation of the class file or {@code null} if no such class file could
             * be located.
             */
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Return value must never be modified by contract")
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
