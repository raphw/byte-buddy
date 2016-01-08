package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorValidatorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic typeDescription;

    @Test
    public void testWildcardNotValidated() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.TYPE_VARIABLE.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onWildcard(typeDescription), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.EXCEPTION.onWildcard(typeDescription), is(false));
    }

    @Test
    public void testExceptionType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.EXCEPTION.onNonGenericType(TypeDefinition.Sort.describe(Exception.class)), is(true));
        TypeDescription.Generic typeVariable = mock(TypeDescription.Generic.class), bound = mock(TypeDescription.Generic.class);
        when(typeVariable.getUpperBounds()).thenReturn(new TypeList.Generic.Explicit(bound));
        when(bound.asGenericType()).thenReturn(bound);
        when(bound.accept(TypeDescription.Generic.Visitor.Validator.EXCEPTION)).thenReturn(false);
        assertThat(TypeDescription.Generic.Visitor.Validator.EXCEPTION.onTypeVariable(typeVariable), is(false));
        when(bound.accept(TypeDescription.Generic.Visitor.Validator.EXCEPTION)).thenReturn(true);
        assertThat(TypeDescription.Generic.Visitor.Validator.EXCEPTION.onTypeVariable(typeVariable), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.EXCEPTION.onGenericArray(mock(TypeDescription.Generic.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.EXCEPTION.onParameterizedType(mock(TypeDescription.Generic.class)), is(false));
    }

    @Test
    public void testSuperClassType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onNonGenericType(TypeDescription.Generic.OBJECT), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onNonGenericType(TypeDefinition.Sort.describe(Serializable.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onNonGenericType(TypeDefinition.Sort.describe(void.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onNonGenericType(TypeDefinition.Sort.describe(int.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onNonGenericType(TypeDefinition.Sort.describe(void.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onNonGenericType(TypeDefinition.Sort.describe(Object[].class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onParameterizedType(TypeDescription.Generic.OBJECT), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onParameterizedType(TypeDefinition.Sort.describe(Serializable.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onTypeVariable(mock(TypeDescription.Generic.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.SUPER_CLASS.onGenericArray(mock(TypeDescription.Generic.class)), is(false));
    }

    @Test
    public void testInterfaceType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onNonGenericType(TypeDescription.Generic.OBJECT), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onNonGenericType(TypeDefinition.Sort.describe(Serializable.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onNonGenericType(TypeDefinition.Sort.describe(void.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onNonGenericType(TypeDefinition.Sort.describe(int.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onNonGenericType(TypeDefinition.Sort.describe(void.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onNonGenericType(TypeDefinition.Sort.describe(Object[].class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onParameterizedType(TypeDescription.Generic.OBJECT), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onParameterizedType(TypeDefinition.Sort.describe(Serializable.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onTypeVariable(mock(TypeDescription.Generic.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.INTERFACE.onGenericArray(mock(TypeDescription.Generic.class)), is(false));
    }

    @Test
    public void testFieldType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onNonGenericType(TypeDescription.Generic.OBJECT), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onNonGenericType(TypeDefinition.Sort.describe(Object[].class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onNonGenericType(TypeDefinition.Sort.describe(int.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onNonGenericType(TypeDefinition.Sort.describe(void.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onGenericArray(mock(TypeDescription.Generic.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onParameterizedType(mock(TypeDescription.Generic.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.FIELD.onTypeVariable(mock(TypeDescription.Generic.class)), is(true));
    }

    @Test
    public void testMethodParameterType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onNonGenericType(TypeDescription.Generic.OBJECT), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onNonGenericType(TypeDefinition.Sort.describe(Object[].class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onNonGenericType(TypeDefinition.Sort.describe(int.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onNonGenericType(TypeDefinition.Sort.describe(void.class)), is(false));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onGenericArray(mock(TypeDescription.Generic.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onParameterizedType(mock(TypeDescription.Generic.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_PARAMETER.onTypeVariable(mock(TypeDescription.Generic.class)), is(true));
    }

    @Test
    public void testMethodReturnType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onNonGenericType(TypeDescription.Generic.OBJECT), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onNonGenericType(TypeDefinition.Sort.describe(Object[].class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onNonGenericType(TypeDefinition.Sort.describe(int.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onNonGenericType(TypeDefinition.Sort.describe(void.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onGenericArray(mock(TypeDescription.Generic.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onParameterizedType(mock(TypeDescription.Generic.class)), is(true));
        assertThat(TypeDescription.Generic.Visitor.Validator.METHOD_RETURN.onTypeVariable(mock(TypeDescription.Generic.class)), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Validator.class).apply();
    }
}
