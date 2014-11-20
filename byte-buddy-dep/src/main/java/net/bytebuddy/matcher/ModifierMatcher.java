package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.ModifierReviewable;
import org.objectweb.asm.Opcodes;

/**
 * Matches a method by a modifier. The method is matched if any bit of the modifier is set on a method.
 */
class ModifierMatcher<T extends ModifierReviewable> extends ElementMatcher.Junction.AbstractBase<T> {

    public static enum MatchMode {

        PUBLIC(Opcodes.ACC_PUBLIC, "isPublic()"),
        PROTECTED(Opcodes.ACC_PROTECTED, "isProtected()"),
        PRIVATE(Opcodes.ACC_PRIVATE, "isPrivate()"),
        FINAL(Opcodes.ACC_FINAL, "isFinal()"),
        STATIC(Opcodes.ACC_STATIC, "isStatic()"),
        SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED, "isSynchronized()"),
        NATIVE(Opcodes.ACC_NATIVE, "isNative()"),
        STRICT(Opcodes.ACC_STRICT, "isStrict()"),
        VAR_ARGS(Opcodes.ACC_VARARGS, "isVarArgs()"),
        SYNTHETIC(Opcodes.ACC_SYNTHETIC, "isSynthetic()"),
        BRIDGE(Opcodes.ACC_BRIDGE, "isBridge()");

        private final int modifier;

        private final String description;

        private MatchMode(int modifier, String description) {
            this.modifier = modifier;
            this.description = description;
        }

        /**
         * Returns the description of this match mode.
         *
         * @return The description of this match mode.
         */
        protected String getDescription() {
            return description;
        }

        protected int getModifiers() {
            return modifier;
        }
    }

    private final MatchMode matchMode;

    public ModifierMatcher(MatchMode matchMode) {
        this.matchMode = matchMode;
    }

    @Override
    public boolean matches(T target) {
        return (matchMode.getModifiers() & target.getModifiers()) != 0;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && matchMode == ((ModifierMatcher) other).matchMode;
    }

    @Override
    public int hashCode() {
        return matchMode.hashCode();
    }

    @Override
    public String toString() {
        return matchMode.getDescription();
    }
}
