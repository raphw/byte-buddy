package net.bytebuddy.dynamic;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.util.ArrayList;
import java.util.List;

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
     * Resolves the given type description with the actual target type if the {@code typeDescription} resembles the
     * {@link TargetType} placeholder.
     *
     * @param typeDescription  The type description to resolve.
     * @param actualTargetType The actual type for which {@link TargetType} was a placeholder.
     * @return The resolved type description.
     */
    public static TypeDescription resolve(TypeDescription typeDescription, TypeDescription actualTargetType) {
        return typeDescription.represents(TargetType.class)
                ? actualTargetType
                : typeDescription;
    }

    /**
     * Resolves any type description in the given listwith the actual target type if the {@code typeDescription} resembles the
     * {@link TargetType} placeholder.
     *
     * @param typeList         The list to resolve.
     * @param actualTargetType The actual type for which {@link TargetType} was a placeholder.
     * @return The resolved list of type descriptions.
     */
    public static TypeList resolve(List<? extends TypeDescription> typeList, TypeDescription actualTargetType) {
        List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(typeList.size());
        for (TypeDescription typeDescription : typeList) {
            typeDescriptions.add(resolve(typeDescription, actualTargetType));
        }
        return new TypeList.Explicit(typeDescriptions);
    }

    /**
     * As the {@link net.bytebuddy.dynamic.TargetType} is only to be used as a marker, its constructor is made inaccessible.
     */
    private TargetType() {
        throw new UnsupportedOperationException("This is a place holder type that should not be instantiated");
    }
}
