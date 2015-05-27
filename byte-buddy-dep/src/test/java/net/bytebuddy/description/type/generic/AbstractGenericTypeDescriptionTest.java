package net.bytebuddy.description.type.generic;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractGenericTypeDescriptionTest {

    private static final String FOO = "foo";

    protected abstract GenericTypeDescription describe(Field field);

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

    public static class SimpleParameterizedType {
        List<String> foo;
    }

    public static class UpperBoundWildcardParameterizedType {
        List<? extends String> foo;
    }

    public static class LowerBoundWildcardParameterizedType {
        List<? super String> foo;
    }
}
