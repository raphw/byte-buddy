package com.blogspot.mydailyjava.bytebuddy.instrumentation;

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
    static final int EMPTY_MASK = 0;

    /**
     * A marker interface for modifiers that can be applied to methods.
     */
    static interface ForMethod extends ModifierContributor {
        /* marker interface */
    }

    /**
     * A marker interface for modifiers that can be applied to fields.
     */
    static interface ForField extends ModifierContributor {
        /* marker interface */
    }

    /**
     * A marker interface for modifiers that can be applied to types.
     */
    static interface ForType extends ModifierContributor {
        /* marker interface */
    }

    /**
     * Returns the mask of this modifier.
     *
     * @return The modifier mask that is to be applied to the target type or type member.
     */
    int getMask();
}
