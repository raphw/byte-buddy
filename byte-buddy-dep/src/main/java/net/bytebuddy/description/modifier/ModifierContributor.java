package net.bytebuddy.description.modifier;

import java.util.Arrays;
import java.util.Collection;

/**
 * An element that describes a type modifier as described in the
 * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">JVMS</a>.
 * <p>&nbsp;</p>
 * This allows for a more expressive and type safe alternative of defining a type's or type member's modifiers.
 * However, note that modifier's that apply competing modifiers (such as {@code private} and {@code protected}
 * should not be combined and will result in invalid types. An exception is thrown when built-in modifiers that
 * cannot be combined are used together.
 */
public interface ModifierContributor {

    /**
     * The empty modifier.
     */
    int EMPTY_MASK = 0;

    /**
     * Returns the mask of this modifier.
     *
     * @return The modifier mask that is to be applied to the target type or type member.
     */
    int getMask();

    /**
     * Returns the entire range of modifiers that address this contributor's property.
     *
     * @return The range of this contributor's property.
     */
    int getRange();

    /**
     * Determines if this is the default modifier.
     *
     * @return {@code true} if this contributor represents the default modifier.
     */
    boolean isDefault();

    /**
     * A marker interface for modifiers that can be applied to methods.
     */
    interface ForMethod extends ModifierContributor {
        /* marker interface */
    }

    /**
     * A marker interface for modifiers that can be applied to fields.
     */
    interface ForField extends ModifierContributor {
        /* marker interface */
    }

    /**
     * A marker interface for modifiers that can be applied to types.
     */
    interface ForType extends ModifierContributor {
        /* marker interface */
    }

    /**
     * A marker interface for modifiers that can be applied to method parameters.
     */
    interface ForParameter extends ModifierContributor {
        /* marker interface */
    }

    class Resolver<T extends ModifierContributor> {

        private final Collection<? extends T> modifierContributors;

        protected Resolver(Collection<? extends T> modifierContributors) {
            this.modifierContributors = modifierContributors;
        }

        public static Resolver<ForType> of(ForType... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        public static Resolver<ForField> of(ForField... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        public static Resolver<ForMethod> of(ForMethod... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        public static Resolver<ForParameter> of(ForParameter... modifierContributor) {
            return Resolver.of(Arrays.asList(modifierContributor));
        }

        public static <S extends ModifierContributor> Resolver<S> of(Collection<? extends S> modifierContributors) {
            return new Resolver<S>(modifierContributors);
        }

        public int resolve() {
            return resolve(EMPTY_MASK);
        }

        public int resolve(int modifiers) {
            for (T modifierContributor : modifierContributors) {
                modifiers = (modifiers & ~modifierContributor.getRange()) | modifierContributor.getMask();
            }
            return modifiers;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && modifierContributors.equals(((Resolver<?>) other).modifierContributors);
        }

        @Override
        public int hashCode() {
            return modifierContributors.hashCode();
        }

        @Override
        public String toString() {
            return "ModifierContributor.Resolver{" +
                    "modifierContributors=" + modifierContributors +
                    '}';
        }
    }
}
