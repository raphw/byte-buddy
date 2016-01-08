package net.bytebuddy.description.type;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericBuilderTest extends AbstractTypeDescriptionGenericTest {

    @Override
    protected TypeDescription.Generic describe(Field field) {
        return describe(field.getGenericType()).accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new FieldDescription.ForLoadedField(field)));
    }

    @Override
    protected TypeDescription.Generic describe(Method method) {
        return describe(method.getGenericReturnType()).accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new MethodDescription.ForLoadedMethod(method)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoOwnerTypeWhenRequired() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.Bar.class, Object.class);
    }

    @Test
    public void testImplicitOwnerTypeWhenRequired() throws Exception {
        assertThat(TypeDescription.Generic.Builder.parameterizedType(Foo.class, Object.class).asType().getOwnerType(),
                is(TypeDefinition.Sort.describe(getClass())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOwnerTypeWhenNotRequired() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.class, Object.class, Collections.<Type>singletonList(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalOwnerType() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.Bar.class, Object.class, Collections.<Type>singletonList(Foo.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonGenericOwnerType() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.Bar.class, Foo.class, Collections.<Type>singletonList(Foo.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenericOwnerType() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(new TypeDescription.ForLoadedType(Foo.Qux.class),
                TypeDescription.Generic.Builder.parameterizedType(Foo.class, Object.class).asType(),
                Collections.<TypeDefinition>singletonList(TypeDescription.OBJECT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleParameterTypeNumber() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.class);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Builder.class).apply();
    }

    private TypeDescription.Generic describe(Type type) {
        if (type instanceof Class) {
            return builder(type).asType();
        } else if (type instanceof TypeVariable) {
            return TypeDescription.Generic.Builder.typeVariable(((TypeVariable) type).getName());
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            if (wildcardType.getLowerBounds().length > 0) {
                return builder(wildcardType.getLowerBounds()[0]).asWildcardLowerBound();
            } else if (wildcardType.getUpperBounds().length > 0) {
                return builder(wildcardType.getUpperBounds()[0]).asWildcardUpperBound();
            } else {
                return TypeDescription.Generic.Builder.unboundWildcard();
            }
        } else if (type instanceof GenericArrayType) {
            return builder(type).asType();
        } else if (type instanceof ParameterizedType) {
            return builder(type).asType();
        } else {
            throw new AssertionError("Unknown type: " + type);
        }
    }

    private TypeDescription.Generic.Builder builder(Type type) {
        if (type instanceof Class) {
            return TypeDescription.Generic.Builder.rawType(((Class<?>) type));
        } else if (type instanceof GenericArrayType) {
            return builder(((GenericArrayType) type).getGenericComponentType()).asArray();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return TypeDescription.Generic.Builder.parameterizedType((Class<?>) parameterizedType.getRawType(),
                    parameterizedType.getOwnerType(),
                    Arrays.asList(parameterizedType.getActualTypeArguments()));
        } else {
            throw new AssertionError("Unexpected type: " + type);
        }
    }

    @SuppressWarnings("unused")
    private static class Foo<T> {

        private class Bar<S> {
            /* empty */
        }

        private static class Qux<S> {
            /* empty */
        }
    }
}
