package net.bytebuddy.description.type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericOfParameterizedTypeForGenerifiedErasureTest {

    @Test
    public void testNonGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfParameterizedType.ForGenerifiedErasure.of(TypeDescription.OBJECT);
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
    }

    @Test
    public void testGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfParameterizedType.ForGenerifiedErasure.of(new TypeDescription.ForLoadedType(Foo.class));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(typeDescription.getTypeArguments().getOnly().getSymbol(), is("T"));
    }

    public static class Foo<T> {
        /* empty */
    }
}