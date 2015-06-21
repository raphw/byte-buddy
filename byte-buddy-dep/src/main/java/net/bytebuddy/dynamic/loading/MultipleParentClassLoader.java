package net.bytebuddy.dynamic.loading;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static net.bytebuddy.utility.ByteBuddyCommons.joinUnique;

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
     * this class loader is returned.
     */
    public static class Builder {

        /**
         * The class loaders that were collected.
         */
        private final List<ClassLoader> classLoaders;

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
        private Builder(List<ClassLoader> classLoaders) {
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
         * @param classLoader The class loaders to collected.
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
            return new Builder(joinUnique(this.classLoaders, classLoaders));
        }

        /**
         * Returns an appropriate class loader that represents all the collected class loaders.
         *
         * @return A suitable class loader.
         */
        public ClassLoader build() {
            return classLoaders.size() == 1
                    ? classLoaders.get(0)
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
