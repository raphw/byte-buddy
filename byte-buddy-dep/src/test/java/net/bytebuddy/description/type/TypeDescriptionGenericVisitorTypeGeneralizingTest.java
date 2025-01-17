package net.bytebuddy.description.type;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorTypeGeneralizingTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic typeDescription, componentType;


    @Test
    public void testGenericArray() throws Exception {
        when(typeDescription.getComponentType()).thenReturn(componentType);
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onGenericArray(typeDescription),
                is(TypeDefinition.Sort.describe(Object[].class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWildcard() throws Exception {
        TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onWildcard(typeDescription);
    }

    @Test
    public void testParameterized() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onParameterizedType(typeDescription),
                is(TypeDefinition.Sort.describe(Object.class)));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onTypeVariable(typeDescription),
                is(TypeDefinition.Sort.describe(Object.class)));
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onNonGenericType(typeDescription),
                is(TypeDefinition.Sort.describe(Object.class)));
    }

    @Test
    public void testNonGenericArray() throws Exception {
        when(typeDescription.isArray()).thenReturn(true);
        when(typeDescription.getComponentType()).thenReturn(componentType);
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onNonGenericType(typeDescription),
                is(TypeDefinition.Sort.describe(Object[].class)));
    }

    @Test
    public void testNonGenericPrimitive() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onNonGenericType(TypeDefinition.Sort.describe(int.class)),
                is(TypeDefinition.Sort.describe(int.class)));
    }

    @Test
    public void testNonGenericPrimitiveArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.Generalizing.INSTANCE.onNonGenericType(TypeDefinition.Sort.describe(int[].class)),
                is(TypeDefinition.Sort.describe(int[].class)));
    }
}
