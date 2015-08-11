package net.bytebuddy;

import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.utility.ByteBuddyCommons;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    interface UnnamedType {

        /**
         * Returns this unnamed type's super class.
         *
         * @return A description of the super class of the type to be named.
         */
        GenericTypeDescription getSuperClass();

        /**
         * Returns a collection of descriptions of this unnamed type's directly implemented interfaces.
         *
         * @return A collection of implemented interfaces.
         */
        Collection<GenericTypeDescription> getDeclaredInterfaces();

        /**
         * Returns the visibility of this unnamed type.
         *
         * @return The visibility of this unnamed type.
         */
        Visibility getVisibility();

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
         * Returns the enumeration state of this unnamed type.
         *
         * @return The enumeration state of this unnamed type.
         */
        EnumerationState getEnumerationState();

        /**
         * Returns the class file version of this unnamed type.
         *
         * @return The class file version of this unnamed type.
         */
        ClassFileVersion getClassFileVersion();

        /**
         * An unnamed type which is to be named by a naming strategy.
         */
        class Default implements UnnamedType {

            /**
             * The unnamed type's super class.
             */
            private final GenericTypeDescription superClass;

            /**
             * The unnamed type's interfaces.
             */
            private final List<? extends GenericTypeDescription> interfaces;

            /**
             * The unnamed type's modifiers.
             */
            private final int modifiers;

            /**
             * The class file version of the unnamed type.
             */
            private final ClassFileVersion classFileVersion;

            /**
             * Creates a new unnamed type.
             *
             * @param superClass       The unnamed type's super class.
             * @param interfaces       The unnamed type's interfaces.
             * @param modifiers        The unnamed type's modifiers.
             * @param classFileVersion The class file version of the unnamed type.
             */
            public Default(GenericTypeDescription superClass,
                           List<? extends GenericTypeDescription> interfaces,
                           int modifiers,
                           ClassFileVersion classFileVersion) {
                this.superClass = superClass;
                this.interfaces = interfaces;
                this.modifiers = modifiers;
                this.classFileVersion = classFileVersion;
            }

            @Override
            public GenericTypeDescription getSuperClass() {
                return superClass;
            }

            @Override
            public List<GenericTypeDescription> getDeclaredInterfaces() {
                return new ArrayList<GenericTypeDescription>(interfaces);
            }

            @Override
            public Visibility getVisibility() {
                switch (modifiers & ByteBuddyCommons.VISIBILITY_MODIFIER_MASK) {
                    case Opcodes.ACC_PUBLIC:
                        return Visibility.PUBLIC;
                    case Opcodes.ACC_PROTECTED:
                        return Visibility.PROTECTED;
                    case Opcodes.ACC_PRIVATE:
                        return Visibility.PRIVATE;
                    case ModifierContributor.EMPTY_MASK:
                        return Visibility.PACKAGE_PRIVATE;
                    default:
                        throw new IllegalStateException("Ambiguous modifier: " + modifiers);
                }
            }

            @Override
            public TypeManifestation getTypeManifestation() {
                if ((modifiers & Modifier.FINAL) != 0) {
                    return TypeManifestation.FINAL;
                    // Note: Interfaces are abstract, the interface condition needs to be checked before abstraction.
                } else if ((modifiers & Opcodes.ACC_INTERFACE) != 0) {
                    return TypeManifestation.INTERFACE;
                } else if ((modifiers & Opcodes.ACC_ABSTRACT) != 0) {
                    return TypeManifestation.ABSTRACT;
                } else {
                    return TypeManifestation.PLAIN;
                }
            }

            @Override
            public EnumerationState getEnumerationState() {
                return EnumerationState.is((modifiers & Opcodes.ACC_ENUM) != 0);
            }

            @Override
            public SyntheticState getSyntheticState() {
                return SyntheticState.is((modifiers & Opcodes.ACC_SYNTHETIC) != 0);
            }

            @Override
            public ClassFileVersion getClassFileVersion() {
                return classFileVersion;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Default aDefault = (Default) other;
                return modifiers == aDefault.modifiers
                        && classFileVersion.equals(aDefault.classFileVersion)
                        && interfaces.equals(aDefault.interfaces)
                        && superClass.equals(aDefault.superClass);
            }

            @Override
            public int hashCode() {
                int result = superClass.hashCode();
                result = 31 * result + interfaces.hashCode();
                result = 31 * result + modifiers;
                result = 31 * result + classFileVersion.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "NamingStrategy.UnnamedType.Default{" +
                        "superClass=" + superClass +
                        ", interfaces=" + interfaces +
                        ", modifiers=" + modifiers +
                        ", classFileVersion=" + classFileVersion +
                        '}';
            }
        }
    }

    /**
     * An unbound {@link net.bytebuddy.NamingStrategy} where the actual naming strategy is still to be determined
     * in dependency of whether a type is to be subclasses, redefined or rebased.
     */
    interface Unbound {

        /**
         * Returns a naming strategy for subclassing a type.
         *
         * @param typeDescription The type that the user specified to be subclasses.
         * @return A naming strategy for naming the generated subclass.
         */
        NamingStrategy subclass(TypeDescription typeDescription);

        /**
         * Returns a naming strategy for redefining a type.
         *
         * @param typeDescription The type that the user specified to be subclasses.
         * @return A naming strategy for naming the redefined class.
         */
        NamingStrategy redefine(TypeDescription typeDescription);

        /**
         * Returns a naming strategy for rebasing a type.
         *
         * @param typeDescription The type that the user specified to be subclasses.
         * @return A naming strategy for naming the rebased class.
         */
        NamingStrategy rebase(TypeDescription typeDescription);

        /**
         * Returns a naming strategy for a type without an explicit base.
         *
         * @return A naming strategy for a type without an explicit base.
         */
        NamingStrategy create();

        /**
         * A default unbound {@link net.bytebuddy.NamingStrategy} where rebased or redefined classes keep
         * their original name and where subclasses are named using a {@link net.bytebuddy.NamingStrategy.SuffixingRandom}
         * strategy.
         */
        class Default implements Unbound {

            /**
             * The type name used for created classes.
             */
            public static final String CREATION_NAME = "net.bytebuddy.generated.Type";

            /**
             * The suffix to apply for generated subclasses.
             */
            private final String suffix;

            /**
             * Creates a default unbound naming strategy.
             *
             * @param suffix The suffix to apply for generated subclasses
             */
            public Default(String suffix) {
                this.suffix = suffix;
            }

            @Override
            public NamingStrategy subclass(TypeDescription typeDescription) {
                return new SuffixingRandom(suffix, new SuffixingRandom.BaseNameResolver.ForGivenType(typeDescription));
            }

            @Override
            public NamingStrategy redefine(TypeDescription typeDescription) {
                return new Fixed(typeDescription.getName());
            }

            @Override
            public NamingStrategy rebase(TypeDescription typeDescription) {
                return new Fixed(typeDescription.getName());
            }

            @Override
            public NamingStrategy create() {
                return new SuffixingRandom(suffix, new SuffixingRandom.BaseNameResolver.ForFixedValue(CREATION_NAME));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && suffix.equals(((Default) other).suffix);
            }

            @Override
            public int hashCode() {
                return suffix.hashCode();
            }

            @Override
            public String toString() {
                return "NamingStrategy.Unbound.Default{" +
                        "suffix='" + suffix + '\'' +
                        '}';
            }
        }

        /**
         * A unified unbound naming strategy which always applies a given naming strategy.
         */
        class Unified implements Unbound {

            /**
             * The unified naming strategy.
             */
            private final NamingStrategy namingStrategy;

            /**
             * Creates a new unified naming strategy.
             *
             * @param namingStrategy The unified naming strategy.
             */
            public Unified(NamingStrategy namingStrategy) {
                this.namingStrategy = namingStrategy;
            }

            @Override
            public NamingStrategy subclass(TypeDescription typeDescription) {
                return namingStrategy;
            }

            @Override
            public NamingStrategy redefine(TypeDescription typeDescription) {
                return namingStrategy;
            }

            @Override
            public NamingStrategy rebase(TypeDescription typeDescription) {
                return namingStrategy;
            }

            @Override
            public NamingStrategy create() {
                return namingStrategy;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && namingStrategy.equals(((Unified) other).namingStrategy);
            }

            @Override
            public int hashCode() {
                return namingStrategy.hashCode();
            }

            @Override
            public String toString() {
                return "NamingStrategy.Unbound.Unified{" +
                        "namingStrategy=" + namingStrategy +
                        '}';
            }
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
    class SuffixingRandom implements NamingStrategy {

        /**
         * The default package for defining types that are renamed to not be contained in the
         * {@link net.bytebuddy.NamingStrategy.SuffixingRandom#JAVA_PACKAGE} package.
         */
        public static final String BYTE_BUDDY_RENAME_PACKAGE = "net.bytebuddy.renamed";

        /**
         * The package prefix of the {@code java.**} packages for which the definition of non-bootstrap types is
         * illegal.
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
            this(suffix, BaseNameResolver.ForUnnamedType.INSTANCE, BYTE_BUDDY_RENAME_PACKAGE);
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
         *                              {@code java.lang} namespace.
         */
        public SuffixingRandom(String suffix, BaseNameResolver baseNameResolver, String javaLangPackagePrefix) {
            this.suffix = suffix;
            this.baseNameResolver = baseNameResolver;
            this.javaLangPackagePrefix = javaLangPackagePrefix;
            randomString = new RandomString();
        }

        @Override
        public String name(UnnamedType unnamedType) {
            String baseName = baseNameResolver.resolve(unnamedType);
            if (baseName.startsWith(JAVA_PACKAGE)) {
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
             * Returns the resolved name.
             *
             * @param unnamedType The unnamed type which is to be named.
             * @return The resolved name.
             */
            String resolve(UnnamedType unnamedType);

            /**
             * Uses the unnamed type's super type's name as the resolved name.
             */
            enum ForUnnamedType implements BaseNameResolver {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public String resolve(UnnamedType unnamedType) {
                    return unnamedType.getSuperClass().asErasure().getName();
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
                public String resolve(UnnamedType unnamedType) {
                    return typeDescription.getName();
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
                public String resolve(UnnamedType unnamedType) {
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
    class PrefixingRandom implements NamingStrategy {

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
        public String name(UnnamedType unnamedType) {
            return String.format("%s.%s$%s", prefix, unnamedType.getSuperClass().asErasure().getName(), randomString.nextString());
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

    /**
     * A naming strategy that applies a fixed name.
     * <p>&nbsp;</p>
     * This strategy should only be used for one shot type generators since they will otherwise create types that
     * impose naming conflicts.
     */
    class Fixed implements NamingStrategy {

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
