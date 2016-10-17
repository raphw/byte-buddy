package net.bytebuddy.description.type;

import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorSubstitutorForTypeVariableBindingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic source, target, unknown, substituted;

    @Mock
    private TypeDescription.AbstractBase typeDefinition;

    @Mock
    private MethodDescription.AbstractBase methodDefinition;

    private Map<TypeDescription.Generic, TypeDescription.Generic> mapping;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        mapping = new HashMap<TypeDescription.Generic, TypeDescription.Generic>();
        mapping.put(source, target);
        when(typeDefinition.accept(any(TypeVariableSource.Visitor.class))).thenCallRealMethod();
        when(methodDefinition.accept(any(TypeVariableSource.Visitor.class))).thenReturn(substituted);
    }

    @Test
    public void testSimpleType() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(mapping).onSimpleType(source), is(source));
    }

    @Test
    public void testNonGenericType() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(mapping).onNonGenericType(source), is(source));
    }

    @Test
    public void testTypeVariableKnownOnType() throws Exception {
        when(source.getTypeVariableSource()).thenReturn(typeDefinition);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(mapping).onTypeVariable(source), is(target));
    }

    @Test
    public void testTypeVariableUnknownOnType() throws Exception {
        when(unknown.getTypeVariableSource()).thenReturn(typeDefinition);
        TypeDescription.Generic rawType = mock(TypeDescription.Generic.class);
        when(unknown.asRawType()).thenReturn(rawType);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(mapping).onTypeVariable(unknown),
                is(rawType));
    }

    @Test
    public void testTypeVariableKnownOnMethod() throws Exception {
        when(source.getTypeVariableSource()).thenReturn(methodDefinition);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(mapping).onTypeVariable(source), is(substituted));
    }

    @Test
    public void testTypeVariableUnknownOnMethod() throws Exception {
        when(unknown.getTypeVariableSource()).thenReturn(methodDefinition);
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding(mapping).onTypeVariable(unknown), is(substituted));
    }

    @Test
    public void testUnequalVariablesAndParameters() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        when(typeDescription.getTypeArguments()).thenReturn(new TypeList.Generic.Explicit(mock(TypeDescription.Generic.class)));
        TypeDescription rawTypeDescription = mock(TypeDescription.class);
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
        when(rawTypeDescription.getTypeVariables()).thenReturn(new TypeList.Generic.Empty());
        assertThat(TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding.bind(typeDescription),
                is((TypeDescription.Generic.Visitor<TypeDescription.Generic>) TypeDescription.Generic.Visitor.TypeVariableErasing.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding.TypeVariableSubstitutor.class).apply();
    }
}
