package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * <p>
 * This {@link java.lang.ClassLoader} is capable of loading classes from multiple parents. This class loader
 * implicitly defines the bootstrap class loader to be its direct parent as it is required for all class loaders.
 * This can be useful when creating a type that inherits a super type and interfaces that are defined by different,
 * non-compatible class loaders.
 * </p>
 * <p>
 * <b>Note</b>: Instances of this class loader can have the same class loader as its parent multiple times,
 * either directly or indirectly by multiple parents sharing a common parent class loader. By definition,
 * this implies that the bootstrap class loader is {@code #(direct parents) + 1} times a parent of this class loader.
 * For the {@link java.lang.ClassLoader#getResources(java.lang.String)} method, this means that this class loader
 * might return the same url multiple times by representing the same class loader multiple times.
 * </p>
 * <p>
 * <b>Important</b>: This class loader does not support the location of packages from its multiple parents. This breaks
 * package equality when loading classes by either loading them directly via this class loader (e.g. by subclassing) or
 * by loading classes with child class loaders of this class loader.
 * </p>
 */
public class MultipleParentClassLoader extends ClassLoader {

    /**
     * A dispatcher for accessing the {@link ClassLoader#loadClass(String, boolean)} method.
     */
    private static final Dispatcher DISPATCHER = Dispatcher.Active.make();

    /**
     * The parents of this class loader in their application order.
     */
    private final List<? extends ClassLoader> parents;

    /**
     * Creates a new class loader with multiple parents.
     *
     * @param parents The parents of this class loader in their application order. This list must not contain {@code null},
     *                i.e. the bootstrap class loader which is an implicit parent of any class loader.
     */
    public MultipleParentClassLoader(List<? extends ClassLoader> parents) {
        super(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        this.parents = parents;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (ClassLoader parent : parents) {
            try {
                return DISPATCHER.loadClass(parent, name, resolve);
            } catch (ClassNotFoundException ignored) {
                /* try next class loader */
            }
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader parent : parents) {
            URL url = parent.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<Enumeration<URL>> enumerations = new ArrayList<Enumeration<URL>>(parents.size() + 1);
        for (ClassLoader parent : parents) {
            enumerations.add(parent.getResources(name));
        }
        enumerations.add(super.getResources(name));
        return new CompoundEnumeration(enumerations);
    }

    @Override
    public String toString() {
        return "MultipleParentClassLoader{" +
                "parents=" + parents +
                '}';
    }

    /**
     * A dispatcher for locating a class from a parent class loader.
     */
    protected interface Dispatcher {

        /**
         * Locates a class.
         *
         * @param classLoader The class loader to access.
         * @param name        The name of the class.
         * @param resolve     {@code true} if the class should be resolved.
         * @return The loaded class.
         * @throws ClassNotFoundException If the class could not be found.
         */
        Class<?> loadClass(ClassLoader classLoader, String name, boolean resolve) throws ClassNotFoundException;

        /**
         * An active dispatcher for the {@link ClassLoader#loadClass(String, boolean)} method.
         */
        class Active implements Dispatcher, PrivilegedAction<Dispatcher> {

            /**
             * The {@link ClassLoader#loadClass(String, boolean)} method.
             */
            private final Method loadClass;

            /**
             * Creates a new active dispatcher.
             *
             * @param loadClass The {@link ClassLoader#loadClass(String, boolean)} method.
             */
            protected Active(Method loadClass) {
                this.loadClass = loadClass;
            }

            /**
             * Creates a new dispatcher.
             *
             * @return A dispatcher for invoking the {@link ClassLoader#loadClass(String, boolean)} method.
             */
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
            protected static Dispatcher make() {
                try {
                    return AccessController.doPrivileged(new Active(ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class)));
                } catch (Exception exception) {
                    return new Erroneous(exception);
                }
            }

            @Override
            public Dispatcher run() {
                loadClass.setAccessible(true);
                return this;
            }

            @Override
            public Class<?> loadClass(ClassLoader classLoader, String name, boolean resolve) throws ClassNotFoundException {
                try {
                    return (Class<?>) loadClass.invoke(classLoader, name, resolve);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + loadClass, exception);
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof ClassNotFoundException) {
                        throw (ClassNotFoundException) cause;
                    }
                    throw new IllegalStateException("Cannot execute " + loadClass, cause);
                }
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Active active = (Active) object;
                return loadClass.equals(active.loadClass);
            }

            @Override
            public int hashCode() {
                return loadClass.hashCode();
            }

            @Override
            public String toString() {
                return "MultipleParentClassLoader.Dispatcher.Active{" +
                        "loadClass=" + loadClass +
                        '}';
            }
        }

        /**
         * A dispatcher when the {@link ClassLoader#loadClass(String, boolean)} method cannot be accessed.
         */
        class Erroneous implements Dispatcher {

            /**
             * The exception that occurred when attempting to create a dispatcher.
             */
            private final Exception exception;

            /**
             * Creates a new erroneous dispatcher.
             *
             * @param exception The exception that occurred when attempting to create a dispatcher.
             */
            protected Erroneous(Exception exception) {
                this.exception = exception;
            }

            @Override
            public Class<?> loadClass(ClassLoader classLoader, String name, boolean resolve) {
                throw new IllegalStateException("Cannot access parent class", exception);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                Erroneous erroneous = (Erroneous) object;
                return exception.equals(erroneous.exception);
            }

            @Override
            public int hashCode() {
                return exception.hashCode();
            }

            @Override
            public String toString() {
                return "MultipleParentClassLoader.Dispatcher.Erroneous{" +
                        "exception=" + exception +
                        '}';
            }
        }
    }

    /**
     * A compound URL enumeration.
     */
    protected static class CompoundEnumeration implements Enumeration<URL> {

        /**
         * Indicates the first index of a list.
         */
        private static final int FIRST = 0;

        /**
         * The remaining lists of enumerations.
         */
        private final List<Enumeration<URL>> enumerations;

        /**
         * The currently represented enumeration or {@code null} if no such enumeration is currently selected.
         */
        private Enumeration<URL> currentEnumeration;

        /**
         * Creates a compound enumeration.
         *
         * @param enumerations The enumerations to represent.
         */
        protected CompoundEnumeration(List<Enumeration<URL>> enumerations) {
            this.enumerations = enumerations;
        }

        @Override
        public boolean hasMoreElements() {
            if (currentEnumeration != null && currentEnumeration.hasMoreElements()) {
                return true;
            } else if (!enumerations.isEmpty()) {
                currentEnumeration = enumerations.remove(FIRST);
                return hasMoreElements();
            } else {
                return false;
            }
        }

        @Override
        @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Null reference is impossible due to element check")
        public URL nextElement() {
            if (hasMoreElements()) {
                return currentEnumeration.nextElement();
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public String toString() {
            return "MultipleParentClassLoader.CompoundEnumeration{" +
                    "enumerations=" + enumerations +
                    ", currentEnumeration=" + currentEnumeration +
                    '}';
        }
    }

    /**
     * A builder to collect class loader and that creates a
     * {@link net.bytebuddy.dynamic.loading.MultipleParentClassLoader} only if multiple or no
     * {@link java.lang.ClassLoader}s are found in the process. If exactly a single class loader is found,
     * this class loader is returned. All class loaders are applied in their collection order with the exception
     * of the bootstrap class loader which is represented by {@code null} and which is an implicit parent of any
     * class loader.
     */
    public static class Builder {

        /**
         * Indicates the first index of a list.
         */
        private static final int ONLY = 0;

        /**
         * The class loaders that were collected.
         */
        private final List<? extends ClassLoader> classLoaders;

        /**
         * Creates a new builder without any class loaders.
         */
        public Builder() {
            this(Collections.<ClassLoader>emptyList());
        }

        /**
         * Creates a new builder.
         *
         * @param classLoaders The class loaders that were collected until now.
         */
        private Builder(List<? extends ClassLoader> classLoaders) {
            this.classLoaders = classLoaders;
        }

        /**
         * Appends the class loaders of the given types. The bootstrap class loader is implicitly skipped as
         * it is an implicit parent of any class loader.
         *
         * @param type The types of which to collect the class loaders.
         * @return A new builder instance with the additional class loaders of the provided types if they were not
         * yet collected.
         */
        public Builder append(Class<?>... type) {
            return append(Arrays.asList(type));
        }

        /**
         * Appends the class loaders of the given types if those class loaders were not yet collected. The bootstrap class
         * loader is implicitly skipped as it is an implicit parent of any class loader.
         *
         * @param types The types of which to collect the class loaders.
         * @return A new builder instance with the additional class loaders.
         */
        public Builder append(Collection<? extends Class<?>> types) {
            List<ClassLoader> classLoaders = new ArrayList<ClassLoader>(types.size());
            for (Class<?> type : types) {
                classLoaders.add(type.getClassLoader());
            }
            return append(classLoaders);
        }

        /**
         * Appends the given class loaders if they were not yet collected. The bootstrap class loader is implicitly
         * skipped as it is an implicit parent of any class loader.
         *
         * @param classLoader The class loaders to be collected.
         * @return A new builder instance with the additional class loaders.
         */
        public Builder append(ClassLoader... classLoader) {
            return append(Arrays.asList(classLoader));
        }

        /**
         * Appends the given class loaders if they were not yet appended. The bootstrap class loader is never appended as
         * it is an implicit parent of any class loader.
         *
         * @param classLoaders The class loaders to collected.
         * @return A new builder instance with the additional class loaders.
         */
        public Builder append(List<? extends ClassLoader> classLoaders) {
            List<ClassLoader> filtered = new ArrayList<ClassLoader>(this.classLoaders.size() + classLoaders.size());
            Set<ClassLoader> registered = new HashSet<ClassLoader>(this.classLoaders);
            filtered.addAll(this.classLoaders);
            for (ClassLoader classLoader : classLoaders) {
                if (classLoader != null && registered.add(classLoader)) {
                    filtered.add(classLoader);
                }
            }
            return new Builder(filtered);
        }

        /**
         * Only retains all class loaders that match the given matcher.
         *
         * @param matcher The matcher to be used for filtering.
         * @return A builder that does not longer consider any appended class loaders that matched the provided matcher.
         */
        public Builder filter(ElementMatcher<? super ClassLoader> matcher) {
            List<ClassLoader> classLoaders = new ArrayList<ClassLoader>(this.classLoaders.size());
            for (ClassLoader classLoader : this.classLoaders) {
                if (matcher.matches(classLoader)) {
                    classLoaders.add(classLoader);
                }
            }
            return new Builder(classLoaders);
        }

        /**
         * <p>
         * Returns the only class loader that was appended if exactly one class loader was appended or a multiple parent class loader as
         * a parent of all supplied class loader and with the bootstrap class loader as an implicit parent. If no class loader
         * </p>
         * <p>
         * <b>Important</b>: Byte Buddy does not provide any access control for the creation of the class loader. It is the responsibility
         * of the user of this builder to provide such privileges.
         * </p>
         *
         * @return A suitable class loader.
         */
        public ClassLoader build() {
            return classLoaders.size() == 1
                    ? classLoaders.get(ONLY)
                    : new MultipleParentClassLoader(classLoaders);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Builder builder = (Builder) other;
            return classLoaders.equals(builder.classLoaders);
        }

        @Override
        public int hashCode() {
            return classLoaders.hashCode();
        }

        @Override
        public String toString() {
            return "MultipleParentClassLoader.Builder{" +
                    "classLoaders=" + classLoaders +
                    '}';
        }
    }
}
