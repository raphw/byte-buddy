package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.matcher.ElementMatcher;

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
     * As the {@link net.bytebuddy.dynamic.TargetType} is only to be used as a marker, its constructor is made inaccessible.
     */
    private TargetType() {
        throw new UnsupportedOperationException("This is a place holder type that should not be instantiated");
    }

    public static TypeDescription resolveRaw(TypeDescription typeDescription,
                                             TypeDescription actualTargetType,
                                             ElementMatcher<? super TypeDescription> matcher) {
        int arity = 0;
        TypeDescription targetType = typeDescription;
        while (targetType.isArray()) {
            targetType = targetType.getComponentType();
            arity++;
        }
        return matcher.matches(typeDescription)
                ? TypeDescription.ArrayProjection.of(actualTargetType, arity)
                : typeDescription;
    }

    public static GenericTypeDescription resolve(GenericTypeDescription typeDescription,
                                                 TypeDescription actualTargetType,
                                                 ElementMatcher<? super TypeDescription> matcher) {
        switch (typeDescription.getSort()) {
            case RAW:
                return resolve(typeDescription.asRawType(), actualTargetType, matcher);
            case GENERIC_ARRAY:
                return GenericTypeDescription.ForGenericArray.Latent.of(resolve(typeDescription.getComponentType(), actualTargetType, matcher), 1);
            case PARAMETERIZED:
                GenericTypeDescription ownerType = typeDescription.getOwnerType();
                return new GenericTypeDescription.ForParameterizedType.Latent(resolveRaw(typeDescription.asRawType(), actualTargetType, matcher),
                        resolve(typeDescription.getParameters(), actualTargetType, matcher),
                        ownerType == null
                                ? null
                                : resolve(ownerType, actualTargetType, matcher));
            case VARIABLE:
                return new GenericTypeDescription.ForTypeVariable.Latent(resolve(typeDescription.getParameters(), actualTargetType, matcher),
                        null, // TODO: How to deal with recursion here?
                        typeDescription.getSymbol());
            case WILDCARD:
                List<GenericTypeDescription> lowerBounds = typeDescription.getLowerBounds();
                return lowerBounds.isEmpty()
                        ? GenericTypeDescription.ForWildcardType.Latent
                        .boundedAbove(resolve(typeDescription.getUpperBounds().get(0), actualTargetType, matcher))
                        : GenericTypeDescription.ForWildcardType.Latent.boundedBelow(resolve(lowerBounds.get(0), actualTargetType, matcher));
            default:
                throw new AssertionError("Unexpected generic type: " + typeDescription.getSort());
        }
    }

    public static TypeList resolveRaw(List<? extends TypeDescription> typeList,
                                      TypeDescription actualTargetType,
                                      ElementMatcher<? super TypeDescription> matcher) {
        List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(typeList.size());
        for (TypeDescription typeDescription : typeList) {
            typeDescriptions.add(resolveRaw(typeDescription, actualTargetType, matcher));
        }
        return new TypeList.Explicit(typeDescriptions);
    }

    public static GenericTypeList resolve(List<? extends GenericTypeDescription> typeList,
                                          TypeDescription actualTargetType,
                                          ElementMatcher<? super TypeDescription> matcher) {
        List<GenericTypeDescription> typeDescriptions = new ArrayList<GenericTypeDescription>(typeList.size());
        for (GenericTypeDescription typeDescription : typeList) {
            typeDescriptions.add(resolve(typeDescription, actualTargetType, matcher));
        }
        return new GenericTypeList.Explicit(typeDescriptions);
    }
}
