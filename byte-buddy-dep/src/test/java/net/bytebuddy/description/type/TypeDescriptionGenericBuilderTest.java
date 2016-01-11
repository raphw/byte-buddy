package net.bytebuddy.description.type;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericBuilderTest extends AbstractTypeDescriptionGenericTest {

    @Override
    protected TypeDescription.Generic describeType(Field field) {
        return describe(field.getGenericType(), TypeDescription.Generic.AnnotationReader.DISPATCHER.resolve(field))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new FieldDescription.ForLoadedField(field)));
    }

    @Override
    protected TypeDescription.Generic describeReturnType(Method method) {
        return describe(method.getGenericReturnType(), TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveReturnType(method))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new MethodDescription.ForLoadedMethod(method)));
    }

    @Override
    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return describe(method.getGenericParameterTypes()[index], TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveParameterType(method, index))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new MethodDescription.ForLoadedMethod(method)));
    }

    @Override
    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return describe(method.getExceptionTypes()[index], TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveExceptionType(method, index))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new MethodDescription.ForLoadedMethod(method)));
    }

    @Override
    protected TypeDescription.Generic describeSuperType(Class<?> type) {
        return describe(type.getGenericSuperclass(), TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveSuperType(type))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new TypeDescription.ForLoadedType(type)));
    }

    @Override
    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return describe(type.getGenericInterfaces()[0], TypeDescription.Generic.AnnotationReader.DISPATCHER.resolveInterface(type, index))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new TypeDescription.ForLoadedType(type)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoOwnerTypeWhenRequired() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.Bar.class, Object.class);
    }

    @Test
    public void testImplicitOwnerTypeWhenRequired() throws Exception {
        assertThat(TypeDescription.Generic.Builder.parameterizedType(Foo.class, Object.class).build().getOwnerType(),
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
                TypeDescription.Generic.Builder.parameterizedType(Foo.class, Object.class).build(),
                Collections.<TypeDefinition>singletonList(TypeDescription.OBJECT));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleParameterTypeNumber() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.class);
    }

    @Test
    @Ignore("Refactoring")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Builder.class).apply();
    }

    private TypeDescription.Generic describe(Type type, TypeDescription.Generic.AnnotationReader annotationReader) {
        if (type instanceof Class) {
            return builder(type, annotationReader).build(annotationReader.asList());
        } else if (type instanceof TypeVariable) {
            return TypeDescription.Generic.Builder.typeVariable(((TypeVariable) type).getName(), annotationReader.asList());
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            if (wildcardType.getLowerBounds().length > 0) {
                return builder(wildcardType.getLowerBounds()[0], annotationReader.ofWildcardLowerBound(0)).asWildcardLowerBound(annotationReader.asList());
            } else if (wildcardType.getUpperBounds().length > 0) {
                return builder(wildcardType.getUpperBounds()[0], annotationReader.ofWildcardUpperBound(0)).asWildcardUpperBound(annotationReader.asList());
            } else {
                return TypeDescription.Generic.Builder.unboundWildcard(annotationReader.asList()); // TODO: Remove?
            }
        } else if (type instanceof GenericArrayType) {
            return builder(type, annotationReader.ofComponentType()).build(annotationReader.asList());
        } else if (type instanceof ParameterizedType) {
            return builder(type, annotationReader).build(annotationReader.asList());
        } else {
            throw new AssertionError("Unknown type: " + type);
        }
    }

    private TypeDescription.Generic.Builder builder(Type type, TypeDescription.Generic.AnnotationReader annotationReader) {
        if (type instanceof Class) {
            return TypeDescription.Generic.Builder.rawType(((Class<?>) type));
        } else if (type instanceof GenericArrayType) {
            return builder(((GenericArrayType) type).getGenericComponentType(), annotationReader.ofComponentType()).asArray(annotationReader.asList());
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return TypeDescription.Generic.Builder.parameterizedType((Class<?>) parameterizedType.getRawType(),
                    parameterizedType.getOwnerType(),
                    Arrays.asList(parameterizedType.getActualTypeArguments())); // TODO: Parameter annotations?
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
