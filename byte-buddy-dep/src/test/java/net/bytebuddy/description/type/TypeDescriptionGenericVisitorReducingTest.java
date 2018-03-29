package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeDescriptionGenericVisitorReducingTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
    public void testGenericArray() throws Exception {
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        assertThat(visitor.onGenericArray(typeDescription), is(rawTypeDescription));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
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
        when(declaringType.findVariable(FOO)).thenReturn(genericTypeDescription);
        when(genericTypeDescription.asErasure()).thenReturn(rawTypeDescription);
        assertThat(visitor.onTypeVariable(typeDescription), is(rawTypeDescription));
        verify(typeDescription, times(2)).getSymbol();
        verifyNoMoreInteractions(typeDescription);
        verify(typeVariableToken).getSymbol();
        verifyNoMoreInteractions(typeVariableToken);
        verify(declaringType).findVariable(FOO);
        verifyNoMoreInteractions(declaringType);
    }

    @Test
    public void testTargetTypeResolution() throws Exception {
        assertThat(visitor.onGenericArray(TargetType.DESCRIPTION.asGenericType()), is(declaringType));
        assertThat(visitor.onParameterizedType(TargetType.DESCRIPTION.asGenericType()), is(declaringType));
        assertThat(visitor.onNonGenericType(TargetType.DESCRIPTION.asGenericType()), is(declaringType));
        when(typeDescription.getSymbol()).thenReturn(BAR);
        when(declaringType.findVariable(BAR)).thenReturn(TargetType.DESCRIPTION.asGenericType());
        assertThat(visitor.onTypeVariable(typeDescription), is(declaringType));
    }
}
