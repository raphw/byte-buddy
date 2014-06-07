package net.bytebuddy;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.modifier.SyntheticState;
import net.bytebuddy.modifier.TypeManifestation;
import net.bytebuddy.modifier.TypeVisibility;

import java.util.Collection;
import java.util.Random;

/**
 * A naming strategy for finding a fully qualified name for a Java type.
 * <p>&nbsp;</p>
 * Note that subclasses that lie within the same package as their superclass have improved access to overriding
 * package-private methods of their super type.
 */
public interface NamingStrategy {

    /**
     * Generates a fully qualified name for a Java type. The resulting should not lie within the {@code java.lang}
     * package since such types cannot be loaded using a normal class loader. Also, the name should not yet be taken
     * by another type since this would cause conflicts in the name space. Therefore, it is recommendable to include
     * a random sequence within the name.
     *
     * @param unnamedType An unnamed type that is to be named.
     * @return A valid identifier for a Java type.
     */
    String name(UnnamedType unnamedType);

    /**
     * An description of a type which is to be named.
     */
    static interface UnnamedType {

        /**
         * Returns this unnamed type's super class.
         *
         * @return A description of the super class of the type to be named.
         */
        TypeDescription getSuperClass();

        /**
         * Returns a collection of descriptions of this unnamed type's directly implemented interfaces.
         *
         * @return A collection of implemented interfaces.
         */
        Collection<TypeDescription> getDeclaredInterfaces();

        /**
         * Returns the visibility of this unnamed type.
         *
         * @return The visibility of this unnamed type.
         */
        TypeVisibility getVisibility();

        /**
         * Returns the manifestation of this unnamed type.
         *
         * @return The manifestation of this unnamed type.
         */
        TypeManifestation getTypeManifestation();

        /**
         * Returns the manifestation of this unnamed type.
         *
         * @return The manifestation of this unnamed type.
         */
        SyntheticState getSyntheticState();

        /**
         * Returns the class file version of this unnamed type.
         *
         * @return The class file version of this unnamed type.
         */
        ClassFileVersion getClassFileVersion();
    }

    /**
     * A naming strategy that creates a name by concatenating:
     * <ol>
     * <li>The super classes package and name</li>
     * <li>A given suffix string</li>
     * <li>A random number</li>
     * </ol>
     * Between all these elements, a {@code $} sign is included into the name to improve readability. As an exception,
     * types that subclass classes from the {@code java.**} packages are prefixed with a given package. This is
     * necessary as it is illegal to define non-bootstrap classes in this name space. The same strategy is applied
     * when subclassing a signed type which is equally illegal.
     */
    static class SuffixingRandom implements NamingStrategy {

        /**
         * The package prefix of the {@code java.**} packages for which the definition of non-bootstrap types is
         * illegal.
         */
        private static final String JAVA_PACKAGE = "java.";

        /**
         * The default package for defining types that are renamed to not be contained in the
         * {@link net.bytebuddy.NamingStrategy.SuffixingRandom#JAVA_PACKAGE} package.
         */
        private static final String BYTE_BUDDY_RENAME_PACKAGE = "net.bytebuddy.renamed";

        /**
         * The suffix to attach to a super type name.
         */
        private final String suffix;

        /**
         * The renaming location for types of the {@link net.bytebuddy.NamingStrategy.SuffixingRandom#JAVA_PACKAGE}.
         */
        private final String javaLangPackagePrefix;

        /**
         * An instance for creating random values.
         */
        private final Random random;

        /**
         * Creates an immutable naming strategy with a given suffix but moves types that subclass types within
         * the {@code java.lang} package into ByteBuddy's package namespace.
         *
         * @param suffix The suffix for the generated class.
         */
        public SuffixingRandom(String suffix) {
            this(suffix, BYTE_BUDDY_RENAME_PACKAGE);
        }

        /**
         * Creates an immutable naming strategy with a given suffix but moves types that subclass types within
         * the {@code java.lang} package into a given namespace.
         *
         * @param suffix                The suffix for the generated class.
         * @param javaLangPackagePrefix The fallback namespace for type's that subclass types within the
         *                              {@code java.lang} namespace.
         */
        public SuffixingRandom(String suffix, String javaLangPackagePrefix) {
            this.suffix = suffix;
            this.javaLangPackagePrefix = javaLangPackagePrefix;
            this.random = new Random();
        }

        @Override
        public String name(UnnamedType unnamedType) {
            String superClassName = unnamedType.getSuperClass().getName();
            if (superClassName.startsWith(JAVA_PACKAGE) || unnamedType.getSuperClass().isSealed()) {
                superClassName = javaLangPackagePrefix + "." + superClassName;
            }
            return String.format("%s$%s$%d", superClassName, suffix, Math.abs(random.nextInt()));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SuffixingRandom that = (SuffixingRandom) other;
            return javaLangPackagePrefix.equals(that.javaLangPackagePrefix)
                    && suffix.equals(that.suffix);
        }

        @Override
        public int hashCode() {
            int result = suffix.hashCode();
            result = 31 * result + javaLangPackagePrefix.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "NamingStrategy.SuffixingRandom{" +
                    "suffix='" + suffix + '\'' +
                    ", javaLangPackagePrefix='" + javaLangPackagePrefix + '\'' +
                    ", random=" + random +
                    '}';
        }
    }

    /**
     * A naming strategy that creates a name by prefixing a given class and its package with another package and
     * by appending a random number to the class's simple name.
     */
    static class PrefixingRandom implements NamingStrategy {

        /**
         * The package to prefix.
         */
        private final String prefix;

        /**
         * A random number generator.
         */
        private final Random random;

        /**
         * Creates a new prefixing random naming strategy.
         *
         * @param prefix The prefix to append.
         */
        public PrefixingRandom(String prefix) {
            this.prefix = prefix;
            random = new Random();
        }

        @Override
        public String name(UnnamedType unnamedType) {
            return String.format("%s.%s$%d", prefix, unnamedType.getSuperClass().getName(), Math.abs(random.nextInt()));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && prefix.equals(((PrefixingRandom) other).prefix);
        }

        @Override
        public int hashCode() {
            return prefix.hashCode();
        }

        @Override
        public String toString() {
            return "NamingStrategy.PrefixingRandom{" +
                    "prefix='" + prefix + '\'' +
                    ", random=" + random +
                    '}';
        }
    }

    /**
     * A naming strategy that applies a fixed name.
     * <p>&nbsp;</p>
     * This strategy should only be used for one shot type generators since they will otherwise create types that
     * impose naming conflicts.
     */
    static class Fixed implements NamingStrategy {

        /**
         * The fixed type name.
         */
        private final String name;

        /**
         * Creates an immutable fixed naming strategy.
         *
         * @param name The name for the created type.
         */
        public Fixed(String name) {
            this.name = name;
        }

        @Override
        public String name(UnnamedType unnamedType) {
            return name;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && name.equals(((Fixed) other).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "NamingStrategy.Fixed{name='" + name + '\'' + '}';
        }
    }
}
