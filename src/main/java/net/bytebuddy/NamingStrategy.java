package net.bytebuddy;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.modifier.MemberVisibility;
import net.bytebuddy.modifier.SyntheticState;
import net.bytebuddy.modifier.TypeManifestation;

import java.util.Collection;
import java.util.Random;

/**
 * A naming strategy for finding a fully qualified internalName for a Java type.
 * <p>&nbsp;</p>
 * Note that subclasses that lie within the same package as their superclass have improved access to overriding
 * package-private methods of their super type.
 */
public interface NamingStrategy {

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
        MemberVisibility getVisibility();

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
         * Returns the class format version of this unnamed type.
         *
         * @return The class format version of this unnamed type.
         */
        ClassFormatVersion getClassFormatVersion();
    }

    /**
     * A naming strategy that creates a internalName by concatenating
     * <ol>
     * <li>The super classes package and internalName</li>
     * <li>A given suffix string</li>
     * <li>A random number</li>
     * </ol>
     * Between all these elements, a {@code $} sign is included into the internalName to improve readability.
     */
    static class SuffixingRandom implements NamingStrategy {

        private static final String JAVA_LANG_PACKAGE = "java.lang.";
        private static final String BYTE_BUDDY_RENAME_PACKAGE = "net.bytebuddy.renamed";

        private final String suffix;
        private final String javaLangPackagePrefix;
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
        public String getName(UnnamedType unnamedType) {
            String superClassName = unnamedType.getSuperClass().getName();
            if (superClassName.startsWith(JAVA_LANG_PACKAGE)) {
                superClassName = javaLangPackagePrefix + "." + superClassName;
            }
            return String.format("%s$$%s$$%d", superClassName, suffix, Math.abs(random.nextInt()));
        }
    }

    /**
     * A naming strategy that applies a fixed internalName.
     * <p>&nbsp;</p>
     * This strategy should only be used for one shot type generators since they will otherwise create types that
     * impose naming conflicts.
     */
    static class Fixed implements NamingStrategy {

        private final String name;

        /**
         * Creates an immutable fixed naming strategy.
         *
         * @param name The internalName for the created type.
         */
        public Fixed(String name) {
            this.name = name;
        }

        @Override
        public String getName(UnnamedType UnnamedType) {
            return name;
        }
    }

    /**
     * Generates a fully qualified internalName for a Java type. The resulting should not lie within the {@code java.lang}
     * package since such types cannot be loaded using a normal class loader. Also, the internalName should not yet be taken
     * by another type since this would cause conflicts in the internalName space. Therefore, it is recommendable to include
     * a random sequence within the internalName.
     *
     * @param unnamedType An unnamed type that is to be named.
     * @return A valid identifier for a Java type.
     */
    String getName(UnnamedType unnamedType);
}
