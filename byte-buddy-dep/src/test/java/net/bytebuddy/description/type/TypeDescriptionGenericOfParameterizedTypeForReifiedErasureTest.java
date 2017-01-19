package net.bytebuddy.description.type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericOfParameterizedTypeForReifiedErasureTest {

    @Test
    public void testNonGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.of(TypeDescription.OBJECT);
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSort(), not(instanceOf(TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.class)));
    }

    @Test
    public void testGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.OfNonGenericType.ForReifiedErasure(new TypeDescription.ForLoadedType(Bar.class));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(typeDescription.getSuperClass().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getSuperClass().asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
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