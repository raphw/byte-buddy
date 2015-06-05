package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractGenericTypeDescriptionTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final String T = "T", S = "S", U = "U", V = "V";

    protected abstract GenericTypeDescription describe(Field field);

    protected abstract GenericTypeDescription describe(Method method);

    @Test
    public void testSimpleParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(SimpleParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        assertThat(genericTypeDescription.getParameters().getOnly().asRawType().represents(String.class), is(true));
        assertThat(genericTypeDescription.getTypeName(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(), nullValue(TypeVariableSource.class));
        assertThat(genericTypeDescription.getSymbol(), nullValue(String.class));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(0));
    }

    @Test
    public void testUpperBoundWildcardParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.WILDCARD));
        try {
            genericTypeDescription.getParameters().getOnly().asRawType();
            fail();
        } catch (IllegalStateException ignored) {
            /* expected */
        }
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().asRawType().represents(String.class), is(true));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getTypeName(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(), nullValue(TypeVariableSource.class));
        assertThat(genericTypeDescription.getSymbol(), nullValue(String.class));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(0));
    }

    @Test
    public void testLowerBoundWildcardParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.WILDCARD));
        try {
            genericTypeDescription.getParameters().getOnly().asRawType();
            fail();
        } catch (IllegalStateException ignored) {
            /* expected */
        }
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        try {
            genericTypeDescription.getParameters().getOnly().asRawType();
            fail();
        } catch (IllegalStateException ignored) {
            /* expected */
        }
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().getOnly().asRawType().represents(String.class), is(true));
        assertThat(genericTypeDescription.getTypeName(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(), nullValue(TypeVariableSource.class));
        assertThat(genericTypeDescription.getSymbol(), nullValue(String.class));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(0));
    }

    @Test
    public void testGenericArrayType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(GenericArrayType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.GENERIC_ARRAY));
        assertThat(genericTypeDescription.getComponentType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getComponentType().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getComponentType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        assertThat(genericTypeDescription.getComponentType().getParameters().getOnly().asRawType().represents(String.class), is(true));
        assertThat(genericTypeDescription.getTypeName(), is(GenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(), nullValue(TypeVariableSource.class));
        assertThat(genericTypeDescription.getSymbol(), nullValue(String.class));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(0));
    }

    @Test
    public void testTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(SimpleTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(genericTypeDescription.getTypeName(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(SimpleTypeVariableType.class)));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
    }

    @Test
    public void testSingleUpperBoundTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(SingleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.STRING));
        assertThat(genericTypeDescription.getTypeName(), is(SingleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(),
                is((TypeVariableSource) new TypeDescription.ForLoadedType(SingleUpperBoundTypeVariableType.class)));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testMultipleUpperBoundTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(3));
        assertThat(genericTypeDescription.getUpperBounds().get(0), is((GenericTypeDescription) TypeDescription.STRING));
        assertThat(genericTypeDescription.getUpperBounds().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(genericTypeDescription.getUpperBounds().get(2), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(genericTypeDescription.getTypeName(), is(MultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(),
                is((TypeVariableSource) new TypeDescription.ForLoadedType(MultipleUpperBoundTypeVariableType.class)));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testInterfaceOnlyMultipleUpperBoundTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(InterfaceOnlyMultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(2));
        assertThat(genericTypeDescription.getUpperBounds().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(genericTypeDescription.getUpperBounds().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(genericTypeDescription.getTypeName(),
                is(InterfaceOnlyMultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(),
                is((TypeVariableSource) new TypeDescription.ForLoadedType(InterfaceOnlyMultipleUpperBoundTypeVariableType.class)));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testShadowedTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(ShadowingTypeVariableType.class.getDeclaredMethod(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(genericTypeDescription.getTypeName(), is(ShadowingTypeVariableType.class.getDeclaredMethod(FOO).getGenericReturnType().toString()));
        assertThat(genericTypeDescription.getComponentType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getVariableSource(),
                is((TypeVariableSource) new MethodDescription.ForLoadedMethod(ShadowingTypeVariableType.class.getDeclaredMethod(FOO))));
        assertThat(genericTypeDescription.getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testNestedTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(NestedTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getTypeName(), is(NestedTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(0));
        Type ownerType = ((ParameterizedType) NestedTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(genericTypeDescription.getOwnerType(), is(GenericTypeDescription.Sort.describe(ownerType)));
        assertThat(genericTypeDescription.getOwnerType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getOwnerType().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly().getSymbol(), is(T));
    }

    @Test
    public void testNestedSpecifiedTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(NestedSpecifiedTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getTypeName(), is(NestedSpecifiedTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(0));
        Type ownerType = ((ParameterizedType) NestedSpecifiedTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(genericTypeDescription.getOwnerType(), is(GenericTypeDescription.Sort.describe(ownerType)));
        assertThat(genericTypeDescription.getOwnerType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getOwnerType().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly(), is((GenericTypeDescription) TypeDescription.STRING));
    }

    @Test
    public void testNestedStaticTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(NestedStaticTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getTypeName(), is(NestedStaticTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly(), is((GenericTypeDescription) TypeDescription.STRING));
        Type ownerType = ((ParameterizedType) NestedStaticTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(genericTypeDescription.getOwnerType(), is(GenericTypeDescription.Sort.describe(ownerType)));
        assertThat(genericTypeDescription.getOwnerType().getSort(), is(GenericTypeDescription.Sort.RAW));
    }

    @Test
    public void testNestedInnerType() throws Exception {
        GenericTypeDescription foo = describe(NestedInnerType.InnerType.class.getDeclaredMethod(FOO));
        assertThat(foo.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(foo.getSymbol(), is(T));
        assertThat(foo.getUpperBounds().size(), is(1));
        assertThat(foo.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(foo.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(NestedInnerType.class)));
        GenericTypeDescription bar = describe(NestedInnerType.InnerType.class.getDeclaredMethod(BAR));
        assertThat(bar.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(bar.getSymbol(), is(S));
        assertThat(bar.getUpperBounds().size(), is(1));
        assertThat(bar.getUpperBounds().getOnly(), is(foo));
        assertThat(bar.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(NestedInnerType.InnerType.class)));
        GenericTypeDescription qux = describe(NestedInnerType.InnerType.class.getDeclaredMethod(QUX));
        assertThat(qux.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(qux.getSymbol(), is(U));
        assertThat(qux.getUpperBounds().size(), is(1));
        assertThat(qux.getUpperBounds().getOnly(), is(foo));
        MethodDescription quxMethod = new MethodDescription.ForLoadedMethod(NestedInnerType.InnerType.class.getDeclaredMethod(QUX));
        assertThat(qux.getVariableSource(), is((TypeVariableSource) quxMethod));
    }

    @Test
    public void testNestedInnerMethod() throws Exception {
        Class<?> innerType = new NestedInnerMethod().foo();
        GenericTypeDescription foo = describe(innerType.getDeclaredMethod(FOO));
        assertThat(foo.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(foo.getSymbol(), is(T));
        assertThat(foo.getUpperBounds().size(), is(1));
        assertThat(foo.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(foo.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(NestedInnerMethod.class)));
        GenericTypeDescription bar = describe(innerType.getDeclaredMethod(BAR));
        assertThat(bar.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(bar.getSymbol(), is(S));
        assertThat(bar.getUpperBounds().size(), is(1));
        assertThat(bar.getUpperBounds().getOnly(), is(foo));
        assertThat(bar.getVariableSource(), is((TypeVariableSource) new MethodDescription.ForLoadedMethod(NestedInnerMethod.class.getDeclaredMethod(FOO))));
        GenericTypeDescription qux = describe(innerType.getDeclaredMethod(QUX));
        assertThat(qux.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(qux.getSymbol(), is(U));
        assertThat(qux.getUpperBounds().size(), is(1));
        assertThat(qux.getUpperBounds().getOnly(), is(bar));
        assertThat(qux.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(innerType)));
        GenericTypeDescription baz = describe(innerType.getDeclaredMethod(BAZ));
        assertThat(baz.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(baz.getSymbol(), is(V));
        assertThat(baz.getUpperBounds().size(), is(1));
        assertThat(baz.getUpperBounds().getOnly(), is(qux));
        assertThat(baz.getVariableSource(), is((TypeVariableSource) new MethodDescription.ForLoadedMethod(innerType.getDeclaredMethod(BAZ))));

    }

    @Test
    public void testRecursiveTypeVariable() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(RecursiveTypeVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(1));
        GenericTypeDescription upperBound = genericTypeDescription.getUpperBounds().getOnly();
        assertThat(upperBound.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(upperBound.asRawType(), is((GenericTypeDescription) genericTypeDescription.asRawType()));
        assertThat(upperBound.getParameters().size(), is(1));
        assertThat(upperBound.getParameters().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testBackwardsReferenceTypeVariable() throws Exception {
        GenericTypeDescription foo = describe(BackwardsReferenceTypeVariable.class.getDeclaredField(FOO));
        assertThat(foo.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(foo.getSymbol(), is(S));
        assertThat(foo.getUpperBounds().size(), is(1));
        TypeDescription backwardsReference = new TypeDescription.ForLoadedType(BackwardsReferenceTypeVariable.class);
        assertThat(foo.getUpperBounds().getOnly(), is(backwardsReference.getTypeVariables().filter(named(T)).getOnly()));
        GenericTypeDescription bar = describe(BackwardsReferenceTypeVariable.class.getDeclaredField(BAR));
        assertThat(bar.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(bar.getSymbol(), is(T));
        assertThat(bar.getUpperBounds().size(), is(1));
        assertThat(bar.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.OBJECT));
    }

    @SuppressWarnings("unused")
    public static class SimpleParameterizedType {

        List<String> foo;
    }

    @SuppressWarnings("unused")
    public static class UpperBoundWildcardParameterizedType {

        List<? extends String> foo;
    }

    @SuppressWarnings("unused")
    public static class LowerBoundWildcardParameterizedType {

        List<? super String> foo;
    }

    @SuppressWarnings("unused")
    public static class GenericArrayType {

        List<String>[] foo;
    }

    @SuppressWarnings("unused")
    public static class SimpleTypeVariableType<T> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class SingleUpperBoundTypeVariableType<T extends String> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class MultipleUpperBoundTypeVariableType<T extends String & Foo & Bar> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class InterfaceOnlyMultipleUpperBoundTypeVariableType<T extends Foo & Bar> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class ShadowingTypeVariableType<T> {

        @SuppressWarnings("all")
        <T> T foo() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class NestedTypeVariableType<T> {

        class Placeholder {
            /* empty */
        }

        Placeholder foo;
    }

    @SuppressWarnings("unused")
    public static class NestedSpecifiedTypeVariableType<T> {

        class Placeholder {
            /* empty */
        }

        NestedSpecifiedTypeVariableType<String>.Placeholder foo;
    }

    @SuppressWarnings("unused")
    public static class NestedStaticTypeVariableType<T> {

        static class Placeholder<S> {
            /* empty */
        }

        NestedStaticTypeVariableType.Placeholder<String> foo;
    }

    @SuppressWarnings("unused")
    public static class NestedInnerType<T> {

        class InnerType<S extends T> {

            <U extends S> T foo() {
                return null;
            }

            <U extends S> S bar() {
                return null;
            }

            <U extends S> U qux() {
                return null;
            }
        }
    }

    @SuppressWarnings("unused")
    public static class NestedInnerMethod<T> {

        <S extends T> Class<?> foo() {
            class InnerType<U extends S> {

                <V extends U> T foo() {
                    return null;
                }

                <V extends U> S bar() {
                    return null;
                }

                <V extends U> U qux() {
                    return null;
                }

                <V extends U> V baz() {
                    return null;
                }
            }
            return InnerType.class;
        }
    }

    @SuppressWarnings("unused")
    public static class RecursiveTypeVariable<T extends RecursiveTypeVariable<T>> {

        T foo;
    }

    @SuppressWarnings("unused")
    public static class BackwardsReferenceTypeVariable<T, S extends T> {

        S foo;

        T bar;
    }

    @SuppressWarnings("unused")
    public interface Foo {
        /* empty */
    }

    @SuppressWarnings("unused")
    public interface Bar {
        /* empty */
    }
}
