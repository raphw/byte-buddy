package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractGenericTypeDescriptionTest {

    private static final String FOO = "foo";

    private static final String T = "T";

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
        assertThat(genericTypeDescription.getParameters().getOnly().asRawType().represents(String.class), is(true));
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
        assertThat(genericTypeDescription.getParameters().getOnly().asRawType().represents(Object.class), is(true));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().asRawType().represents(Object.class), is(true));
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
        assertThat(genericTypeDescription.getOwnerType().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.RAW));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly(), is((GenericTypeDescription) TypeDescription.STRING));
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
    public interface Foo {
        /* empty */
    }

    @SuppressWarnings("unused")
    public interface Bar {
        /* empty */
    }
}
