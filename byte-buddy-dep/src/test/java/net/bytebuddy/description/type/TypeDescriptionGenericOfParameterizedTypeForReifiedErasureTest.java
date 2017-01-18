package net.bytebuddy.description.type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericOfParameterizedTypeForReifiedErasureTest {

    @Test
    public void testNonGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfParameterizedType.ForReifiedErasure.of(TypeDescription.OBJECT);
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
    }

    @Test
    public void testGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfParameterizedType.ForReifiedErasure.of(new TypeDescription.ForLoadedType(Bar.class));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
        assertThat(typeDescription.getSuperClass().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getSuperClass().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
    }

    public static class Foo<T> {

        T foo;
    }

    public static class Bar<S extends Number> extends Foo<S> {
        /* empty */
    }
}