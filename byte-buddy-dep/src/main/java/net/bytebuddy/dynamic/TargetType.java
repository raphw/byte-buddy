package net.bytebuddy.dynamic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.description.type.generic.TypeVariableSource;
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

    public static TypeDescription resolve(TypeDescription observed,
                                          TypeDescription substitute,
                                          ElementMatcher<? super TypeDescription> substitutionMatcher) {
        int arity = 0;
        TypeDescription componentType = observed;
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
            arity++;
        }
        return substitutionMatcher.matches(componentType)
                ? TypeDescription.ArrayProjection.of(substitute, arity)
                : observed;
    }

    public static GenericTypeDescription resolve(GenericTypeDescription observed,
                                                 TypeDescription substitute,
                                                 ElementMatcher<? super TypeDescription> substitutionMatcher) {
        switch (observed.getSort()) {
            case RAW:
                return resolve(observed.asRawType(), substitute, substitutionMatcher);
            case GENERIC_ARRAY:
                return GenericTypeDescription.ForGenericArray.Latent.of(resolve(observed.getComponentType(), substitute, substitutionMatcher), 1);
            case PARAMETERIZED:
                GenericTypeDescription ownerType = observed.getOwnerType();
                return new GenericTypeDescription.ForParameterizedType.Latent(resolve(observed.asRawType(), substitute, substitutionMatcher),
                        resolve(observed.getParameters(), substitute, substitutionMatcher),
                        ownerType == null
                                ? null
                                : resolve(ownerType, substitute, substitutionMatcher));
            case WILDCARD:
                GenericTypeList lowerBounds = observed.getLowerBounds(), upperBounds = observed.getUpperBounds();
                return lowerBounds.isEmpty()
                        ? GenericTypeDescription.ForWildcardType.Latent.boundedAbove(resolve(upperBounds.getOnly(), substitute, substitutionMatcher))
                        : GenericTypeDescription.ForWildcardType.Latent.boundedBelow(resolve(lowerBounds.getOnly(), substitute, substitutionMatcher));
            case VARIABLE:
                return observed.getVariableSource().accept(new TypeVariableProxy.Extractor(substitute, substitutionMatcher)).resolve(observed);
            default:
                throw new AssertionError("Unexpected generic type: " + observed.getSort());
        }
    }

    public static GenericTypeList resolve(List<? extends GenericTypeDescription> typeList,
                                          TypeDescription substitute,
                                          ElementMatcher<? super TypeDescription> substitutionMatcher) {
        List<GenericTypeDescription> resolved = new ArrayList<GenericTypeDescription>(typeList.size());
        for (GenericTypeDescription typeDescription : typeList) {
            resolved.add(resolve(typeDescription, substitute, substitutionMatcher));
        }
        return new GenericTypeList.Explicit(resolved);
    }

    protected interface TypeVariableProxy {

        GenericTypeDescription resolve(GenericTypeDescription original);

        enum Retaining implements TypeVariableProxy {

            INSTANCE;

            @Override
            public GenericTypeDescription resolve(GenericTypeDescription original) {
                return original;
            }
        }

        class ForType implements TypeVariableProxy {

            private final TypeDescription substitute;

            public ForType(TypeDescription substitute) {
                this.substitute = substitute;
            }

            @Override
            public GenericTypeDescription resolve(GenericTypeDescription original) {
                // TODO: Lazy!
                GenericTypeDescription typeVariable = substitute.findVariable(original.getSymbol());
                if (typeVariable == null) {
                    throw new IllegalStateException("Cannot resolve type variable " + original.getSymbol() + " for " + substitute);
                }
                return typeVariable;
            }
        }

        class ForMethod implements TypeVariableProxy {

            private final TypeDescription substitute;

            private final MethodDescription methodDescription;

            public ForMethod(TypeDescription substitute, MethodDescription methodDescription) {
                this.substitute = substitute;
                this.methodDescription = methodDescription; // TODO: Not method description, rather raw resolved look up.
            }

            @Override
            public GenericTypeDescription resolve(GenericTypeDescription original) {
                // TODO: Lazy!
                GenericTypeDescription typeVariable = substitute.getDeclaredMethods().filter(is(methodDescription))
                        .getOnly()
                        .findVariable(original.getSymbol());
                if (typeVariable == null) {
                    throw new IllegalStateException("Cannot resolve type variable " + original.getSymbol() + " for " + methodDescription);
                }
                return typeVariable;
            }
        }

        class Extractor implements TypeVariableSource.Visitor<TypeVariableProxy> {

            private final TypeDescription substitute;

            private final ElementMatcher<? super TypeDescription> substitutionMatcher;

            public Extractor(TypeDescription substitute, ElementMatcher<? super TypeDescription> substitutionMatcher) {
                this.substitute = substitute;
                this.substitutionMatcher = substitutionMatcher;
            }

            @Override
            public TypeVariableProxy onType(TypeDescription typeDescription) {
                return substitutionMatcher.matches(typeDescription)
                        ? new TypeVariableProxy.ForType(substitute)
                        : Retaining.INSTANCE;
            }

            @Override
            public TypeVariableProxy onMethod(MethodDescription methodDescription) {
                return substitutionMatcher.matches(methodDescription.getDeclaringType())
                        ? new TypeVariableProxy.ForMethod(substitute, methodDescription)
                        : Retaining.INSTANCE;
            }
        }
    }

    // TODO: Make resolution lazy for generic types.
}
