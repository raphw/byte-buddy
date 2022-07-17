package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.TargetType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeDescriptionGenericVisitorReducingTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic typeDescription, bound, genericTypeDescription;

    @Mock
    private TypeDescription declaringType, rawTypeDescription;

    @Mock
    private TypeVariableToken typeVariableToken;

    private TypeDescription.Generic.Visitor<TypeDescription> visitor;

    @Before
    public void setUp() throws Exception {
        when(bound.asGenericType()).thenReturn(bound);
        visitor = new TypeDescription.Generic.Visitor.Reducing(declaringType, Collections.singletonList(typeVariableToken));
    }

    @Test(expected = IllegalStateException.class)
    public void testWildcardThrowsException() throws Exception {
        visitor.onWildcard(typeDescription);
    }

    @Test
    public void testGenericArrayOfNonVariable() throws Exception {
        when(typeDescription.getComponentType()).thenReturn(typeDescription);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        assertThat(visitor.onGenericArray(typeDescription), is(rawTypeDescription));
        verify(typeDescription).asErasure();
        verify(typeDescription).getComponentType();
        verify(typeDescription).isArray();
        verify(typeDescription).getSort();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testGenericArrayOfVariableContextDeclared() throws Exception {
        when(typeDescription.getComponentType()).thenReturn(typeDescription);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        when(typeDescription.getSymbol()).thenReturn(FOO);
        when(typeVariableToken.getSymbol()).thenReturn(BAR);
        when(declaringType.findExpectedVariable(FOO)).thenReturn(typeDescription);
        when(rawTypeDescription.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        when(rawTypeDescription.getDescriptor()).thenReturn(BAR);
        assertThat(visitor.onGenericArray(typeDescription), not(rawTypeDescription));
        verify(typeDescription).asErasure();
        verify(typeDescription).getComponentType();
        verify(typeDescription).isArray();
        verify(typeDescription).getSort();
        verify(typeDescription, times(2)).getSymbol();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testGenericArrayOfVariableSelfDeclared() throws Exception {
        when(typeDescription.getComponentType()).thenReturn(typeDescription);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        when(typeDescription.getComponentType()).thenReturn(genericTypeDescription);
        when(genericTypeDescription.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        when(genericTypeDescription.getSymbol()).thenReturn(FOO);
        when(typeVariableToken.getSymbol()).thenReturn(FOO);
        when(typeVariableToken.getBounds()).thenReturn(new TypeList.Generic.Explicit(bound));
        when(bound.accept(visitor)).thenReturn(rawTypeDescription);
        when(rawTypeDescription.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        when(rawTypeDescription.getDescriptor()).thenReturn(BAR);
        assertThat(visitor.onGenericArray(typeDescription), not(rawTypeDescription));
        verify(typeDescription).getComponentType();
        verifyNoMoreInteractions(typeDescription);
        verify(genericTypeDescription).getSymbol();
        verify(genericTypeDescription).getSort();
        verify(genericTypeDescription).isArray();
        verifyNoMoreInteractions(genericTypeDescription);
    }

    @Test
    public void testParameterizedType() throws Exception {
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        assertThat(visitor.onParameterizedType(typeDescription), is(rawTypeDescription));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testNonGenericType() throws Exception {
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        assertThat(visitor.onNonGenericType(typeDescription), is(rawTypeDescription));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testTypeVariableSelfDeclared() throws Exception {
        when(typeDescription.getSymbol()).thenReturn(FOO);
        when(typeVariableToken.getSymbol()).thenReturn(FOO);
        when(typeVariableToken.getBounds()).thenReturn(new TypeList.Generic.Explicit(bound));
        when(bound.accept(visitor)).thenReturn(rawTypeDescription);
        assertThat(visitor.onTypeVariable(typeDescription), is(rawTypeDescription));
        verify(typeDescription).getSymbol();
        verifyNoMoreInteractions(typeDescription);
        verify(typeVariableToken).getSymbol();
        verify(typeVariableToken).getBounds();
        verifyNoMoreInteractions(typeVariableToken);
    }

    @Test
    public void testTypeVariableContextDeclared() throws Exception {
        when(typeDescription.getSymbol()).thenReturn(FOO);
        when(typeVariableToken.getSymbol()).thenReturn(BAR);
        when(declaringType.findExpectedVariable(FOO)).thenReturn(genericTypeDescription);
        when(genericTypeDescription.asErasure()).thenReturn(rawTypeDescription);
        assertThat(visitor.onTypeVariable(typeDescription), is(rawTypeDescription));
        verify(typeDescription, times(2)).getSymbol();
        verifyNoMoreInteractions(typeDescription);
        verify(typeVariableToken).getSymbol();
        verifyNoMoreInteractions(typeVariableToken);
        verify(declaringType).findExpectedVariable(FOO);
        verifyNoMoreInteractions(declaringType);
    }

    @Test
    public void testTargetTypeResolution() throws Exception {
        assertThat(visitor.onGenericArray(TypeDescription.ArrayProjection.of(TargetType.DESCRIPTION).asGenericType()).getComponentType(), is(declaringType));
        assertThat(visitor.onParameterizedType(TargetType.DESCRIPTION.asGenericType()), is(declaringType));
        assertThat(visitor.onNonGenericType(TargetType.DESCRIPTION.asGenericType()), is(declaringType));
        when(typeDescription.getSymbol()).thenReturn(BAR);
        when(declaringType.findExpectedVariable(BAR)).thenReturn(TargetType.DESCRIPTION.asGenericType());
        assertThat(visitor.onTypeVariable(typeDescription), is(declaringType));
    }
}
