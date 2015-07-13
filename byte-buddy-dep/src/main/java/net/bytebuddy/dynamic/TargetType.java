package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;

/**
 * This type is used as a place holder for creating methods or fields that refer to the type that currently subject
 * of creation within a {@link net.bytebuddy.dynamic.DynamicType.Builder}.
 */
public final class TargetType {

    /**
     * A description representation of the {@link net.bytebuddy.dynamic.TargetType}.
     */
    public static final TypeDescription DESCRIPTION = new TypeDescription.ForLoadedType(TargetType.class);

    /**
     * An unusable constructor to avoid instance creation.
     */
    private TargetType() {
        throw new UnsupportedOperationException("This class only serves as a marker type");
    }
}
