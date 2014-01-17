package com.blogspot.mydailyjava.bytebuddy.method;

public interface MethodModifierReviewable {

    boolean isAbstract();

    boolean isFinal();

    boolean isStatic();

    boolean isPublic();

    boolean isProtected();

    boolean isPackagePrivate();

    boolean isPrivate();

    boolean isNative();

    boolean isSynchronized();

    boolean isStrict();
}
