package net.bytebuddy.matcher;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.ModifierReviewable;
import org.objectweb.asm.Opcodes;

/**
 * An element matcher that matches a byte code element by its modifiers.
 *
 * @param <T> The type of the matched entity.
 */
@EqualsAndHashCode(callSuper = false)
public class ModifierMatcher<T extends ModifierReviewable> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matching mode to apply by this modifier matcher.
     */
    private final Mode mode;

    /**
     * Creates a new element matcher that matches an element by its modifier.
     *
     * @param mode The match mode to apply to the matched element's modifier.
     */
    public ModifierMatcher(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean matches(T target) {
        return (mode.getModifiers() & target.getModifiers()) != 0;
    }

    @Override
    public String toString() {
        return mode.getDescription();
    }

    /**
     * Determines the type of modifier to be matched by a {@link net.bytebuddy.matcher.ModifierMatcher}.
     */
    public enum Mode {

        /**
         * Matches an element that is considered {@code public}.
         */
        PUBLIC(Opcodes.ACC_PUBLIC, "isPublic()"),

        /**
         * Matches an element that is considered {@code protected}.
         */
        PROTECTED(Opcodes.ACC_PROTECTED, "isProtected()"),

        /**
         * Matches an element that is considered {@code private}.
         */
        PRIVATE(Opcodes.ACC_PRIVATE, "isPrivate()"),

        /**
         * Matches an element that is considered {@code final}.
         */
        FINAL(Opcodes.ACC_FINAL, "isFinal()"),

        /**
         * Matches an element that is considered {@code static}.
         */
        STATIC(Opcodes.ACC_STATIC, "isStatic()"),

        /**
         * Matches an element that is considered {@code synchronized}.
         */
        SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED, "isSynchronized()"),

        /**
         * Matches an element that is considered {@code native}.
         */
        NATIVE(Opcodes.ACC_NATIVE, "isNative()"),

        /**
         * Matches an element that is considered {@code strict}.
         */
        STRICT(Opcodes.ACC_STRICT, "isStrict()"),

        /**
         * Matches an element that is considered to be varargs.
         */
        VAR_ARGS(Opcodes.ACC_VARARGS, "isVarArgs()"),

        /**
         * Matches an element that is considered {@code synthetic}.
         */
        SYNTHETIC(Opcodes.ACC_SYNTHETIC, "isSynthetic()"),

        /**
         * Matches an element that is considered a bridge method.
         */
        BRIDGE(Opcodes.ACC_BRIDGE, "isBridge()"),

        /**
         * Matches an element that is considered {@code abstract}.
         */
        ABSTRACT(Opcodes.ACC_ABSTRACT, "isAbstract()"),

        /**
         * Matches a type that is considered an interface.
         */
        INTERFACE(Opcodes.ACC_INTERFACE, "isInterface()"),

        /**
         * Matches a type that is considered an annotation.
         */
        ANNOTATION(Opcodes.ACC_ANNOTATION, "isAnnotation()"),

        /**
         * Matches a volatile field.
         */
        VOLATILE(Opcodes.ACC_VOLATILE, "isVolatile()"),

        /**
         * Matches a transient field.
         */
        TRANSIENT(Opcodes.ACC_TRANSIENT, "isTransient()"),

        /**
         * Matches a mandated parameter.
         */
        MANDATED(Opcodes.ACC_MANDATED, "isMandated()"),

        /**
         * Matches a type or field for describing an enumeration.
         */
        ENUMERATION(Opcodes.ACC_ENUM, "isEnum()");

        /**
         * The mask of the modifier to match.
         */
        private final int modifiers;

        /**
         * The textual representation of this instance's matching mode.
         */
        private final String description;

        /**
         * Creates a new modifier matcher mode.
         *
         * @param modifiers   The mask of the modifier to match.
         * @param description The textual representation of this instance's matching mode.
         */
        Mode(int modifiers, String description) {
            this.modifiers = modifiers;
            this.description = description;
        }

        /**
         * Returns the textual description of this mode.
         *
         * @return The textual description of this mode.
         */
        protected String getDescription() {
            return description;
        }

        /**
         * Returns the modifiers to match by this mode.
         *
         * @return The modifiers to match by this mode.
         */
        protected int getModifiers() {
            return modifiers;
        }

        @Override
        public String toString() {
            return "ModifierMatcher.Mode." + name();
        }
    }
}
