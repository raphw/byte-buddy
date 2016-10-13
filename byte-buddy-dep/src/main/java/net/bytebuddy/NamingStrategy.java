package net.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.RandomString;

/**
 * <p>
 * A naming strategy for determining a fully qualified name for a dynamically created Java type.
 * </p>
 * <p>
 * Note that subclasses that lie within the same package as their superclass can access package-private methods
 * of super types within the same package.
 * </p>
 */
public interface NamingStrategy {

    /**
     * Determines a new name when creating a new type that subclasses the provided type.
     *
     * @param superClass The super type of the created type.
     * @return The name of the dynamic type.
     */
    String subclass(TypeDescription.Generic superClass);

    /**
     * Determines a name for the dynamic type when redefining the provided type.
     *
     * @param typeDescription The type being redefined.
     * @return The name of the dynamic type.
     */
    String redefine(TypeDescription typeDescription);

    /**
     * Determines a name for the dynamic type when rebasing the provided type.
     *
     * @param typeDescription The type being redefined.
     * @return The name of the dynamic type.
     */
    String rebase(TypeDescription typeDescription);

    /**
     * An abstract base implementation where the names of redefined and rebased types are retained.
     */
    abstract class AbstractBase implements NamingStrategy {

        @Override
        public String subclass(TypeDescription.Generic superClass) {
            return name(superClass.asErasure());
        }

        /**
         * Determines a new name when creating a new type that subclasses the provided type.
         *
         * @param superClass The super type of the created type.
         * @return The name of the dynamic type.
         */
        protected abstract String name(TypeDescription superClass);

        @Override
        public String redefine(TypeDescription typeDescription) {
            return typeDescription.getName();
        }

        @Override
        public String rebase(TypeDescription typeDescription) {
            return typeDescription.getName();
        }
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
    class SuffixingRandom extends AbstractBase {

        /**
         * The default package for defining types that are renamed to not be contained in the
         * {@link net.bytebuddy.NamingStrategy.SuffixingRandom#JAVA_PACKAGE} package.
         */
        public static final String BYTE_BUDDY_RENAME_PACKAGE = "net.bytebuddy.renamed";

        /**
         * Indicates that types of the {@code java.*} package should not be prefixed.
         */
        public static final String NO_PREFIX = "";

        /**
         * The package prefix of the {@code java.*} packages for which the definition of
         * non-bootstrap types is illegal.
         */
        private static final String JAVA_PACKAGE = "java.";

        /**
         * The suffix to attach to a super type name.
         */
        private final String suffix;

        /**
         * The renaming location for types of the {@link net.bytebuddy.NamingStrategy.SuffixingRandom#JAVA_PACKAGE}.
         */
        private final String javaLangPackagePrefix;

        /**
         * An instance for creating random seed values.
         */
        private final RandomString randomString;

        /**
         * A resolver for the base name for naming the unnamed type.
         */
        private final BaseNameResolver baseNameResolver;

        /**
         * Creates an immutable naming strategy with a given suffix but moves types that subclass types within
         * the {@code java.lang} package into Byte Buddy's package namespace. All names are derived from the
         * unnamed type's super type.
         *
         * @param suffix The suffix for the generated class.
         */
        public SuffixingRandom(String suffix) {
            this(suffix, BaseNameResolver.ForUnnamedType.INSTANCE);
        }

        /**
         * Creates an immutable naming strategy with a given suffix but moves types that subclass types within
         * the {@code java.lang} package into Byte Buddy's package namespace.
         *
         * @param suffix                The suffix for the generated class.
         * @param javaLangPackagePrefix The fallback namespace for type's that subclass types within the
         *                              {@code java.*} namespace. If The prefix is set to the empty string,
         *                              no prefix is added.
         */
        public SuffixingRandom(String suffix, String javaLangPackagePrefix) {
            this(suffix, BaseNameResolver.ForUnnamedType.INSTANCE, javaLangPackagePrefix);
        }

        /**
         * Creates an immutable naming strategy with a given suffix but moves types that subclass types within
         * the {@code java.lang} package into Byte Buddy's package namespace.
         *
         * @param suffix           The suffix for the generated class.
         * @param baseNameResolver The base name resolver that is queried for locating the base name.
         */
        public SuffixingRandom(String suffix, BaseNameResolver baseNameResolver) {
            this(suffix, baseNameResolver, BYTE_BUDDY_RENAME_PACKAGE);
        }

        /**
         * Creates an immutable naming strategy with a given suffix but moves types that subclass types within
         * the {@code java.lang} package into a given namespace.
         *
         * @param suffix                The suffix for the generated class.
         * @param baseNameResolver      The base name resolver that is queried for locating the base name.
         * @param javaLangPackagePrefix The fallback namespace for type's that subclass types within the
         *                              {@code java.*} namespace. If The prefix is set to the empty string,
         *                              no prefix is added.
         */
        public SuffixingRandom(String suffix, BaseNameResolver baseNameResolver, String javaLangPackagePrefix) {
            this.suffix = suffix;
            this.baseNameResolver = baseNameResolver;
            this.javaLangPackagePrefix = javaLangPackagePrefix;
            randomString = new RandomString();
        }

        @Override
        protected String name(TypeDescription superClass) {
            String baseName = baseNameResolver.resolve(superClass);
            if (baseName.startsWith(JAVA_PACKAGE) && !javaLangPackagePrefix.equals("")) {
                baseName = javaLangPackagePrefix + "." + baseName;
            }
            return String.format("%s$%s$%s", baseName, suffix, randomString.nextString());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SuffixingRandom that = (SuffixingRandom) other;
            return javaLangPackagePrefix.equals(that.javaLangPackagePrefix)
                    && suffix.equals(that.suffix)
                    && baseNameResolver.equals(that.baseNameResolver);
        }

        @Override
        public int hashCode() {
            int result = suffix.hashCode();
            result = 31 * result + javaLangPackagePrefix.hashCode();
            result = 31 * result + baseNameResolver.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "NamingStrategy.SuffixingRandom{" +
                    "suffix='" + suffix + '\'' +
                    ", javaLangPackagePrefix='" + javaLangPackagePrefix + '\'' +
                    ", baseNameResolver=" + baseNameResolver +
                    ", randomString=" + randomString +
                    '}';
        }

        /**
         * A base name resolver is responsible for resolving a name onto which the suffix is appended.
         */
        public interface BaseNameResolver {

            /**
             * Resolves the base name for a given type description.
             *
             * @param typeDescription The type for which the base name is resolved.
             * @return The base name for the given type.
             */
            String resolve(TypeDescription typeDescription);

            /**
             * Uses the unnamed type's super type's name as the resolved name.
             */
            enum ForUnnamedType implements BaseNameResolver {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public String resolve(TypeDescription typeDescription) {
                    return typeDescription.getName();
                }

                @Override
                public String toString() {
                    return "NamingStrategy.SuffixingRandom.BaseNameResolver.ForUnnamedType." + name();
                }
            }

            /**
             * Uses a specific type's name as the resolved name.
             */
            class ForGivenType implements BaseNameResolver {

                /**
                 * The type description which represents the resolved name.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new base name resolver that resolves a using the name of a given type.
                 *
                 * @param typeDescription The type description which represents the resolved name.
                 */
                public ForGivenType(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public String resolve(TypeDescription typeDescription) {
                    return this.typeDescription.getName();
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForGivenType) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "NamingStrategy.SuffixingRandom.BaseNameResolver.ForGivenType{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }

            /**
             * A base name resolver that simply returns a fixed value.
             */
            class ForFixedValue implements BaseNameResolver {

                /**
                 * The fixed base name.
                 */
                private final String name;

                /**
                 * Creates a new base name resolver for a fixed name.
                 *
                 * @param name The fixed name
                 */
                public ForFixedValue(String name) {
                    this.name = name;
                }

                @Override
                public String resolve(TypeDescription typeDescription) {
                    return name;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && name.equals(((ForFixedValue) other).name);
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }

                @Override
                public String toString() {
                    return "NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue{" +
                            "name='" + name + '\'' +
                            '}';
                }
            }
        }
    }

    /**
     * A naming strategy that creates a name by prefixing a given class and its package with another package and
     * by appending a random number to the class's simple name.
     */
    class PrefixingRandom extends AbstractBase {

        /**
         * The package to prefix.
         */
        private final String prefix;

        /**
         * A seed generator.
         */
        private final RandomString randomString;

        /**
         * Creates a new prefixing random naming strategy.
         *
         * @param prefix The prefix to append.
         */
        public PrefixingRandom(String prefix) {
            this.prefix = prefix;
            randomString = new RandomString();
        }

        @Override
        protected String name(TypeDescription superClass) {
            return String.format("%s.%s$%s", prefix, superClass.getName(), randomString.nextString());
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
                    ", randomString=" + randomString +
                    '}';
        }
    }
}
