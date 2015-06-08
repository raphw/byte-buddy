package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * This type is used as a place holder for creating methods or fields that refer to the type that currently subject
 * of creation within a {@link net.bytebuddy.dynamic.DynamicType.Builder}.
 */
public final class TargetType {

    /**
     * A description representation of the {@link net.bytebuddy.dynamic.TargetType}.
     */
    public static final TypeDescription DESCRIPTION = new TypeDescription.ForLoadedType(TargetType.class);

    public static final ElementMatcher<TypeDescription> MATCHER = is(DESCRIPTION);

    /**
     * As the {@link net.bytebuddy.dynamic.TargetType} is only to be used as a marker, its constructor is made inaccessible.
     */
    private TargetType() {
        throw new UnsupportedOperationException("This is a place holder type that should not be instantiated");
    }

    public static GenericTypeDescription resolve(GenericTypeDescription typeDescription, TypeDescription substitute) {
        return typeDescription.accept(GenericTypeDescription.Visitor.Substitutor.ForRawType.replace(substitute, MATCHER));
    }

    public static GenericTypeList resolve(List<? extends GenericTypeDescription> typeDescriptions, TypeDescription substitute) {
        List<GenericTypeDescription> substituted = new ArrayList<GenericTypeDescription>(typeDescriptions.size());
        GenericTypeDescription.Visitor<GenericTypeDescription> visitor = GenericTypeDescription.Visitor.Substitutor.ForRawType.replace(substitute, MATCHER);
        for (GenericTypeDescription typeDescription : typeDescriptions) {
            substituted.add(typeDescription.accept(visitor));
        }
        return new GenericTypeList.Explicit(substituted);
    }
}
