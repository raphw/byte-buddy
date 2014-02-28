package com.blogspot.mydailyjava.bytebuddy.dynamic;

/**
 * This type is used as a place holder for creating methods or fields that refer to the type that currently subject
 * of creation within a {@link com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType.Builder}.
 */
public final class TargetType {

    private TargetType() {
        throw new AssertionError("This is a place holder type that should not be instantiated");
    }
}
