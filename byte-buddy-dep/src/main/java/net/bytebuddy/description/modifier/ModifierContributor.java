package net.bytebuddy.description.modifier;

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
}
