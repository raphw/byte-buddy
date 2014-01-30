package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import java.lang.reflect.Modifier;

public interface ModifierReviewable {

    static abstract class AbstractModifierReviewable implements ModifierReviewable {

        @Override
        public boolean isAbstract() {
            return Modifier.isAbstract(getModifiers());
        }

        @Override
        public boolean isFinal() {
            return Modifier.isFinal(getModifiers());
        }

        @Override
        public boolean isStatic() {
            return Modifier.isStatic(getModifiers());
        }

        @Override
        public boolean isPublic() {
            return Modifier.isPublic(getModifiers());
        }

        @Override
        public boolean isProtected() {
            return Modifier.isProtected(getModifiers());
        }

        @Override
        public boolean isPackagePrivate() {
            return !isPublic() && !isProtected() && !isPrivate();
        }

        @Override
        public boolean isPrivate() {
            return Modifier.isPrivate(getModifiers());
        }

        @Override
        public boolean isNative() {
            return Modifier.isNative(getModifiers());
        }

        @Override
        public boolean isSynchronized() {
            return Modifier.isSynchronized(getModifiers());
        }

        @Override
        public boolean isStrict() {
            return Modifier.isStrict(getModifiers());
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

    int getModifiers();

    boolean isSynthetic();
}
