package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class TypeDescriptionGenericBuilderTest extends AbstractTypeDescriptionGenericTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    protected TypeDescription.Generic describeType(Field field) {
        return describe(field.getGenericType(), new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new FieldDescription.ForLoadedField(field)));
    }

    protected TypeDescription.Generic describeReturnType(Method method) {
        return describe(method.getGenericReturnType(), new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedMethodReturnType(method))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new MethodDescription.ForLoadedMethod(method)));
    }

    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return describe(method.getGenericParameterTypes()[index], new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableParameterType(method, index))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new MethodDescription.ForLoadedMethod(method)));
    }

    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        Type[] type = method.getGenericExceptionTypes();
        Arrays.sort(type, new Comparator<Type>() {
            public int compare(Type left, Type right) {
                return left.toString().compareTo(right.toString());
            }
        });
        return describe(type[index], new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableExceptionType(method, index))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(new MethodDescription.ForLoadedMethod(method)));
    }

    protected TypeDescription.Generic describeSuperClass(Class<?> type) {
        return describe(type.getGenericSuperclass(), new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(type))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(TypeDescription.ForLoadedType.of(type)));
    }

    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return describe(type.getGenericInterfaces()[index], new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedInterface(type, index))
                .accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(TypeDescription.ForLoadedType.of(type)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoOwnerTypeWhenRequired() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.Inner.class, Object.class);
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
        TypeDescription.Generic.Builder.parameterizedType(Foo.Inner.class, Object.class, Collections.<Type>singletonList(Foo.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonGenericOwnerType() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.Inner.class, Foo.class, Collections.<Type>singletonList(Foo.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenericOwnerType() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(Foo.Nested.class),
                TypeDescription.Generic.Builder.parameterizedType(Foo.class, Object.class).build(),
                Collections.<TypeDefinition>singletonList(TypeDescription.ForLoadedType.of(Object.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleParameterTypeNumber() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Foo.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForbiddenZeroArity() throws Exception {
        TypeDescription.Generic.Builder.rawType(Foo.class).asArray(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testForbiddenNegativeType() throws Exception {
        TypeDescription.Generic.Builder.rawType(Foo.class).asArray(-1);
    }

    @Test
    public void testMultipleArityArray() throws Exception {
        assertThat(TypeDescription.Generic.Builder.rawType(Foo.class).asArray(2).build().getComponentType().getComponentType().represents(Foo.class), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotAnnotateVoid() throws Exception {
        TypeDescription.Generic.Builder.rawType(void.class).annotate(mock(AnnotationDescription.class)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonGenericTypeAsParameterizedType() throws Exception {
        TypeDescription.Generic.Builder.parameterizedType(Object.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingOwnerType() throws Exception {
        TypeDescription.Generic.Builder.rawType(Bar.Inner.class, TypeDescription.Generic.UNDEFINED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleType() throws Exception {
        TypeDescription.Generic.Builder.rawType(Bar.Inner.class, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatibleOwnerTypeWhenNonRequired() throws Exception {
        TypeDescription.Generic.Builder.rawType(Object.class, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
    }

    @Test
    public void testExplicitOwnerTypeOfNonGenericType() throws Exception {
        TypeDescription.Generic ownerType = TypeDescription.Generic.Builder.rawType(Bar.class).build();
        TypeDescription.Generic typeDescription = TypeDescription.Generic.Builder.rawType(Bar.Inner.class, ownerType).build();
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.represents(Bar.Inner.class), is(true));
        assertThat(typeDescription.getOwnerType(), sameInstance(ownerType));
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testTypeAnnotationOwnerType() throws Exception {
        super.testTypeAnnotationOwnerType();
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testTypeAnnotationNonGenericInnerType() throws Exception {
        super.testTypeAnnotationNonGenericInnerType();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcardCannotBeResolved() {
        TypeDescription.Generic.Builder.of(TypeDescription.Generic.Builder.unboundWildcard());
    }

    private TypeDescription.Generic describe(Type type, TypeDescription.Generic.AnnotationReader annotationReader) {
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            return wildcardType.getLowerBounds().length == 0
                    ? builder(wildcardType.getUpperBounds()[0], annotationReader.ofWildcardUpperBoundType(0)).asWildcardUpperBound(annotationReader.asList())
                    : builder(wildcardType.getLowerBounds()[0], annotationReader.ofWildcardLowerBoundType(0)).asWildcardLowerBound(annotationReader.asList());
        } else {
            return builder(type, annotationReader).build();
        }
    }

    private TypeDescription.Generic.Builder builder(Type type, TypeDescription.Generic.AnnotationReader annotationReader) {
        return TypeDescription.Generic.Builder.of(TypeDefinition.Sort.describe(type, annotationReader));
    }

    @SuppressWarnings("unused")
    private static class Foo<T> {

        private class Inner<S> {
            /* empty */
        }

        private static class Nested<S> {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    private class Bar {

        private class Inner {
            /* empty */
        }
    }
}
