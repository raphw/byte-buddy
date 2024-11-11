/*
 * Copyright 2014 - Present Rafael Winterhalter
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
package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.StreamDrainer;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.bytebuddy.matcher.ElementMatchers.isChildOf;

/**
 * Locates a class file or its byte array representation when it is given its type description.
 */
public interface ClassFileLocator extends Closeable {

    /**
     * The file extension for a Java class file.
     */
    String CLASS_FILE_EXTENSION = ".class";

    /**
     * The prefix folder for {@code META-INF/versions/} which contains multi-release files.
     */
    String META_INF_VERSIONS = "META-INF/versions/";

    /**
     * Locates the class file for a given type and returns the binary data of the class file.
     *
     * @param name The name of the type to locate a class file representation for.
     * @return Any binary representation of the type which might be illegal.
     * @throws java.io.IOException If reading a class file causes an error.
     */
    Resolution locate(String name) throws IOException;

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
        @HashCodeAndEqualsPlugin.Enhance
        class Illegal implements Resolution {

            /**
             * The name of the unresolved class file.
             */
            private final String typeName;

            /**
             * Creates an illegal resolution for a class file.
             *
             * @param typeName The name of the unresolved class file.
             */
            public Illegal(String typeName) {
                this.typeName = typeName;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isResolved() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public byte[] resolve() {
                throw new IllegalStateException("Could not locate class file for " + typeName);
            }
        }

        /**
         * Represents a byte array as binary data.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
            @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The array is not modified by class contract.")
            public Explicit(byte[] binaryRepresentation) {
                this.binaryRepresentation = binaryRepresentation;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isResolved() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The array is not modified by class contract.")
            public byte[] resolve() {
                return binaryRepresentation;
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

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            return new Resolution.Illegal(name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * A class file locator that is aware of multi-release JAR file semantics.
     */
    @HashCodeAndEqualsPlugin.Enhance
    abstract class MultiReleaseAware implements ClassFileLocator {

        /**
         * The property name of a multi-release JAR file.
         */
        private static final String MULTI_RELEASE_ATTRIBUTE = "Multi-Release";

        /**
         * Indicates that no multi-release versions exist.
         */
        protected static final int[] NO_MULTI_RELEASE = new int[0];

        /**
         * Contains the existing multi-release jar folders that are available for the
         * current JVM version in decreasing order.
         */
        private final int[] version;

        /**
         * Creates a multi-release aware class file locator.
         *
         * @param version Contains the existing multi-release jar folders that are available for the
         *                current JVM version in decreasing order.
         */
        protected MultiReleaseAware(int[] version) {
            this.version = version;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            String path = name.replace('.', '/') + CLASS_FILE_EXTENSION;
            for (int index = 0; index < version.length + 1; index++) {
                byte[] binaryRepresentation = doLocate(index == version.length ? path : META_INF_VERSIONS + version[index] + "/" + path);
                if (binaryRepresentation != null) {
                    return new Resolution.Explicit(binaryRepresentation);
                }
            }
            return new Resolution.Illegal(name);
        }

        /**
         * Resolves a possible multi-release entry, if it exists.
         *
         * @param path The path of the class file.
         * @return The class file's binary representation or {@code null} if it does not exist.
         * @throws IOException If an I/O exception occurs.
         */
        @MaybeNull
        protected abstract byte[] doLocate(String path) throws IOException;
    }

    /**
     * A simple class file locator that returns class files from a selection of given types.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
         * Creates a class file locator that represents all types of a dynamic type.
         *
         * @param dynamicType The dynamic type to represent.
         * @return A class file locator representing the dynamic type's types.
         */
        public static ClassFileLocator of(DynamicType dynamicType) {
            return of(dynamicType.getAllTypes());
        }

        /**
         * Creates a class file locator that represents all types of a dynamic type.
         *
         * @param binaryRepresentations The binary representation of all types.
         * @return A class file locator representing the dynamic type's types.
         */
        public static ClassFileLocator of(Map<TypeDescription, byte[]> binaryRepresentations) {
            Map<String, byte[]> classFiles = new HashMap<String, byte[]>();
            for (Map.Entry<TypeDescription, byte[]> entry : binaryRepresentations.entrySet()) {
                classFiles.put(entry.getKey().getName(), entry.getValue());
            }
            return new Simple(classFiles);
        }

        /**
         * Creates a class file locator of a map of resources where class files are mapped by their path and file extension.
         *
         * @param binaryRepresentations A map of resource names to their binary representation.
         * @return A class file locator that finds class files within the map.
         */
        public static ClassFileLocator ofResources(Map<String, byte[]> binaryRepresentations) {
            Map<String, byte[]> classFiles = new HashMap<String, byte[]>();
            for (Map.Entry<String, byte[]> entry : binaryRepresentations.entrySet()) {
                if (entry.getKey().endsWith(CLASS_FILE_EXTENSION)) {
                    classFiles.put(entry.getKey().substring(0, entry.getKey().length() - CLASS_FILE_EXTENSION.length()).replace('/', '.'), entry.getValue());
                }
            }
            return new Simple(classFiles);
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            byte[] binaryRepresentation = classFiles.get(name);
            return binaryRepresentation == null ? new Resolution.Illegal(name) : new Resolution.Explicit(binaryRepresentation);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * <p>
     * A class file locator that queries a class loader for binary representations of class files.
     * </p>
     * <p>
     * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
     * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
     * </p>
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForClassLoader implements ClassFileLocator {

        /**
         * A class loader that does not define resources of its own but allows querying for resources supplied by the boot loader.
         */
        private static final ClassLoader BOOT_LOADER_PROXY = doPrivileged(BootLoaderProxyCreationAction.INSTANCE);

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
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Creates a class file locator that queries the system class loader.
         *
         * @return A class file locator that queries the system class loader.
         */
        public static ClassFileLocator ofSystemLoader() {
            return new ForClassLoader(ClassLoader.getSystemClassLoader());
        }

        /**
         * Creates a class file locator that queries the plaform class loader or the extension class loader if the
         * current VM is not at least of version 9.
         *
         * @return A class file locator that queries the plaform class loader or the extension class loader.
         */
        public static ClassFileLocator ofPlatformLoader() {
            return of(ClassLoader.getSystemClassLoader().getParent());
        }

        /**
         * Creates a class file locator that queries the boot loader.
         *
         * @return A class file locator that queries the boot loader.
         */
        public static ClassFileLocator ofBootLoader() {
            return new ForClassLoader(BOOT_LOADER_PROXY);
        }

        /**
         * Creates a class file locator for a given class loader.
         *
         * @param classLoader The class loader to be used which might be {@code null} to represent the bootstrap loader.
         * @return A corresponding source locator.
         */
        public static ClassFileLocator of(@MaybeNull ClassLoader classLoader) {
            return new ForClassLoader(classLoader == null ? BOOT_LOADER_PROXY : classLoader);
        }

        /**
         * Attempts to create a binary representation of a loaded type by requesting data from its
         * {@link java.lang.ClassLoader}.
         *
         * @param type The type of interest.
         * @return The binary representation of the supplied type.
         */
        public static byte[] read(Class<?> type) {
            try {
                ClassLoader classLoader = type.getClassLoader();
                return locate(classLoader == null ? BOOT_LOADER_PROXY : classLoader, TypeDescription.ForLoadedType.getName(type)).resolve();
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read class file for " + type, exception);
            }
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param type The types of interest.
         * @return A mapping of the supplied types to their binary representation.
         */
        public static Map<Class<?>, byte[]> read(Class<?>... type) {
            return read(Arrays.asList(type));
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param types The types of interest.
         * @return A mapping of the supplied types to their binary representation.
         */
        public static Map<Class<?>, byte[]> read(Collection<? extends Class<?>> types) {
            Map<Class<?>, byte[]> result = new HashMap<Class<?>, byte[]>();
            for (Class<?> type : types) {
                result.put(type, read(type));
            }
            return result;
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param type The types of interest.
         * @return A mapping of the supplied types' names to their binary representation.
         */
        public static Map<String, byte[]> readToNames(Class<?>... type) {
            return readToNames(Arrays.asList(type));
        }

        /**
         * Attempts to create a binary representation of several loaded types by requesting
         * data from their respective {@link java.lang.ClassLoader}s.
         *
         * @param types The types of interest.
         * @return A mapping of the supplied types' names to their binary representation.
         */
        public static Map<String, byte[]> readToNames(Collection<? extends Class<?>> types) {
            Map<String, byte[]> result = new HashMap<String, byte[]>();
            for (Class<?> type : types) {
                result.put(type.getName(), read(type));
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            return locate(classLoader, name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }

        /**
         * Locates the class file for the supplied type by requesting a resource from the class loader.
         *
         * @param classLoader The class loader to query.
         * @param name        The name of the type for which to locate a class file.
         * @return A resolution for the class file.
         * @throws IOException If reading the class file causes an exception.
         */
        protected static Resolution locate(ClassLoader classLoader, String name) throws IOException {
            InputStream inputStream = classLoader.getResourceAsStream(name.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (inputStream != null) {
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            } else {
                return new Resolution.Illegal(name);
            }
        }

        /**
         * A privileged action for creating a proxy class loader for the boot class loader.
         */
        protected enum BootLoaderProxyCreationAction implements PrivilegedAction<ClassLoader> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public ClassLoader run() {
                return new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
            }
        }

        /**
         * <p>
         * A class file locator that queries a class loader for binary representations of class files.
         * The class loader is only weakly referenced.
         * </p>
         * <p>
         * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
         * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
         * </p>
         */
        public static class WeaklyReferenced extends WeakReference<ClassLoader> implements ClassFileLocator {

            /**
             * The represented class loader's hash code.
             */
            private final int hashCode;

            /**
             * Creates a class file locator for a class loader that is weakly referenced.
             *
             * @param classLoader The class loader to represent.
             */
            protected WeaklyReferenced(ClassLoader classLoader) {
                super(classLoader);
                hashCode = System.identityHashCode(classLoader);
            }

            /**
             * Creates a class file locator for a given class loader. If the class loader is not the bootstrap
             * class loader or the system class loader which cannot be collected, the class loader is only weakly
             * referenced.
             *
             * @param classLoader The class loader to be used. If this class loader represents the bootstrap class
             *                    loader which is represented by the {@code null} value, this system class loader
             *                    is used instead.
             * @return A corresponding source locator.
             */
            public static ClassFileLocator of(@MaybeNull ClassLoader classLoader) {
                return classLoader == null || classLoader == ClassLoader.getSystemClassLoader() || classLoader == ClassLoader.getSystemClassLoader().getParent() ? ForClassLoader.of(classLoader) : new WeaklyReferenced(classLoader);
            }

            /**
             * {@inheritDoc}
             */
            public Resolution locate(String name) throws IOException {
                ClassLoader classLoader = get();
                return classLoader == null ? new Resolution.Illegal(name) : ForClassLoader.locate(classLoader, name);
            }

            /**
             * {@inheritDoc}
             */
            public void close() {
                /* do nothing */
            }

            @Override
            public int hashCode() {
                return hashCode;
            }

            @Override
            public boolean equals(@MaybeNull Object other) {
                if (this == other) {
                    return true;
                } else if (other == null || getClass() != other.getClass()) {
                    return false;
                }
                WeaklyReferenced weaklyReferenced = (WeaklyReferenced) other;
                ClassLoader classLoader = weaklyReferenced.get();
                return classLoader != null && get() == classLoader;
            }
        }
    }

    /**
     * <p>
     * A class file locator that locates class files by querying a Java module's {@code getResourceAsStream} method.
     * </p>
     * <p>
     * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
     * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
     * </p>
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForModule implements ClassFileLocator {

        /**
         * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
         */
        private static final Object[] NO_ARGUMENT = new Object[0];

        /**
         * The represented Java module.
         */
        private final JavaModule module;

        /**
         * Creates a new class file locator for a Java module.
         *
         * @param module The represented Java module.
         */
        protected ForModule(JavaModule module) {
            this.module = module;
        }

        /**
         * Returns a class file locator that exposes all class files of the boot module layer. This class file locator is only available
         * on virtual machines of version 9 or later. On earlier versions, the returned class file locator does not locate any resources.
         *
         * @return A class file locator that locates classes of the boot layer.
         */
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should always be wrapped for clarity")
        public static ClassFileLocator ofBootLayer() {
            try {
                Map<String, ClassFileLocator> bootModules = new HashMap<String, ClassFileLocator>();
                Class<?> layerType = Class.forName("java.lang.ModuleLayer");
                Method getPackages = JavaType.MODULE.load().getMethod("getPackages");
                for (Object rawModule : (Set<?>) layerType.getMethod("modules").invoke(layerType.getMethod("boot").invoke(null))) {
                    ClassFileLocator classFileLocator = ForModule.of(JavaModule.of(rawModule));
                    for (Object packageName : (Set<?>) getPackages.invoke(rawModule, NO_ARGUMENT)) {
                        bootModules.put((String) packageName, classFileLocator);
                    }
                }
                return new PackageDiscriminating(bootModules);
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot process boot layer", exception);
            }
        }

        /**
         * Returns a class file locator for the provided module. If the provided module is not named, class files are located via this
         * unnamed module's class loader.
         *
         * @param module The module to create a class file locator for.
         * @return An appropriate class file locator.
         */
        public static ClassFileLocator of(JavaModule module) {
            return module.isNamed() ? new ForModule(module) : ForClassLoader.of(module.getClassLoader());
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            return locate(module, name);
        }

        /**
         * Creates a resolution for a Java module's class files.
         *
         * @param module   The Java module to query.
         * @param typeName The name of the type being queried.
         * @return A resolution for the query.
         * @throws IOException If an I/O exception was thrown.
         */
        protected static Resolution locate(JavaModule module, String typeName) throws IOException {
            InputStream inputStream = module.getResourceAsStream(typeName.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (inputStream != null) {
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            } else {
                return new Resolution.Illegal(typeName);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }

        /**
         * <p>
         * A class file locator for a Java module that only references this module weakly. If a module was garbage collected,
         * this class file locator only returns unresolved resolutions.
         * </p>
         * <p>
         * <b>Important</b>: Even when calling {@link Closeable#close()} on this class file locator, no underlying
         * class loader is closed if it implements the {@link Closeable} interface as this is typically not intended.
         * </p>
         */
        public static class WeaklyReferenced extends WeakReference<Object> implements ClassFileLocator {

            /**
             * The represented module's hash code.
             */
            private final int hashCode;

            /**
             * Creates a class file locator for a Java module that is weakly referenced.
             *
             * @param module The raw Java module to represent.
             */
            protected WeaklyReferenced(Object module) {
                super(module);
                hashCode = System.identityHashCode(module);
            }

            /**
             * Creates a class file locator for a Java module where the module is referenced weakly. If the module is not named, the module's class loader
             * is represented instead. Module's of the boot layer are not referenced weakly.
             *
             * @param module The Java module to represent.
             * @return A suitable class file locator.
             */
            public static ClassFileLocator of(JavaModule module) {
                if (module.isNamed()) {
                    return module.getClassLoader() == null || module.getClassLoader() == ClassLoader.getSystemClassLoader() || module.getClassLoader() == ClassLoader.getSystemClassLoader().getParent() ? new ForModule(module) : new WeaklyReferenced(module.unwrap());
                } else {
                    return ForClassLoader.WeaklyReferenced.of(module.getClassLoader());
                }
            }

            /**
             * {@inheritDoc}
             */
            public Resolution locate(String name) throws IOException {
                Object module = get();
                return module == null ? new Resolution.Illegal(name) : ForModule.locate(JavaModule.of(module), name);
            }

            /**
             * {@inheritDoc}
             */
            public void close() {
                /* do nothing */
            }

            /**
             * {@inheritDoc}
             */
            public int hashCode() {
                return hashCode;
            }

            /**
             * {@inheritDoc}
             */
            public boolean equals(@MaybeNull Object other) {
                if (this == other) {
                    return true;
                } else if (other == null || getClass() != other.getClass()) {
                    return false;
                }
                WeaklyReferenced weaklyReferenced = (WeaklyReferenced) other;
                Object module = weaklyReferenced.get();
                return module != null && get() == module;
            }
        }
    }

    /**
     * A class file locator that locates classes within a Java <i>jar</i> file.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForJarFile extends MultiReleaseAware {

        /**
         * A list of potential locations of the runtime jar for different platforms.
         */
        private static final List<String> RUNTIME_LOCATIONS = Arrays.asList("lib/rt.jar", "../lib/rt.jar", "../Classes/classes.jar");

        /**
         * The jar file to read from.
         */
        private final JarFile jarFile;

        /**
         * Indicates if the jar file should be closed upon closing this class file locator.
         */
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
        private final boolean close;

        /**
         * Creates a new class file locator for the given jar file. The jar file will not be closed
         * upon closing this class file locator.
         *
         * @param jarFile The jar file to read from.
         */
        public ForJarFile(JarFile jarFile) {
            this(NO_MULTI_RELEASE, jarFile, false);
        }

        /**
         * Creates a new class file locator for the given jar file.
         *
         * @param version Contains the existing multi-release jar folders that are available for the
         *                current JVM version in decreasing order.
         * @param jarFile The jar file to read from.
         * @param close   Indicates if the jar file should be closed upon closing this class file locator.
         */
        protected ForJarFile(int[] version, JarFile jarFile, boolean close) {
            super(version);
            this.jarFile = jarFile;
            this.close = close;
        }

        /**
         * Creates a new class file locator for the given jar file. Multi-release jars are not considered.
         *
         * @param file The jar file to read from.
         * @return A class file locator for the jar file.
         * @throws IOException If an I/O exception is thrown.
         */
        public static ClassFileLocator of(File file) throws IOException {
            return new ForJarFile(MultiReleaseAware.NO_MULTI_RELEASE, new JarFile(file, false, ZipFile.OPEN_READ), true);
        }

        /**
         * Creates a new class file locator for the given jar file. Multi-release jar files
         * are resolved as if executed on a JVM of the supplied version.
         *
         * @param file             The jar file to read from.
         * @param classFileVersion The class file version to consider when resolving class files in multi-release jars.
         * @return A class file locator for the jar file.
         * @throws IOException If an I/O exception is thrown.
         */
        public static ClassFileLocator of(File file, ClassFileVersion classFileVersion) throws IOException {
            return of(new JarFile(file, false, ZipFile.OPEN_READ), classFileVersion, true);
        }

        /**
         * Creates a new class file locator for the given jar file. Multi-release jar files
         * are resolved as if executed on a JVM of the supplied version. The jar file will not be closed
         * upon closing this class file locator.
         *
         * @param jarFile          The jar file to read from.
         * @param classFileVersion The class file version to consider when resolving class files in multi-release jars.
         * @return A class file locator for the jar file.
         * @throws IOException If an I/O exception is thrown.
         */
        public static ClassFileLocator of(JarFile jarFile, ClassFileVersion classFileVersion) throws IOException {
            return of(jarFile, classFileVersion, false);
        }

        /**
         * Creates a new class file locator for the given jar file. Multi-release jar files
         * are resolved as if executed on a JVM of the supplied version.
         *
         * @param jarFile          The jar file to read from.
         * @param classFileVersion The class file version to consider when resolving class files in multi-release jars.
         * @param close            Indicates if the jar file should be closed upon closing this class file locator.
         * @return A class file locator for the jar file.
         * @throws IOException If an I/O exception is thrown.
         */
        private static ClassFileLocator of(JarFile jarFile, ClassFileVersion classFileVersion, boolean close) throws IOException {
            if (classFileVersion.getJavaVersion() < 9) {
                return new ForJarFile(jarFile);
            } else {
                Manifest manifest = jarFile.getManifest();
                int[] version;
                if (manifest != null && Boolean.parseBoolean(manifest.getMainAttributes().getValue(MultiReleaseAware.MULTI_RELEASE_ATTRIBUTE))) {
                    SortedSet<Integer> versions = new TreeSet<Integer>();
                    Enumeration<JarEntry> enumeration = jarFile.entries();
                    while (enumeration.hasMoreElements()) {
                        String name = enumeration.nextElement().getName();
                        if (name.endsWith(CLASS_FILE_EXTENSION) && name.startsWith(META_INF_VERSIONS)) {
                            try {
                                int candidate = Integer.parseInt(name.substring(META_INF_VERSIONS.length(), name.indexOf('/', META_INF_VERSIONS.length())));
                                if (candidate > 7 && candidate <= classFileVersion.getJavaVersion()) {
                                    versions.add(candidate);
                                }
                            } catch (NumberFormatException ignored) {
                                /* do nothing */
                            }
                        }
                    }
                    version = new int[versions.size()];
                    Iterator<Integer> iterator = versions.iterator();
                    for (int index = 0; index < versions.size(); index++) {
                        version[versions.size() - index - 1] = iterator.next();
                    }
                } else {
                    version = MultiReleaseAware.NO_MULTI_RELEASE;
                }
                return new ForJarFile(version, jarFile, close);
            }
        }

        /**
         * Resolves a class file locator for the class path that reads class files directly from the file system. The resulting
         * class file locator does not imply classes on the boot path.
         *
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofClassPath() throws IOException {
            return ofClassPath(System.getProperty("java.class.path"));
        }

        /**
         * <p>
         * Resolves a class file locator for the class path that reads class files directly from the file system.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @param classPath The class path to scan with the elements separated by {@code path.separator}.
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofClassPath(String classPath) throws IOException {
            ClassFileVersion classFileVersion = ClassFileVersion.ofThisVm();
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            for (String element : Pattern.compile(File.pathSeparator, Pattern.LITERAL).split(classPath)) {
                File file = new File(element);
                if (file.isDirectory()) {
                    classFileLocators.add(ForFolder.of(file, classFileVersion));
                } else if (file.isFile()) {
                    classFileLocators.add(of(file, classFileVersion));
                }
            }
            return new Compound(classFileLocators);
        }

        /**
         * Resolves a class file locator for the runtime jar. If such a file does not exist or cannot be located, a runtime exception is thrown.
         *
         * @return A class file locator for the runtime jar, if available.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofRuntimeJar() throws IOException {
            String javaHome = System.getProperty("java.home").replace('\\', '/');
            File runtimeJar = null;
            for (String location : RUNTIME_LOCATIONS) {
                File candidate = new File(javaHome, location);
                if (candidate.isFile()) {
                    runtimeJar = candidate;
                    break;
                }
            }
            if (runtimeJar == null) {
                throw new IllegalStateException("Runtime jar does not exist in " + javaHome + " for any of " + RUNTIME_LOCATIONS);
            }
            return of(runtimeJar);
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        @SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", justification = "Null value indicates failed lookup.")
        protected byte[] doLocate(String path) throws IOException {
            ZipEntry zipEntry = jarFile.getEntry(path);
            if (zipEntry == null) {
                return null;
            } else {
                InputStream inputStream = jarFile.getInputStream(zipEntry);
                try {
                    return StreamDrainer.DEFAULT.drain(inputStream);
                } finally {
                    inputStream.close();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            if (close) {
                jarFile.close();
            }
        }
    }

    /**
     * A class file locator that locates classes within a Java <i>jmod</i> file. This class file locator should not be used
     * for reading modular jar files for which {@link ForJarFile} is appropriate.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForModuleFile implements ClassFileLocator {

        /**
         * The file extension of a modular Java package.
         */
        private static final String JMOD_FILE_EXTENSION = ".jmod";

        /**
         * A list of potential locations of the boot path for different platforms.
         */
        private static final List<String> BOOT_LOCATIONS = Arrays.asList("jmods", "../jmods", "modules");

        /**
         * The represented jmod file.
         */
        private final ZipFile zipFile;

        /**
         * Creates a new class file locator for a jmod file.
         *
         * @param zipFile The represented jmod file.
         */
        public ForModuleFile(ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        /**
         * Creates a new class file locator for this VM's boot module path.
         *
         * @return A class file locator for this VM's boot module path.
         * @throws IOException If an I/O error occurs.
         */
        public static ClassFileLocator ofBootPath() throws IOException {
            String javaHome = System.getProperty("java.home").replace('\\', '/');
            File bootPath = null;
            for (String location : BOOT_LOCATIONS) {
                File candidate = new File(javaHome, location);
                if (candidate.isDirectory()) {
                    bootPath = candidate;
                    break;
                }
            }
            if (bootPath == null) {
                throw new IllegalStateException("Boot modules do not exist in " + javaHome + " for any of " + BOOT_LOCATIONS);
            }
            return ofBootPath(bootPath);
        }

        /**
         * Creates a new class file locator for a Java boot module path.
         *
         * @param bootPath The boot path folder.
         * @return A class file locator for this VMs boot module path.
         * @throws IOException If an I/O error occurs.
         */
        public static ClassFileLocator ofBootPath(File bootPath) throws IOException {
            File[] module = bootPath.listFiles();
            if (module == null) {
                return NoOp.INSTANCE;
            }
            ClassFileVersion classFileVersion = ClassFileVersion.ofThisVm();
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>(module.length);
            for (File aModule : module) {
                if (aModule.isFile()) {
                    classFileLocators.add(of(aModule));
                } else if (aModule.isDirectory()) { // Relevant for locally built OpenJDK.
                    classFileLocators.add(ForFolder.of(aModule, classFileVersion));
                }
            }
            return new Compound(classFileLocators);
        }

        /**
         * <p>
         * Resolves a class file locator for this VM's Java module path that reads class files directly from the file system.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofModulePath() throws IOException {
            String modulePath = System.getProperty("jdk.module.path");
            return modulePath == null ? NoOp.INSTANCE : ofModulePath(modulePath);
        }

        /**
         * <p>
         * Resolves a class file locator for a Java module path that reads class files directly from the file system. All
         * elements of the module path are resolved relative to this VM's {@code user.dir}.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @param modulePath The module path to scan with the elements separated by {@code path.separator}.
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofModulePath(String modulePath) throws IOException {
            return ofModulePath(modulePath, System.getProperty("user.dir"));
        }

        /**
         * <p>
         * Resolves a class file locator for a Java module path that reads class files directly from the file system.
         * </p>
         * <p>
         * <b>Note</b>: The resulting class file locator does not include classes of the bootstrap class loader.
         * </p>
         *
         * @param modulePath The module path to scan with the elements separated by {@code path.separator}.
         * @param baseFolder The relative location of the elements on the module path.
         * @return A class file locator for the class path.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator ofModulePath(String modulePath, String baseFolder) throws IOException {
            ClassFileVersion classFileVersion = ClassFileVersion.ofThisVm();
            List<ClassFileLocator> classFileLocators = new ArrayList<ClassFileLocator>();
            for (String element : Pattern.compile(System.getProperty("path.separator"), Pattern.LITERAL).split(modulePath)) {
                File file = new File(baseFolder, element);
                if (file.isDirectory()) {
                    File[] module = file.listFiles();
                    if (module != null) {
                        for (File aModule : module) {
                            if (aModule.isDirectory()) {
                                classFileLocators.add(ForFolder.of(aModule, classFileVersion));
                            } else if (aModule.isFile()) {
                                classFileLocators.add(aModule.getName().endsWith(JMOD_FILE_EXTENSION) ? of(aModule) : ForJarFile.of(aModule, classFileVersion));
                            }
                        }
                    }
                } else if (file.isFile()) {
                    classFileLocators.add(file.getName().endsWith(JMOD_FILE_EXTENSION) ? of(file) : ForJarFile.of(file));
                }
            }
            return new Compound(classFileLocators);
        }

        /**
         * Returns a class file locator for the given module file.
         *
         * @param file The module file.
         * @return A class file locator for the given module
         * @throws IOException If an I/O error occurs.
         */
        public static ClassFileLocator of(File file) throws IOException {
            return new ForModuleFile(new ZipFile(file));
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            ZipEntry zipEntry = zipFile.getEntry("classes/" + name.replace('.', '/') + CLASS_FILE_EXTENSION);
            if (zipEntry == null) {
                return new Resolution.Illegal(name);
            } else {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                try {
                    return new Resolution.Explicit(StreamDrainer.DEFAULT.drain(inputStream));
                } finally {
                    inputStream.close();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            zipFile.close();
        }
    }

    /**
     * A class file locator that finds files from a standardized Java folder structure with
     * folders donating packages and class files being saved as {@code <classname>.class} files
     * within their package folder.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForFolder extends MultiReleaseAware {

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
            this(NO_MULTI_RELEASE, folder);
        }

        /**
         * Creates a new class file locator for a folder structure of class files.
         *
         * @param version Contains the existing multi-release jar folders that are available for the
         *                current JVM version in decreasing order.
         * @param folder  The base folder of the package structure.
         */
        protected ForFolder(int[] version, File folder) {
            super(version);
            this.folder = folder;
        }

        /**
         * Creates a new class file locator for a folder structure of class files. The created locator considers
         * the provided class file version when resolving class files and if multiple versions are available
         *
         * @param folder           The base folder of the package structure.
         * @param classFileVersion The class file version to consider for multi-release JAR files.
         * @return An appropriate class file locator.
         * @throws IOException If an I/O exception occurs.
         */
        public static ClassFileLocator of(File folder, ClassFileVersion classFileVersion) throws IOException {
            if (classFileVersion.getJavaVersion() < 9) {
                return new ForFolder(NO_MULTI_RELEASE, folder);
            } else {
                File manifest = new File(folder, JarFile.MANIFEST_NAME);
                boolean multiRelease;
                if (manifest.exists()) {
                    InputStream inputStream = new FileInputStream(manifest);
                    try {
                        multiRelease = Boolean.parseBoolean(new Manifest(inputStream).getMainAttributes().getValue("Multi-Release"));
                    } finally {
                        inputStream.close();
                    }
                } else {
                    multiRelease = false;
                }
                int[] version;
                if (multiRelease) {
                    File[] file = new File(folder, META_INF_VERSIONS).listFiles();
                    if (file != null) {
                        SortedSet<Integer> versions = new TreeSet<Integer>();
                        for (int index = 0; index < file.length; index++) {
                            try {
                                int candidate = Integer.parseInt(file[index].getName());
                                if (candidate > 7 && candidate <= classFileVersion.getJavaVersion()) {
                                    versions.add(candidate);
                                }
                            } catch (NumberFormatException ignored) {
                                /* do nothing */
                            }
                        }
                        version = new int[versions.size()];
                        Iterator<Integer> iterator = versions.iterator();
                        for (int index = 0; index < versions.size(); index++) {
                            version[versions.size() - index - 1] = iterator.next();
                        }
                    } else {
                        version = NO_MULTI_RELEASE;
                    }
                } else {
                    version = NO_MULTI_RELEASE;
                }
                return new ForFolder(version, folder);
            }
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        @SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", justification = "Null value indicates failed lookup.")
        protected byte[] doLocate(String path) throws IOException {
            File file = new File(folder, path);
            if (file.exists()) {
                InputStream inputStream = new FileInputStream(file);
                try {
                    return StreamDrainer.DEFAULT.drain(inputStream);
                } finally {
                    inputStream.close();
                }
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * A class file locator that reads class files from one or several URLs. The reading is accomplished via using an {@link URLClassLoader}.
     * Doing so, boot loader resources might be located additionally to those found via the specified URLs.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForUrl implements ClassFileLocator {

        /**
         * The class loader that delegates to the URLs.
         */
        private final ClassLoader classLoader;

        /**
         * Creates a new class file locator for the given URLs.
         *
         * @param url The URLs to search for class files.
         */
        public ForUrl(URL... url) {
            classLoader = doPrivileged(new ClassLoaderCreationAction(url));
        }

        /**
         * Creates a new class file locator for the given URLs.
         *
         * @param urls The URLs to search for class files.
         */
        public ForUrl(Collection<? extends URL> urls) {
            this(urls.toArray(new URL[0]));
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            return ForClassLoader.locate(classLoader, name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            if (classLoader instanceof Closeable) {
                ((Closeable) classLoader).close();
            }
        }

        /**
         * An action to create a class loader with the purpose of locating classes from an URL location.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ClassLoaderCreationAction implements PrivilegedAction<ClassLoader> {

            /**
             * The URLs to locate classes from.
             */
            private final URL[] url;

            /**
             * Creates a new class loader creation action.
             *
             * @param url The URLs to locate classes from.
             */
            protected ClassLoaderCreationAction(URL[] url) {
                this.url = url;
            }

            /**
             * {@inheritDoc}
             */
            public ClassLoader run() {
                return new URLClassLoader(url, ClassLoadingStrategy.BOOTSTRAP_LOADER);
            }
        }
    }

    /**
     * A Java agent that allows the location of class files by emulating a retransformation. Note that this class file
     * locator causes a class to be loaded in order to look up its class file. Also, this locator does deliberately not
     * support the look-up of classes that represent lambda expressions.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForInstrumentation implements ClassFileLocator {

        /**
         * A dispatcher for interacting with the instrumentation API.
         */
        private static final Dispatcher DISPATCHER = doPrivileged(JavaDispatcher.of(Dispatcher.class));

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
         * @param classLoader     The class loader to read a class from or {@code null} to use the boot loader.
         */
        public ForInstrumentation(Instrumentation instrumentation, @MaybeNull ClassLoader classLoader) {
            this(instrumentation, ClassLoadingDelegate.Default.of(classLoader));
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation      The instrumentation to be used.
         * @param classLoadingDelegate The delegate responsible for class loading.
         */
        public ForInstrumentation(Instrumentation instrumentation, ClassLoadingDelegate classLoadingDelegate) {
            if (!DISPATCHER.isRetransformClassesSupported(instrumentation)) {
                throw new IllegalArgumentException(instrumentation + " does not support retransformation");
            }
            this.instrumentation = instrumentation;
            this.classLoadingDelegate = classLoadingDelegate;
        }

        /**
         * Resolves the instrumentation provided by {@code net.bytebuddy.agent.Installer}.
         *
         * @return The installed instrumentation instance.
         */
        private static Instrumentation resolveByteBuddyAgentInstrumentation() {
            try {
                Class<?> installer = ClassLoader.getSystemClassLoader().loadClass("net.bytebuddy.agent.Installer");
                JavaModule source = JavaModule.ofType(AgentBuilder.class), target = JavaModule.ofType(installer);
                if (source != null && !source.canRead(target)) {
                    Class<?> module = Class.forName("java.lang.Module");
                    module.getMethod("addReads", module).invoke(source.unwrap(), target.unwrap());
                }
                return (Instrumentation) installer.getMethod("getInstrumentation").invoke(null);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
            }
        }

        /**
         * Returns an agent-based class file locator for the given class loader and an already installed
         * Byte Buddy-agent.
         *
         * @param classLoader The class loader that is expected to load the looked-up a class.
         * @return A class file locator for the given class loader based on a Byte Buddy agent.
         */
        public static ClassFileLocator fromInstalledAgent(@MaybeNull ClassLoader classLoader) {
            return new ForInstrumentation(resolveByteBuddyAgentInstrumentation(), classLoader);
        }

        /**
         * Returns a class file locator that is capable of locating a class file for the given type using the given instrumentation instance.
         *
         * @param instrumentation The instrumentation instance to query for a retransformation.
         * @param type            The locatable type which class loader is used as a fallback.
         * @return A class file locator for locating the class file of the given type.
         */
        public static ClassFileLocator of(Instrumentation instrumentation, Class<?> type) {
            return new ForInstrumentation(instrumentation, ClassLoadingDelegate.Explicit.of(type));
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            try {
                ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(classLoadingDelegate.getClassLoader(), name);
                DISPATCHER.addTransformer(instrumentation, classFileTransformer, true);
                try {
                    DISPATCHER.retransformClasses(instrumentation, new Class<?>[]{classLoadingDelegate.locate(name)});
                    byte[] binaryRepresentation = classFileTransformer.getBinaryRepresentation();
                    return binaryRepresentation == null ? new Resolution.Illegal(name) : new Resolution.Explicit(binaryRepresentation);
                } finally {
                    instrumentation.removeTransformer(classFileTransformer);
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception ignored) {
                return new Resolution.Illegal(name);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }

        /**
         * A dispatcher to interact with the {@link Instrumentation} API.
         */
        @JavaDispatcher.Proxied("java.lang.instrument.Instrumentation")
        protected interface Dispatcher {

            /**
             * Invokes the {@code Instrumentation#isRetransformClassesSupported} method.
             *
             * @param instrumentation The instrumentation instance to invoke the method on.
             * @return {@code true} if the supplied instrumentation instance supports retransformation.
             */
            boolean isRetransformClassesSupported(Instrumentation instrumentation);

            /**
             * Registers a transformer.
             *
             * @param instrumentation      The instrumentation instance to invoke the method on.
             * @param classFileTransformer The class file transformer to register.
             * @param canRetransform       {@code true} if the class file transformer should be invoked upon a retransformation.
             */
            void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform);

            /**
             * Retransforms the supplied classes.
             *
             * @param instrumentation The instrumentation instance to invoke the method on.
             * @param type            The types to retransform.
             * @throws UnmodifiableClassException If any of the supplied types are unmodifiable.
             */
            void retransformClasses(Instrumentation instrumentation, Class<?>[] type) throws UnmodifiableClassException;
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
            @MaybeNull
            ClassLoader getClassLoader();

            /**
             * A default implementation of a class loading delegate.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Default implements ClassLoadingDelegate {

                /**
                 * A class loader that does not define resources of its own but allows querying for resources supplied by the boot loader.
                 */
                private static final ClassLoader BOOT_LOADER_PROXY = doPrivileged(BootLoaderProxyCreationAction.INSTANCE);

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
                 * @param classLoader The class loader for which to create a delegate or {@code null} to use the boot loader.
                 * @return The class loading delegate for the provided class loader.
                 */
                public static ClassLoadingDelegate of(@MaybeNull ClassLoader classLoader) {
                    return ForDelegatingClassLoader.isDelegating(classLoader) ? new ForDelegatingClassLoader(classLoader) : new Default(classLoader == null ? BOOT_LOADER_PROXY : classLoader);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    return classLoader.loadClass(name);
                }

                /**
                 * {@inheritDoc}
                 */
                @MaybeNull
                public ClassLoader getClassLoader() {
                    return classLoader == BOOT_LOADER_PROXY ? ClassLoadingStrategy.BOOTSTRAP_LOADER : classLoader;
                }

                /**
                 * A privileged action for creating a proxy class loader for the boot class loader.
                 */
                protected enum BootLoaderProxyCreationAction implements PrivilegedAction<ClassLoader> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public ClassLoader run() {
                        return new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
                    }
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
                private static final Dispatcher.Initializable DISPATCHER = doPrivileged(Dispatcher.CreationAction.INSTANCE);

                /**
                 * Creates a class loading delegate for a delegating class loader.
                 *
                 * @param classLoader The delegating class loader.
                 */
                protected ForDelegatingClassLoader(ClassLoader classLoader) {
                    super(classLoader);
                }

                /**
                 * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
                 *
                 * @param action The action to execute from a privileged context.
                 * @param <T>    The type of the action's resolved value.
                 * @return The action's resolved value.
                 */
                @AccessControllerPlugin.Enhance
                private static <T> T doPrivileged(PrivilegedAction<T> action) {
                    return action.run();
                }

                /**
                 * Checks if a class loader is a delegating class loader.
                 *
                 * @param classLoader The class loader to inspect or {@code null} to check the boot loader.
                 * @return {@code true} if the class loader is a delegating class loader.
                 */
                protected static boolean isDelegating(@MaybeNull ClassLoader classLoader) {
                    return classLoader != null && classLoader.getClass().getName().equals(DELEGATING_CLASS_LOADER_NAME);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Vector<Class<?>> classes;
                    try {
                        classes = DISPATCHER.initialize().extract(classLoader);
                    } catch (RuntimeException ignored) {
                        return super.locate(name);
                    }
                    if (classes.size() != 1) {
                        return super.locate(name);
                    }
                    Class<?> type = classes.get(ONLY);
                    return TypeDescription.ForLoadedType.getName(type).equals(name) ? type : super.locate(name);
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
                     * An uninitialized version of a dispatcher for extracting a class loader's loaded classes.
                     */
                    interface Initializable {

                        /**
                         * Initializes the dispatcher.
                         *
                         * @return An initialized dispatcher.
                         */
                        Dispatcher initialize();
                    }

                    /**
                     * An action for creating a dispatcher.
                     */
                    enum CreationAction implements PrivilegedAction<Initializable> {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * {@inheritDoc}
                         */
                        public Initializable run() {
                            try {
                                return new Dispatcher.Resolved(ClassLoader.class.getDeclaredField("classes"));
                            } catch (Exception exception) {
                                return new Dispatcher.Unresolved(exception.getMessage());
                            }
                        }
                    }

                    /**
                     * Represents a field that could be located.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
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

                        /**
                         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
                         *
                         * @param action The action to execute from a privileged context.
                         * @param <T>    The type of the action's resolved value.
                         * @return The action's resolved value.
                         */
                        @AccessControllerPlugin.Enhance
                        private static <T> T doPrivileged(PrivilegedAction<T> action) {
                            return action.run();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher initialize() {
                            return doPrivileged(this);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        @SuppressWarnings("unchecked")
                        public Vector<Class<?>> extract(ClassLoader classLoader) {
                            try {
                                return (Vector<Class<?>>) field.get(classLoader);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access field", exception);
                            }
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher run() {
                            field.setAccessible(true);
                            return this;
                        }
                    }

                    /**
                     * Represents a field that could not be located.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Unresolved implements Initializable {

                        /**
                         * The reason why this dispatcher is unavailable.
                         */
                        private final String message;

                        /**
                         * Creates a representation of a non-resolved field.
                         *
                         * @param message The reason why this dispatcher is unavailable.
                         */
                        public Unresolved(String message) {
                            this.message = message;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher initialize() {
                            throw new UnsupportedOperationException("Could not locate classes vector: " + message);
                        }
                    }
                }
            }

            /**
             * A class loading delegate that allows the location of explicitly registered classes that cannot
             * be located by a class loader directly. This allows for locating classes that are loaded by
             * an anonymous class loader which does not register its classes in a system dictionary.
             */
            @HashCodeAndEqualsPlugin.Enhance
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
                public Explicit(@MaybeNull ClassLoader classLoader, Collection<? extends Class<?>> types) {
                    this(Default.of(classLoader), types);
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
                        this.types.put(TypeDescription.ForLoadedType.getName(type), type);
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

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Class<?> type = types.get(name);
                    return type == null ? fallbackDelegate.locate(name) : type;
                }

                /**
                 * {@inheritDoc}
                 */
                @MaybeNull
                public ClassLoader getClassLoader() {
                    return fallbackDelegate.getClassLoader();
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
            @AlwaysNull
            private static final byte[] DO_NOT_TRANSFORM = null;

            /**
             * The class loader that is expected to have loaded the looked-up a class.
             */
            @MaybeNull
            private final ClassLoader classLoader;

            /**
             * The name of the type to look up.
             */
            private final String typeName;

            /**
             * The binary representation of the looked-up class.
             */
            @MaybeNull
            @SuppressFBWarnings(value = "VO_VOLATILE_REFERENCE_TO_ARRAY", justification = "The array is not to be modified by contract")
            private volatile byte[] binaryRepresentation;

            /**
             * Creates a class file transformer for the purpose of extraction.
             *
             * @param classLoader The class loader that is expected to have loaded the looked-up a class.
             * @param typeName    The name of the type to look up.
             */
            protected ExtractionClassFileTransformer(@MaybeNull ClassLoader classLoader, String typeName) {
                this.classLoader = classLoader;
                this.typeName = typeName;
            }

            /**
             * {@inheritDoc}
             */
            @MaybeNull
            @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "The array is not modified by class contract.")
            public byte[] transform(@MaybeNull ClassLoader classLoader, @MaybeNull String internalName, @MaybeNull Class<?> redefinedType, ProtectionDomain protectionDomain, byte[] binaryRepresentation) {
                if (internalName != null && isChildOf(this.classLoader).matches(classLoader) && typeName.equals(internalName.replace('/', '.'))) {
                    this.binaryRepresentation = binaryRepresentation.clone();
                }
                return DO_NOT_TRANSFORM;
            }

            /**
             * Returns the binary representation of the class file that was looked up. The returned array must never be modified.
             *
             * @return The binary representation of the class file or {@code null} if no such class file could
             * be located.
             */
            @MaybeNull
            @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The array is not to be modified by contract")
            protected byte[] getBinaryRepresentation() {
                return binaryRepresentation;
            }
        }
    }

    /**
     * A class file locator that discriminates by a type's package.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class PackageDiscriminating implements ClassFileLocator {

        /**
         * A mapping of package names to class file locators.
         */
        private final Map<String, ClassFileLocator> classFileLocators;

        /**
         * Creates a new package-discriminating class file locator.
         *
         * @param classFileLocators A mapping of package names to class file locators where an empty string donates the default package.
         */
        public PackageDiscriminating(Map<String, ClassFileLocator> classFileLocators) {
            this.classFileLocators = classFileLocators;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            int packageIndex = name.lastIndexOf('.');
            ClassFileLocator classFileLocator = classFileLocators.get(packageIndex == -1 ? NamedElement.EMPTY_NAME : name.substring(0, packageIndex));
            return classFileLocator == null ? new Resolution.Illegal(name) : classFileLocator.locate(name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            for (ClassFileLocator classFileLocator : classFileLocators.values()) {
                classFileLocator.close();
            }
        }
    }

    /**
     * A class file locator that only applies for matched names.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Filtering implements ClassFileLocator {

        /**
         * The matcher to determine if the delegate matcher is considered.
         */
        private final ElementMatcher<? super String> matcher;

        /**
         * The delegate class file locator.
         */
        private final ClassFileLocator delegate;

        /**
         * Creates a new filtering class file locator.
         *
         * @param matcher  The matcher to determine if the delegate matcher is considered.
         * @param delegate The delegate class file locator.
         */
        public Filtering(ElementMatcher<? super String> matcher, ClassFileLocator delegate) {
            this.matcher = matcher;
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            return matcher.matches(name) ? delegate.locate(name) : new Resolution.Illegal(name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            delegate.close();
        }
    }

    /**
     * A compound {@link ClassFileLocator} that chains several locators.
     * Any class file locator is queried in the supplied order until one locator is able to provide an input
     * stream of the class file.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements ClassFileLocator, Closeable {

        /**
         * The {@link ClassFileLocator}s which are represented by this compound
         * class file locator  in the order of their application.
         */
        private final List<ClassFileLocator> classFileLocators;

        /**
         * Creates a new compound class file locator.
         *
         * @param classFileLocator The {@link ClassFileLocator}s to be
         *                         represented by this compound class file locator in the order of their application.
         */
        public Compound(ClassFileLocator... classFileLocator) {
            this(Arrays.asList(classFileLocator));
        }

        /**
         * Creates a new compound class file locator.
         *
         * @param classFileLocators The {@link ClassFileLocator}s to be represented by this compound class file locator in
         *                          the order of their application.
         */
        public Compound(List<? extends ClassFileLocator> classFileLocators) {
            this.classFileLocators = new ArrayList<ClassFileLocator>();
            for (ClassFileLocator classFileLocator : classFileLocators) {
                if (classFileLocator instanceof Compound) {
                    this.classFileLocators.addAll(((Compound) classFileLocator).classFileLocators);
                } else if (!(classFileLocator instanceof NoOp)) {
                    this.classFileLocators.add(classFileLocator);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) throws IOException {
            for (ClassFileLocator classFileLocator : classFileLocators) {
                Resolution resolution = classFileLocator.locate(name);
                if (resolution.isResolved()) {
                    return resolution;
                }
            }
            return new Resolution.Illegal(name);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            for (ClassFileLocator classFileLocator : classFileLocators) {
                classFileLocator.close();
            }
        }
    }
}
