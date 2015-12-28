package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Type;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDefinitionTest {

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownType() throws Exception {
        TypeDefinition.Sort.describe(mock(Type.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeArityGenericArray() throws Exception {
        TypeDescription.Generic.ForGenericArray.Latent.of(mock(TypeDescription.Generic.class), -1);
    }

    @Test
    public void testZeroArityReturnsInstance() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        assertThat(TypeDescription.Generic.ForGenericArray.Latent.of(typeDescription, 0), sameInstance(typeDescription));
    }

    @Test
    public void testNonGenericArrayType() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        assertThat(TypeDescription.Generic.ForGenericArray.Latent.of(typeDescription, 1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(TypeDescription.Generic.ForGenericArray.Latent.of(typeDescription, 1).getComponentType(), is(typeDescription));
    }

    @Test
    public void testZeroArityIteratesTypesInstance() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        TypeDescription.Generic componentType = mock(TypeDescription.Generic.class);
        when(typeDescription.getComponentType()).thenReturn(componentType);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.GENERIC_ARRAY);
        when(componentType.getSort()).thenReturn(TypeDefinition.Sort.PARAMETERIZED);
        TypeDescription.Generic result = TypeDescription.Generic.ForGenericArray.Latent.of(typeDescription, 1);
        assertThat(result.getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(result.getComponentType().getSort(), is(TypeDefinition.Sort.GENERIC_ARRAY));
        assertThat(result.getComponentType().getComponentType(), is(componentType));
    }

    @Test
    public void testZeroArityIteratesTypesInstanceNonGeneric() throws Exception {
        TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
        TypeDescription.Generic componentType = mock(TypeDescription.Generic.class);
        when(typeDescription.getComponentType()).thenReturn(componentType);
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.GENERIC_ARRAY);
        when(componentType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        TypeDescription.Generic result = TypeDescription.Generic.ForGenericArray.Latent.of(typeDescription, 1);
        assertThat(result.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(result.getComponentType().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(result.getComponentType().getComponentType(), is(componentType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDefinition.Sort.class).apply();
    }
}
