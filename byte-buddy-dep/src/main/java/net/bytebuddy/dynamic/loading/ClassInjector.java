package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.RandomString;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * A class injector is capable of injecting classes into a {@link java.lang.ClassLoader} without
 * requiring the class loader to being able to explicitly look up these classes.
 */
public interface ClassInjector {

    /**
     * A convenience reference to the default protection domain which is {@code null}.
     */
    ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    /**
     * Injects the given types into the represented class loader.
     *
     * @param types The types to load via injection.
     * @return The loaded types that were passed as arguments.
     */
    Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types);

    /**
     * A class injector that uses reflective method calls.
     */
    class UsingReflection implements ClassInjector {

        /**
         * A storage for the reflection method representations that are obtained on loading this classes.
         */
        private static final ReflectionStore REFLECTION_STORE;

        /*
         * Obtains the reflective instances used by this injector or a no-op instance that throws the exception
         * that occurred when attempting to obtain the reflective member instances.
         */
        static {
            ReflectionStore reflectionStore;
            try {
                Method findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                findLoadedClassMethod.setAccessible(true);
                Method loadByteArrayMethod = ClassLoader.class.getDeclaredMethod("defineClass",
                        String.class,
                        byte[].class,
                        int.class,
                        int.class,
                        ProtectionDomain.class);
                loadByteArrayMethod.setAccessible(true);
                reflectionStore = new ReflectionStore.Resolved(findLoadedClassMethod, loadByteArrayMethod);
            } catch (Exception e) {
                reflectionStore = new ReflectionStore.Faulty(e);
            }
            REFLECTION_STORE = reflectionStore;
        }

        /**
         * The class loader into which the classes are to be injected.
         */
        private final ClassLoader classLoader;

        /**
         * The protection domain that is used when loading classes.
         */
        private final ProtectionDomain protectionDomain;

        /**
         * The access control context of this class loader's instantiation.
         */
        private final AccessControlContext accessControlContext;

        /**
         * Creates a new injector for the given {@link java.lang.ClassLoader} and a default
         * {@link java.security.ProtectionDomain}.
         *
         * @param classLoader The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
         */
        public UsingReflection(ClassLoader classLoader) {
            this(classLoader, DEFAULT_PROTECTION_DOMAIN);
        }

        /**
         * Creates a new injector for the given {@link java.lang.ClassLoader} and {@link java.security.ProtectionDomain}.
         *
         * @param classLoader      The {@link java.lang.ClassLoader} into which new class definitions are to be injected.
         * @param protectionDomain The protection domain to apply during class definition.
         */
        public UsingReflection(ClassLoader classLoader, ProtectionDomain protectionDomain) {
            if (classLoader == null) {
                throw new IllegalArgumentException("Cannot inject classes into the bootstrap class loader");
            }
            this.classLoader = classLoader;
            this.protectionDomain = protectionDomain;
            accessControlContext = AccessController.getContext();
        }

        @Override
        public Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types) {
            try {
                Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>(types.size());
                synchronized (classLoader) {
                    for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                        Class<?> type = (Class<?>) REFLECTION_STORE.getFindLoadedClassMethod().invoke(classLoader, entry.getKey().getName());
                        if (type == null) {
                            try {
                                type = AccessController.doPrivileged(new ClassLoadingAction(entry.getKey().getName(), entry.getValue()), accessControlContext);
                            } catch (PrivilegedActionException e) {
                                if (e.getCause() instanceof IllegalAccessException) {
                                    throw (IllegalAccessException) e.getCause();
                                } else if (e.getCause() instanceof InvocationTargetException) {
                                    throw (InvocationTargetException) e.getCause();
                                } else {
                                    throw (RuntimeException) e.getCause();
                                }
                            }
                        }
                        loaded.put(entry.getKey(), type);
                    }
                }
                return loaded;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not access injection method", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Exception on invoking loader method", e.getCause());
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            UsingReflection that = (UsingReflection) other;
            return accessControlContext.equals(that.accessControlContext)
                    && classLoader.equals(that.classLoader)
                    && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null);
        }

        @Override
        public int hashCode() {
            int result = classLoader.hashCode();
            result = 31 * result + (protectionDomain != null ? protectionDomain.hashCode() : 0);
            result = 31 * result + accessControlContext.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ClassInjector.UsingReflection{" +
                    "classLoader=" + classLoader +
                    ", protectionDomain=" + protectionDomain +
                    ", accessControlContext=" + accessControlContext +
                    '}';
        }

        /**
         * A storage for method representations in order to access a class loader reflectively.
         */
        protected interface ReflectionStore {

            /**
             * Returns the method for finding a class on a class loader.
             *
             * @return The method for finding a class on a class loader.
             */
            Method getFindLoadedClassMethod();

            /**
             * Returns the method for loading a class into a class loader.
             *
             * @return The method for loading a class into a class loader.
             */
            Method getLoadByteArrayMethod();

            /**
             * Represents a successfully loaded method lookup.
             */
            class Resolved implements ReflectionStore {

                /**
                 * The method for finding a class on a class loader.
                 */
                private final Method findLoadedClassMethod;

                /**
                 * The method for loading a class into a class loader.
                 */
                private final Method loadByteArrayMethod;

                /**
                 * Creates a new resolved reflection store.
                 *
                 * @param findLoadedClassMethod The method for finding a class on a class loader.
                 * @param loadByteArrayMethod   The method for loading a class into a class loader.
                 */
                protected Resolved(Method findLoadedClassMethod, Method loadByteArrayMethod) {
                    this.findLoadedClassMethod = findLoadedClassMethod;
                    this.loadByteArrayMethod = loadByteArrayMethod;
                }

                @Override
                public Method getFindLoadedClassMethod() {
                    return findLoadedClassMethod;
                }

                @Override
                public Method getLoadByteArrayMethod() {
                    return loadByteArrayMethod;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Resolved resolved = (Resolved) other;
                    return findLoadedClassMethod.equals(resolved.findLoadedClassMethod)
                            && loadByteArrayMethod.equals(resolved.loadByteArrayMethod);
                }

                @Override
                public int hashCode() {
                    int result = findLoadedClassMethod.hashCode();
                    result = 31 * result + loadByteArrayMethod.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "ClassInjector.UsingReflection.ReflectionStore.Resolved{" +
                            "findLoadedClassMethod=" + findLoadedClassMethod +
                            ", loadByteArrayMethod=" + loadByteArrayMethod +
                            '}';
                }
            }

            /**
             * Represents an unsuccessfully loaded method lookup.
             */
            class Faulty implements ReflectionStore {

                /**
                 * The message to display in an exception.
                 */
                private static final String MESSAGE = "Cannot access reflection API for class loading";

                /**
                 * The exception that occurred when looking up the reflection methods.
                 */
                private final Exception exception;

                /**
                 * Creates a new faulty reflection store.
                 *
                 * @param exception The exception that was thrown when attempting to lookup the method.
                 */
                protected Faulty(Exception exception) {
                    this.exception = exception;
                }

                @Override
                public Method getFindLoadedClassMethod() {
                    throw new RuntimeException(MESSAGE, exception);
                }

                @Override
                public Method getLoadByteArrayMethod() {
                    throw new RuntimeException(MESSAGE, exception);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && exception.equals(((Faulty) other).exception);
                }

                @Override
                public int hashCode() {
                    return exception.hashCode();
                }

                @Override
                public String toString() {
                    return "ClassInjector.UsingReflection.ReflectionStore.Faulty{exception=" + exception + '}';
                }
            }
        }

        /**
         * A privileged action for loading a class reflectively.
         */
        protected class ClassLoadingAction implements PrivilegedExceptionAction<Class<?>> {

            /**
             * A convenience variable representing the first index of an array, to make the code more readable.
             */
            private static final int FROM_BEGINNING = 0;

            /**
             * The name of the class that is being loaded.
             */
            private final String name;

            /**
             * The binary representation of the class that is being loaded.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new class loading action.
             *
             * @param name                 The name of the class that is being loaded.
             * @param binaryRepresentation The binary representation of the class that is being loaded.
             */
            protected ClassLoadingAction(String name, byte[] binaryRepresentation) {
                this.name = name;
                this.binaryRepresentation = binaryRepresentation;
            }

            @Override
            public Class<?> run() throws IllegalAccessException, InvocationTargetException {
                return (Class<?>) REFLECTION_STORE.getLoadByteArrayMethod().invoke(classLoader,
                        name,
                        binaryRepresentation,
                        FROM_BEGINNING,
                        binaryRepresentation.length,
                        protectionDomain);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ClassLoadingAction that = (ClassLoadingAction) other;
                return Arrays.equals(binaryRepresentation, that.binaryRepresentation)
                        && UsingReflection.this.equals(that.getOuter())
                        && name.equals(that.name);
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private UsingReflection getOuter() {
                return UsingReflection.this;
            }

            @Override
            public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + UsingReflection.this.hashCode();
                result = 31 * result + Arrays.hashCode(binaryRepresentation);
                return result;
            }

            @Override
            public String toString() {
                return "ClassInjector.UsingReflection.ClassLoadingAction{" +
                        "injector=" + UsingReflection.this +
                        ", name='" + name + '\'' +
                        ", binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                        '}';
            }
        }
    }

    /**
     * A class injector using a {@link java.lang.instrument.Instrumentation} to append to either the boot classpath
     * or the system class path.
     */
    class UsingInstrumentation implements ClassInjector {

        /**
         * A prefix to use of generated files.
         */
        private static final String PREFIX = "jar";

        /**
         * The class file extension.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * The instrumentation to use for appending to the class path or the boot path.
         */
        private final Instrumentation instrumentation;

        /**
         * A representation of the target path to which classes are to be appended.
         */
        private final Target target;

        /**
         * The folder to be used for storing jar files.
         */
        private final File folder;

        /**
         * A random string generator for creating file names.
         */
        private final RandomString randomString;

        /**
         * Creates an instrumentation-based class injector.
         *
         * @param folder          The folder to be used for storing jar files.
         * @param target          A representation of the target path to which classes are to be appended.
         * @param instrumentation The instrumentation to use for appending to the class path or the boot path.
         */
        public UsingInstrumentation(File folder, Target target, Instrumentation instrumentation) {
            this.folder = folder;
            this.target = target;
            this.instrumentation = instrumentation;
            randomString = new RandomString();
        }

        @Override
        public Map<TypeDescription, Class<?>> inject(Map<? extends TypeDescription, byte[]> types) {
            File jarFile = new File(folder, String.format("%s%s.jar", PREFIX, randomString.nextString()));
            try {
                if (!jarFile.createNewFile()) {
                    throw new IllegalStateException("Cannot create file " + jarFile);
                }
                JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)));
                try {
                    for (Map.Entry<? extends TypeDescription, byte[]> entry : types.entrySet()) {
                        jarOutputStream.putNextEntry(new JarEntry(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION));
                        jarOutputStream.write(entry.getValue());
                    }
                } finally {
                    jarOutputStream.close();
                }
                target.inject(instrumentation, new JarFile(jarFile));
                Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>(types.size());
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                for (TypeDescription typeDescription : types.keySet()) {
                    loaded.put(typeDescription, classLoader.loadClass(typeDescription.getName()));
                }
                return loaded;
            } catch (IOException e) {
                throw new IllegalStateException("Cannot write jar file to disk", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load injected class", e);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            UsingInstrumentation that = (UsingInstrumentation) other;
            return folder.equals(that.folder)
                    && instrumentation.equals(that.instrumentation)
                    && target == that.target;
        }

        @Override
        public int hashCode() {
            int result = instrumentation.hashCode();
            result = 31 * result + target.hashCode();
            result = 31 * result + folder.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ClassInjector.UsingInstrumentation{" +
                    "instrumentation=" + instrumentation +
                    ", target=" + target +
                    ", folder=" + folder +
                    ", randomString=" + randomString +
                    '}';
        }

        /**
         * A representation of the target to which Java classes should be appended to.
         */
        public enum Target {

            /**
             * Representation of the bootstrap class loader.
             */
            BOOTSTRAP {
                @Override
                protected void inject(Instrumentation instrumentation, JarFile jarFile) {
                    instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
                }
            },

            /**
             * Representation of the system class loader.
             */
            SYSTEM {
                @Override
                protected void inject(Instrumentation instrumentation, JarFile jarFile) {
                    instrumentation.appendToSystemClassLoaderSearch(jarFile);
                }
            };

            /**
             * Adds the given classes to the represented class loader.
             *
             * @param instrumentation The instrumentation instance to use.
             * @param jarFile         The jar file to append.
             */
            protected abstract void inject(Instrumentation instrumentation, JarFile jarFile);

            @Override
            public String toString() {
                return "ClassInjector.UsingInstrumentation.Target." + name();
            }
        }
    }
}
