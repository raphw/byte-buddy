package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
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
        assertThat(genericTypeDescription.getSourceCodeName(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription, is(GenericTypeDescription.Sort.describe(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription, CoreMatchers.not(GenericTypeDescription.Sort.describe(GenericArrayType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().getOnly().asErasure().represents(String.class), is(true));
        assertThat(genericTypeDescription.getTypeName(), is(SimpleParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
    }

    @Test
    public void testParameterizedTypeIterator() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(SimpleParameterizedType.class.getDeclaredField(FOO));
        Iterator<GenericTypeDescription> iterator = genericTypeDescription.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(genericTypeDescription));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoComponentType() throws Exception {
        describe(SimpleParameterizedType.class.getDeclaredField(FOO)).getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoVariableSource() throws Exception {
        describe(SimpleParameterizedType.class.getDeclaredField(FOO)).getVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoSymbol() throws Exception {
        describe(SimpleParameterizedType.class.getDeclaredField(FOO)).getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoUpperBounds() throws Exception {
        describe(SimpleParameterizedType.class.getDeclaredField(FOO)).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterizedTypeNoLowerBounds() throws Exception {
        describe(SimpleParameterizedType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test
    public void testUpperBoundWildcardParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getSourceCodeName(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription,
                is(GenericTypeDescription.Sort.describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription,
                CoreMatchers.not(GenericTypeDescription.Sort.describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.WILDCARD));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().asErasure().represents(String.class), is(true));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getTypeName(), is(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoComponentType() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoSymbol() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoErasure() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoStackSize() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoSuperType() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSuperType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoFields() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testUpperBoundsWildcardParameterizedTypeNoIterator() throws Exception {
        describe(UpperBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().iterator();
    }

    @Test
    public void testLowerBoundWildcardParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getSourceCodeName(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription,
                is(GenericTypeDescription.Sort.describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription,
                CoreMatchers.not(GenericTypeDescription.Sort.describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.WILDCARD));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().getOnly().asErasure().represents(String.class), is(true));
        assertThat(genericTypeDescription.getTypeName(), is(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoComponentType() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoSymbol() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoErasure() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoStackSize() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoSuperType() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSuperType();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoFields() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testLowerBoundWildcardParameterizedTypeNoIterator() throws Exception {
        describe(LowerBoundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().iterator();
    }

    @Test
    public void testUnboundWildcardParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getSourceCodeName(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription,
                is(GenericTypeDescription.Sort.describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription,
                CoreMatchers.not(GenericTypeDescription.Sort.describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.WILDCARD));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().asErasure().represents(Object.class), is(true));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getTypeName(), is(UnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoComponentType() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoSymbol() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoErasure() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoStackSize() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoSuperType() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSuperType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundBoundWildcardParameterizedTypeNoFields() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnboundBoundWildcardParameterizedTypeNoIterator() throws Exception {
        describe(UnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().iterator();
    }

    @Test
    public void testExplicitlyUnboundWildcardParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getSourceCodeName(),
                is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(),
                is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(),
                is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription,
                is(GenericTypeDescription.Sort.describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription,
                CoreMatchers.not(GenericTypeDescription.Sort.describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.WILDCARD));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().getOnly().getUpperBounds().getOnly().asErasure().represents(Object.class), is(true));
        assertThat(genericTypeDescription.getParameters().getOnly().getLowerBounds().size(), is(0));
        assertThat(genericTypeDescription.getTypeName(), is(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoComponentType() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoOwnerType() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoVariableSource() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoSymbol() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoErasure() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().asErasure();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoStackSize() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoSuperType() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getSuperType();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundWildcardParameterizedTypeNoInterfaces() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundBoundWildcardParameterizedTypeNoFields() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundBoundWildcardParameterizedTypeNoMethods() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testExplicitlyUnboundBoundWildcardParameterizedTypeNoIterator() throws Exception {
        describe(ExplicitlyUnboundWildcardParameterizedType.class.getDeclaredField(FOO)).getParameters().getOnly().iterator();
    }

    @Test
    public void testNestedParameterizedType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(NestedParameterizedType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().getOnly().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().getOnly().getParameters().getOnly().asErasure().represents(Foo.class), is(true));
    }

    @Test
    public void testGenericArrayType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(GenericArrayType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.GENERIC_ARRAY));
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getDeclaredFields().size(), is(0));
        assertThat(genericTypeDescription.getDeclaredMethods().size(), is(0));
        assertThat(genericTypeDescription.getParameters().size(), is(0));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getSuperType(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(genericTypeDescription.getInterfaces(), is(TypeDescription.ARRAY_INTERFACES));
        assertThat(genericTypeDescription.getSourceCodeName(), is(GenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(), is(GenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(), is(GenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(GenericArrayType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription, is(GenericTypeDescription.Sort.describe(GenericArrayType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription, CoreMatchers.not(GenericTypeDescription.Sort.describe(GenericArrayType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getComponentType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getComponentType().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getComponentType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getComponentType().getParameters().getOnly().asErasure().represents(String.class), is(true));
        assertThat(genericTypeDescription.getTypeName(), is(GenericArrayType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test
    public void testGenericArrayTypeIterator() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(GenericArrayType.class.getDeclaredField(FOO));
        Iterator<GenericTypeDescription> iterator = genericTypeDescription.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(genericTypeDescription));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoVariableSource() throws Exception {
        describe(GenericArrayType.class.getDeclaredField(FOO)).getVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoSymbol() throws Exception {
        describe(GenericArrayType.class.getDeclaredField(FOO)).getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoUpperBounds() throws Exception {
        describe(GenericArrayType.class.getDeclaredField(FOO)).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayTypeNoLowerBounds() throws Exception {
        describe(GenericArrayType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test
    public void testGenericArrayOfGenericComponentType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.GENERIC_ARRAY));
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getDeclaredFields().size(), is(0));
        assertThat(genericTypeDescription.getDeclaredMethods().size(), is(0));
        assertThat(genericTypeDescription.getParameters().size(), is(0));
        assertThat(genericTypeDescription.getOwnerType(), nullValue(GenericTypeDescription.class));
        assertThat(genericTypeDescription.getSuperType(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(genericTypeDescription.getInterfaces(), is(TypeDescription.ARRAY_INTERFACES));
        assertThat(genericTypeDescription.getSourceCodeName(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription, is(GenericTypeDescription.Sort.describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription, CoreMatchers.not(GenericTypeDescription.Sort.describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getComponentType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getComponentType().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getComponentType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getComponentType().getParameters().getOnly().asErasure().represents(String.class), is(true));
        assertThat(genericTypeDescription.getTypeName(), is(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO).getGenericType().toString()));
    }

    @Test
    public void testGenericArrayOfGenericComponentTypeIterator() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO));
        Iterator<GenericTypeDescription> iterator = genericTypeDescription.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(genericTypeDescription));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoVariableSource() throws Exception {
        describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getVariableSource();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoSymbol() throws Exception {
        describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getSymbol();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoUpperBounds() throws Exception {
        describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getUpperBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testGenericArrayOfGenericComponentTypeNoLowerBounds() throws Exception {
        describe(GenericArrayOfGenericComponentType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test
    public void testTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(SimpleTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSourceCodeName(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getTypeName(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.toString(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.hashCode(),
                is(GenericTypeDescription.Sort.describe(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType()).hashCode()));
        assertThat(genericTypeDescription, is(GenericTypeDescription.Sort.describe(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType())));
        assertThat(genericTypeDescription,
                CoreMatchers.not(GenericTypeDescription.Sort.describe(SimpleTypeVariableType.class.getDeclaredField(FOO).getType())));
        assertThat(genericTypeDescription, CoreMatchers.not(new Object()));
        assertThat(genericTypeDescription.equals(null), is(false));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(genericTypeDescription.getUpperBounds().getOnly().getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getTypeName(), is(SimpleTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(SimpleTypeVariableType.class)));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoLowerBounds() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).getLowerBounds();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoComponentType() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).getComponentType();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableNoOwnerType() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).getOwnerType();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoSuperType() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).getSuperType();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoInterfaceTypes() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).getInterfaces();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoFields() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoMethods() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariableTypeNoIterator() throws Exception {
        describe(SimpleTypeVariableType.class.getDeclaredField(FOO)).iterator();
    }

    @Test
    public void testSingleUpperBoundTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(SingleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.STRING));
        assertThat(genericTypeDescription.getUpperBounds().getOnly().getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getTypeName(), is(SingleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(SingleUpperBoundTypeVariableType.class)));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testMultipleUpperBoundTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(3));
        assertThat(genericTypeDescription.getUpperBounds().get(0), is((GenericTypeDescription) TypeDescription.STRING));
        assertThat(genericTypeDescription.getUpperBounds().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(genericTypeDescription.getUpperBounds().get(2), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(genericTypeDescription.getTypeName(), is(MultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(MultipleUpperBoundTypeVariableType.class)));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testInterfaceOnlyMultipleUpperBoundTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(InterfaceOnlyMultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(2));
        assertThat(genericTypeDescription.getUpperBounds().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(genericTypeDescription.getUpperBounds().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(genericTypeDescription.getTypeName(), is(InterfaceOnlyMultipleUpperBoundTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getVariableSource(), is((TypeVariableSource) new TypeDescription.ForLoadedType(InterfaceOnlyMultipleUpperBoundTypeVariableType.class)));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testShadowedTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(ShadowingTypeVariableType.class.getDeclaredMethod(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(genericTypeDescription.getSymbol(), is(T));
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getUpperBounds().size(), is(1));
        assertThat(genericTypeDescription.getUpperBounds().getOnly(), is((GenericTypeDescription) TypeDescription.OBJECT));
        assertThat(genericTypeDescription.getTypeName(), is(ShadowingTypeVariableType.class.getDeclaredMethod(FOO).getGenericReturnType().toString()));
        assertThat(genericTypeDescription.getVariableSource(), is((TypeVariableSource) new MethodDescription.ForLoadedMethod(ShadowingTypeVariableType.class.getDeclaredMethod(FOO))));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().size(), is(1));
        assertThat(genericTypeDescription.getVariableSource().getTypeVariables().getOnly(), is(genericTypeDescription));
    }

    @Test
    public void testNestedTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(NestedTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getTypeName(), is(NestedTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
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
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getParameters().size(), is(0));
        Type ownerType = ((ParameterizedType) NestedSpecifiedTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(genericTypeDescription.getOwnerType(), is(GenericTypeDescription.Sort.describe(ownerType)));
        assertThat(genericTypeDescription.getOwnerType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getOwnerType().getParameters().size(), is(1));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getOwnerType().getParameters().getOnly(), is((GenericTypeDescription) TypeDescription.STRING));
    }

    @Test
    public void testNestedStaticTypeVariableType() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(NestedStaticTypeVariableType.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getTypeName(), is(NestedStaticTypeVariableType.class.getDeclaredField(FOO).getGenericType().toString()));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getStackSize(), is(StackSize.SINGLE));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getParameters().getOnly(), is((GenericTypeDescription) TypeDescription.STRING));
        Type ownerType = ((ParameterizedType) NestedStaticTypeVariableType.class.getDeclaredField(FOO).getGenericType()).getOwnerType();
        assertThat(genericTypeDescription.getOwnerType(), is(GenericTypeDescription.Sort.describe(ownerType)));
        assertThat(genericTypeDescription.getOwnerType().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
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
        assertThat(qux.getUpperBounds().getOnly(), is(bar));
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
        assertThat(upperBound.asErasure(), is((GenericTypeDescription) genericTypeDescription.asErasure()));
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

    @Test
    public void testParameterizedTypeSuperTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        GenericTypeDescription superType = genericTypeDescription.getSuperType();
        assertThat(superType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
        assertThat(superType.getParameters().size(), is(2));
        assertThat(superType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(superType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(superType.getDeclaredFields().size(), is(1));
        assertThat(superType.getDeclaredFields().getOnly().getDeclaringType(), is(superType));
        GenericTypeDescription fieldType = superType.getDeclaredFields().getOnly().getType();
        assertThat(fieldType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(fieldType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(fieldType.getParameters().size(), is(2));
        assertThat(fieldType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(fieldType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(superType.getDeclaredMethods().filter(isConstructor()).size(), is(1));
        assertThat(superType.getDeclaredMethods().filter(isMethod()).size(), is(1));
        assertThat(superType.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType(), is((superType)));
        assertThat(superType.getDeclaredMethods().filter(isConstructor()).getOnly().getDeclaringType(), is((superType)));
        GenericTypeDescription methodReturnType = superType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodReturnType.getParameters().size(), is(2));
        assertThat(methodReturnType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodReturnType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        GenericTypeDescription methodParameterType = superType.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodParameterType.getParameters().size(), is(2));
        assertThat(methodParameterType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodParameterType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
    }

    @Test
    public void testParameterizedTypeInterfaceResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        assertThat(genericTypeDescription.getInterfaces().size(), is(1));
        GenericTypeDescription interfaceType = genericTypeDescription.getInterfaces().getOnly();
        assertThat(interfaceType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(interfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
        assertThat(interfaceType.getParameters().size(), is(2));
        assertThat(interfaceType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(interfaceType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(interfaceType.getDeclaredFields().size(), is(0));
        assertThat(interfaceType.getDeclaredMethods().filter(isConstructor()).size(), is(0));
        assertThat(interfaceType.getDeclaredMethods().filter(isMethod()).size(), is(1));
        assertThat(interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType(), is((interfaceType)));
        GenericTypeDescription methodReturnType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodReturnType.getParameters().size(), is(2));
        assertThat(methodReturnType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodReturnType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        GenericTypeDescription methodParameterType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodParameterType.getParameters().size(), is(2));
        assertThat(methodParameterType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(methodParameterType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
    }

    @Test
    public void testParameterizedTypeRawSuperTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(BAR));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        GenericTypeDescription superType = genericTypeDescription.getSuperType();
        assertThat(superType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
        assertThat(superType.getParameters().size(), is(0));
        assertThat(superType.getDeclaredFields().size(), is(1));
        assertThat(superType.getDeclaredFields().getOnly().getDeclaringType().getDeclaredFields().getOnly().getType(),
                is(superType.getDeclaredFields().getOnly().getType()));
        GenericTypeDescription fieldType = superType.getDeclaredFields().getOnly().getType();
        assertThat(fieldType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(fieldType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(fieldType.getParameters().size(), is(0));
        GenericTypeDescription methodReturnType = superType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodReturnType.getParameters().size(), is(0));
        GenericTypeDescription methodParameterType = superType.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodParameterType.getParameters().size(), is(0));
        assertThat(superType.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType().getDeclaredMethods().filter(isMethod()).getOnly().getReturnType(),
                is(superType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType()));
        assertThat(superType.getDeclaredMethods().filter(isMethod()).getOnly().getDeclaringType().getDeclaredMethods().filter(isMethod()).getOnly().getParameters().getOnly().getType(),
                is(superType.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().getOnly().getType()));
    }

    @Test
    public void testParameterizedTypeRawInterfaceTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(BAR));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        GenericTypeDescription interfaceType = genericTypeDescription.getInterfaces().getOnly();
        assertThat(interfaceType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(interfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
        assertThat(interfaceType.getParameters().size(), is(0));
        assertThat(interfaceType.getDeclaredFields().size(), is(0));
        GenericTypeDescription methodReturnType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType();
        assertThat(methodReturnType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(methodReturnType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodReturnType.getParameters().size(), is(0));
        GenericTypeDescription methodParameterType = interfaceType.getDeclaredMethods().filter(isMethod()).getOnly().getParameters().asTypeList().getOnly();
        assertThat(methodParameterType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(methodParameterType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(methodParameterType.getParameters().size(), is(0));
        assertThat(interfaceType.getDeclaredMethods().getOnly().getDeclaringType().getDeclaredMethods().getOnly().getReturnType(),
                is(interfaceType.getDeclaredMethods().getOnly().getReturnType()));
        assertThat(interfaceType.getDeclaredMethods().getOnly().getDeclaringType().getDeclaredMethods().getOnly().getParameters().getOnly().getType(),
                is(interfaceType.getDeclaredMethods().getOnly().getParameters().getOnly().getType()));
    }

    @Test
    public void testParameterizedTypePartiallyRawSuperTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(QUX));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        GenericTypeDescription superType = genericTypeDescription.getSuperType();
        assertThat(superType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Intermediate.class)));
        GenericTypeDescription superSuperType = superType.getSuperType();
        assertThat(superSuperType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superSuperType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
        assertThat(superSuperType.getParameters().size(), is(2));
        assertThat(superSuperType.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superSuperType.getParameters().get(0).asErasure().represents(List.class), is(true));
        assertThat(superSuperType.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superSuperType.getParameters().get(1).asErasure().represents(List.class), is(true));
    }

    @Test
    public void testParameterizedTypePartiallyRawInterfaceTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(QUX));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        GenericTypeDescription superType = genericTypeDescription.getSuperType();
        assertThat(superType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Intermediate.class)));
        GenericTypeDescription superInterfaceType = superType.getInterfaces().getOnly();
        assertThat(superInterfaceType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superInterfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
        assertThat(superInterfaceType.getParameters().size(), is(2));
        assertThat(superInterfaceType.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superInterfaceType.getParameters().get(0).asErasure().represents(List.class), is(true));
        assertThat(superInterfaceType.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superInterfaceType.getParameters().get(1).asErasure().represents(List.class), is(true));
    }

    @Test
    public void testParameterizedTypeNestedPartiallyRawSuperTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(BAZ));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        GenericTypeDescription superType = genericTypeDescription.getSuperType();
        assertThat(superType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.NestedIntermediate.class)));
        GenericTypeDescription superSuperType = superType.getSuperType();
        assertThat(superSuperType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superSuperType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.Base.class)));
        assertThat(superSuperType.getParameters().size(), is(2));
        assertThat(superSuperType.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superSuperType.getParameters().get(0).asErasure().represents(List.class), is(true));
        assertThat(superSuperType.getParameters().get(0).getParameters().size(), is(1));
        assertThat(superSuperType.getParameters().get(0).getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superSuperType.getParameters().get(0).getParameters().getOnly().asErasure().represents(List.class), is(true));
        assertThat(superSuperType.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superSuperType.getParameters().get(1).asErasure().represents(List.class), is(true));
        assertThat(superSuperType.getParameters().get(1).getParameters().size(), is(1));
        assertThat(superSuperType.getParameters().get(1).getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superSuperType.getParameters().get(1).getParameters().getOnly().asErasure().represents(String.class), is(true));
    }

    @Test
    public void testParameterizedTypeNestedPartiallyRawInterfaceTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(BAZ));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(1));
        GenericTypeDescription superType = genericTypeDescription.getSuperType();
        assertThat(superType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.NestedIntermediate.class)));
        GenericTypeDescription superInterfaceType = superType.getInterfaces().getOnly();
        assertThat(superInterfaceType.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superInterfaceType.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(TypeResolution.BaseInterface.class)));
        assertThat(superInterfaceType.getParameters().size(), is(2));
        assertThat(superInterfaceType.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superInterfaceType.getParameters().get(0).asErasure().represents(List.class), is(true));
        assertThat(superInterfaceType.getParameters().get(0).getParameters().size(), is(1));
        assertThat(superInterfaceType.getParameters().get(0).getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superInterfaceType.getParameters().get(0).getParameters().getOnly().asErasure().represents(List.class), is(true));
        assertThat(superInterfaceType.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(superInterfaceType.getParameters().get(1).asErasure().represents(List.class), is(true));
        assertThat(superInterfaceType.getParameters().get(1).getParameters().size(), is(1));
        assertThat(superInterfaceType.getParameters().get(1).getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superInterfaceType.getParameters().get(1).getParameters().getOnly().asErasure().represents(String.class), is(true));
    }

    @Test
    public void testShadowedTypeSuperTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(FOO + BAR));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(2));
        GenericTypeDescription superType = genericTypeDescription.getSuperType();
        assertThat(superType.getParameters().size(), is(2));
        assertThat(superType.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(superType.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(superType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
    }

    @Test
    public void testShadowedTypeInterfaceTypeResolution() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(TypeResolution.class.getDeclaredField(FOO + BAR));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(2));
        GenericTypeDescription interfaceType = genericTypeDescription.getInterfaces().getOnly();
        assertThat(interfaceType.getParameters().size(), is(2));
        assertThat(interfaceType.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(interfaceType.getParameters().get(0), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(interfaceType.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(interfaceType.getParameters().get(1), is((GenericTypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
    }

    @Test
    public void testMethodTypeVariableIsRetained() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(2));
        assertThat(genericTypeDescription.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().get(0).asErasure().represents(Number.class), is(true));
        assertThat(genericTypeDescription.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().get(1).asErasure().represents(Integer.class), is(true));
        MethodDescription methodDescription = genericTypeDescription.getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("S"));
        assertThat(methodDescription.getReturnType().getVariableSource(), is((TypeVariableSource) methodDescription));
    }

    @Test
    public void testShadowedMethodTypeVariableIsRetained() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(2));
        assertThat(genericTypeDescription.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().get(0).asErasure().represents(Number.class), is(true));
        assertThat(genericTypeDescription.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().get(1).asErasure().represents(Integer.class), is(true));
        MethodDescription methodDescription = genericTypeDescription.getDeclaredMethods().filter(named(BAR)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("T"));
        assertThat(methodDescription.getReturnType().getVariableSource(), is((TypeVariableSource) methodDescription));
    }

    @Test
    public void testMethodTypeVariableWithExtensionIsRetained() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(genericTypeDescription.getParameters().size(), is(2));
        assertThat(genericTypeDescription.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().get(0).asErasure().represents(Number.class), is(true));
        assertThat(genericTypeDescription.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(genericTypeDescription.getParameters().get(1).asErasure().represents(Integer.class), is(true));
        MethodDescription methodDescription = genericTypeDescription.getDeclaredMethods().filter(named(QUX)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("S"));
        assertThat(methodDescription.getReturnType().getVariableSource(), is((TypeVariableSource) methodDescription));
        assertThat(methodDescription.getReturnType().getUpperBounds().size(), is(1));
        assertThat(methodDescription.getReturnType().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(methodDescription.getReturnType().getUpperBounds().getOnly().asErasure().represents(Number.class), is(true));
    }

    @Test
    public void testMethodTypeVariableErasedBound() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(BAR)).getSuperType();
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        MethodDescription methodDescription = genericTypeDescription.getDeclaredMethods().filter(named(FOO)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("S"));
        assertThat(methodDescription.getReturnType().getVariableSource(), is((TypeVariableSource) methodDescription));
    }

    @Test
    public void testMethodTypeVariableWithExtensionErasedBound() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(BAR)).getSuperType();
        assertThat(genericTypeDescription.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        MethodDescription methodDescription = genericTypeDescription.getDeclaredMethods().filter(named(QUX)).getOnly();
        assertThat(methodDescription.getReturnType().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(methodDescription.getReturnType().getSymbol(), is("S"));
        assertThat(methodDescription.getReturnType().getVariableSource(), is((TypeVariableSource) methodDescription));
        assertThat(methodDescription.getReturnType().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(methodDescription.getReturnType().getUpperBounds().getOnly().asErasure().represents(Object.class), is(true));
    }

    @Test
    public void testGenericFieldHashCode() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getDeclaredFields().filter(named(FOO)).getOnly().hashCode(),
                CoreMatchers.not(new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO)).hashCode()));
        assertThat(genericTypeDescription.getDeclaredFields().filter(named(FOO)).getOnly().asDefined().hashCode(),
                is(new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO)).hashCode()));
    }

    @Test
    public void testGenericFieldEquality() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getDeclaredFields().filter(named(FOO)).getOnly(),
                CoreMatchers.not((FieldDescription) new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO))));
        assertThat(genericTypeDescription.getDeclaredFields().filter(named(FOO)).getOnly().asDefined(),
                is((FieldDescription) new FieldDescription.ForLoadedField(MemberVariable.class.getDeclaredField(FOO))));
    }

    @Test
    public void testGenericMethodHashCode() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().hashCode(),
                CoreMatchers.not(new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO)).hashCode()));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().asDefined().hashCode(),
                is(new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO)).hashCode()));
    }

    @Test
    public void testGenericMethodEquality() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(FOO)).getOnly(),
                CoreMatchers.not((MethodDescription) new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO))));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(FOO)).getOnly().asDefined(),
                is((MethodDescription) new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(FOO))));
    }

    @Test
    public void testGenericParameterHashCode() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly().hashCode(), CoreMatchers.not(
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly().hashCode()));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly().asDefined().hashCode(), is(
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly().hashCode()));
    }

    @Test
    public void testGenericParameterEquality() throws Exception {
        GenericTypeDescription genericTypeDescription = describe(MemberVariable.class.getDeclaredField(FOO));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly(), CoreMatchers.not((ParameterDescription)
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly()));
        assertThat(genericTypeDescription.getDeclaredMethods().filter(named(BAZ)).getOnly().getParameters().getOnly().asDefined(), is((ParameterDescription)
                new MethodDescription.ForLoadedMethod(MemberVariable.class.getDeclaredMethod(BAZ, Object.class)).getParameters().getOnly()));
    }

    @SuppressWarnings("unused")
    public interface Foo {
        /* empty */
    }

    @SuppressWarnings("unused")
    public interface Bar {
        /* empty */
    }

    @SuppressWarnings("unused")
    public interface Qux<T, U> {
        /* empty */
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
    public static class UnboundWildcardParameterizedType {

        List<?> foo;
    }

    @SuppressWarnings("all")
    public static class ExplicitlyUnboundWildcardParameterizedType {

        List<? extends Object> foo;
    }

    @SuppressWarnings("unused")
    public static class NestedParameterizedType {

        List<List<Foo>> foo;
    }

    @SuppressWarnings("unused")
    public static class GenericArrayType {

        List<String>[] foo;
    }

    public static class GenericArrayOfGenericComponentType<T extends String> {

        List<T>[] foo;
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

        Placeholder foo;

        class Placeholder {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class NestedSpecifiedTypeVariableType<T> {

        NestedSpecifiedTypeVariableType<String>.Placeholder foo;

        class Placeholder {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class NestedStaticTypeVariableType<T> {

        NestedStaticTypeVariableType.Placeholder<String> foo;

        static class Placeholder<S> {
            /* empty */
        }
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
    public static class TypeResolution<T> {

        private TypeResolution<Foo>.Inner<Bar> foo;

        private TypeResolution<Foo>.Raw<Bar> bar;

        private TypeResolution<Foo>.PartiallyRaw<Bar> qux;

        private TypeResolution<Foo>.NestedPartiallyRaw<Bar> baz;

        private TypeResolution<Foo>.Shadowed<Bar, Foo> foobar;

        public interface BaseInterface<V, W> {

            Qux<V, W> qux(Qux<V, W> qux);
        }

        public static class Intermediate<V, W> extends Base<List<V>, List<? extends W>> implements BaseInterface<List<V>, List<? extends W>> {
            /* empty */
        }

        public static class NestedIntermediate<V, W> extends Base<List<List<V>>, List<String>> implements BaseInterface<List<List<V>>, List<String>> {
            /* empty */
        }

        public static class Base<V, W> {

            Qux<V, W> qux;

            public Qux<V, W> qux(Qux<V, W> qux) {
                return null;
            }
        }

        public class Inner<S> extends Base<T, S> implements BaseInterface<T, S> {
            /* empty */
        }

        @SuppressWarnings("unchecked")
        public class Raw<S> extends Base implements BaseInterface {
            /* empty */
        }

        @SuppressWarnings("unchecked")
        public class PartiallyRaw<S> extends Intermediate {
            /* empty */
        }

        @SuppressWarnings("unchecked")
        public class NestedPartiallyRaw<S> extends NestedIntermediate {
            /* empty */
        }

        @SuppressWarnings("all")
        public class Shadowed<T, S> extends Base<T, S> implements BaseInterface<T, S> {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class MemberVariable<U, T extends U> {

        public MemberVariable<Number, Integer> foo;

        public Raw bar;

        public <S> S foo() {
            return null;
        }

        @SuppressWarnings("all")
        public <T> T bar() {
            return null;
        }

        @SuppressWarnings("all")
        public <S extends U> S qux() {
            return null;
        }

        public U baz(U u) {
            return u;
        }

        @SuppressWarnings("unchecked")
        public static class Raw extends MemberVariable {
            /* empty */
        }
    }
}
