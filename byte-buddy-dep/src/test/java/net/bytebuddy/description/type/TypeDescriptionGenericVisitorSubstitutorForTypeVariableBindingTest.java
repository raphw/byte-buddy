package net.bytebuddy.description.type;

import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorSubstitutorForTypeVariableBindingTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic parameterizedType, source, target, unknown, substituted;

    @Mock
    private TypeDescription.AbstractBase typeDefinition;

    @Mock
    private MethodDescription.AbstractBase methodDefinition;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(parameterizedType.findBindingOf(source)).thenReturn(target);
        when(typeDefinition.accept(any(TypeVariableSource.Visitor.class))).thenCallRealMethod();
        when(methodDefinition.accept(any(TypeVariableSource.Visitor.class))).thenReturn(substituted);
    }

    @Test
    public void testSimpleType() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(parameterizedType).onSimpleType(source), is(source));
    }

    @Test
    public void testNonGenericType() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(parameterizedType).onNonGenericType(source), is(source));
    }

    @Test
    public void testTypeVariableKnownOnType() throws Exception {
        when(source.getTypeVariableSource()).thenReturn(typeDefinition);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(parameterizedType).onTypeVariable(source), is(target));
    }

    @Test
    public void testTypeVariableUnknownOnType() throws Exception {
        when(unknown.getTypeVariableSource()).thenReturn(typeDefinition);
        TypeDescription.Generic rawType = mock(TypeDescription.Generic.class);
        when(unknown.asRawType()).thenReturn(rawType);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(parameterizedType).onTypeVariable(unknown), is(rawType));
    }

    @Test
    public void testTypeVariableKnownOnMethod() throws Exception {
        when(source.getTypeVariableSource()).thenReturn(methodDefinition);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(parameterizedType).onTypeVariable(source), is(substituted));
    }

    @Test
    public void testTypeVariableUnknownOnMethod() throws Exception {
        when(unknown.getTypeVariableSource()).thenReturn(methodDefinition);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(parameterizedType).onTypeVariable(unknown), is(substituted));
    }

    @Test
    public void testUnequalVariablesAndParameters() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        when(typeDescription.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(mock(TypeDescription.Generic.class)));
        TypeDescription rawTypeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        when(rawTypeDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
    }
}
