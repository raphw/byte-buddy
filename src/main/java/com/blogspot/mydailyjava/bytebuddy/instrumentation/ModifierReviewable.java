package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public interface ModifierReviewable {

    static abstract class AbstractModifierReviewable implements ModifierReviewable {

        @Override
        public boolean isAbstract() {
            return matchesMask(Modifier.ABSTRACT);
        }

        @Override
        public boolean isFinal() {
            return matchesMask(Modifier.FINAL);
        }

        @Override
        public boolean isStatic() {
            return matchesMask(Modifier.STATIC);
        }

        @Override
        public boolean isPublic() {
            return matchesMask(Modifier.PUBLIC);
        }

        @Override
        public boolean isProtected() {
            return matchesMask(Modifier.PROTECTED);
        }

        @Override
        public boolean isPackagePrivate() {
            return !isPublic() && !isProtected() && !isPrivate();
        }

        @Override
        public boolean isPrivate() {
            return matchesMask(Modifier.PRIVATE);
        }

        @Override
        public boolean isNative() {
            return matchesMask(Modifier.NATIVE);
        }

        @Override
        public boolean isSynchronized() {
            return matchesMask(Modifier.SYNCHRONIZED);
        }

        @Override
        public boolean isStrict() {
            return matchesMask(Modifier.STRICT);
        }

        @Override
        public boolean isSynthetic() {
            return matchesMask(Opcodes.ACC_SYNTHETIC);
        }

        public boolean isSuper() {
            return matchesMask(Opcodes.ACC_SUPER);
        }

        public boolean isBridge() {
            return matchesMask(Opcodes.ACC_BRIDGE);
        }

        public boolean isDeprecated() {
            return matchesMask(Opcodes.ACC_DEPRECATED);
        }

        public boolean isAnnotation() {
            return matchesMask(Opcodes.ACC_ANNOTATION);
        }

        public boolean isEnum() {
            return matchesMask(Opcodes.ACC_ENUM);
        }

        public boolean isInterface() {
            return matchesMask(Opcodes.ACC_INTERFACE);
        }

        public boolean isTransient() {
            return matchesMask(Opcodes.ACC_TRANSIENT);
        }

        public boolean isVolatile() {
            return matchesMask(Opcodes.ACC_VOLATILE);
        }

        public boolean isVarArgs() {
            return matchesMask(Opcodes.ACC_VARARGS);
        }

        private boolean matchesMask(int mask) {
            return (getModifiers() & mask) != 0;
        }
    }

    boolean isFinal();

    boolean isStatic();

    boolean isPublic();

    boolean isProtected();

    boolean isPackagePrivate();

    boolean isPrivate();

    boolean isAbstract();

    boolean isNative();

    boolean isSynchronized();

    boolean isStrict();

    boolean isSynthetic();

    int getModifiers();
}
