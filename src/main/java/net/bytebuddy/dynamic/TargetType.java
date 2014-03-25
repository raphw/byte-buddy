package net.bytebuddy.dynamic;

import net.bytebuddy.instrumentation.type.TypeDescription;

/**
 * This type is used as a place holder for creating methods or fields that refer to the type that currently subject
 * of creation within a {@link net.bytebuddy.dynamic.DynamicType.Builder}.
 */
public final class TargetType {

    /**
     * A description representation of the {@link net.bytebuddy.dynamic.TargetType}.
     */
    public static final TypeDescription DESCRIPTION = new TypeDescription.ForLoadedType(TargetType.class);

    private TargetType() {
        throw new AssertionError("This is a place holder type that should not be instantiated");
    }
}
