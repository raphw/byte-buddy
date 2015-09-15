package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.IOException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import static net.bytebuddy.utility.ByteBuddyCommons.filterUnique;

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
     * The parents of this class loader in their application order.
     */
    private final List<? extends ClassLoader> parents;

    /**
     * Creates a new class loader with multiple parents.
     *
     * @param parents The parents of this class loader in their application order.
     */
    public MultipleParentClassLoader(List<? extends ClassLoader> parents) {
        super(null);
        this.parents = parents;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader parent : parents) {
            try {
                return parent != null
                        ? parent.loadClass(name)
                        : super.loadClass(name);
            } catch (ClassNotFoundException ignored) {
                /* try next class loader */
            }
        }
        return super.loadClass(name);
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader parent : parents) {
            URL url = parent != null
                    ? parent.getResource(name)
                    : super.getResource(name);
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
            enumerations.add(parent != null
                    ? parent.getResources(name)
                    : super.getResources(name));
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
     * this class loader is returned. All class loaders are applied in their collection order.
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
         * Appends the class loaders of the given types.
         *
         * @param type The types of which to collect the class loaders.
         * @return A new builder instance with the additional class loaders of the provided types if they were not
         * yet collected.
         */
        public Builder append(Class<?>... type) {
            return append(Arrays.asList(type));
        }

        /**
         * Appends the class loaders of the given types if those class loaders were not yet collected.
         *
         * @param types The types of which to collect the class loaders.
         * @return A new builder instance with the additional class loaders.
         */
        public Builder append(Collection<Class<?>> types) {
            List<ClassLoader> classLoaders = new ArrayList<ClassLoader>(types.size());
            for (Class<?> type : types) {
                classLoaders.add(type.getClassLoader());
            }
            return append(classLoaders);
        }

        /**
         * Appends the given class loaders if they were not yet collected.
         *
         * @param classLoader The class loaders to be collected.
         * @return A new builder instance with the additional class loaders.
         */
        public Builder append(ClassLoader... classLoader) {
            return append(Arrays.asList(classLoader));
        }

        /**
         * Appends the given class loaders if they were not yet collected.
         *
         * @param classLoaders The class loaders to collected.
         * @return A new builder instance with the additional class loaders.
         */
        public Builder append(List<? extends ClassLoader> classLoaders) {
            return new Builder(filterUnique(this.classLoaders, classLoaders));
        }

        /**
         * Removes all class loaders that match the given filter.
         *
         * @param matcher The matcher to be used for filtering.
         * @return A builder that does not longer consider any appended class loaders that matched the provided matcher.
         */
        public Builder filter(ElementMatcher<? super ClassLoader> matcher) {
            List<ClassLoader> classLoaders = new ArrayList<ClassLoader>(this.classLoaders.size());
            for (ClassLoader classLoader : this.classLoaders) {
                if (!matcher.matches(classLoader)) {
                    classLoaders.add(classLoader);
                }
            }
            return new Builder(classLoaders);
        }

        /**
         * Returns an appropriate class loader that represents all the collected class loaders using the current access control context.
         *
         * @return A suitable class loader.
         */
        public ClassLoader build() {
            return build(AccessController.getContext());
        }

        /**
         * Returns an appropriate class loader that represents all the collected class loaders.
         *
         * @param accessControlContext The access control context to be used for creating the class loader.
         * @return A suitable class loader.
         */
        public ClassLoader build(AccessControlContext accessControlContext) {
            return classLoaders.size() == 1
                    ? classLoaders.get(ONLY)
                    : AccessController.doPrivileged(new ClassLoaderCreationAction(classLoaders), accessControlContext);
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

        /**
         * A privileged action for creating a multiple-parent class loader.
         */
        protected static class ClassLoaderCreationAction implements PrivilegedAction<ClassLoader> {

            /**
             * The class loaders to combine.
             */
            private final List<? extends ClassLoader> classLoaders;

            /**
             * Creates a new action for creating a multiple-parent class loader.
             *
             * @param classLoaders The class loaders to combine.
             */
            protected ClassLoaderCreationAction(List<? extends ClassLoader> classLoaders) {
                this.classLoaders = classLoaders;
            }

            @Override
            public ClassLoader run() {
                return new MultipleParentClassLoader(classLoaders);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && classLoaders.equals(((ClassLoaderCreationAction) other).classLoaders);
            }

            @Override
            public int hashCode() {
                return classLoaders.hashCode();
            }

            @Override
            public String toString() {
                return "MultipleParentClassLoader.Builder.ClassLoaderCreationAction{" +
                        "classLoaders=" + classLoaders +
                        '}';
            }
        }
    }
}
