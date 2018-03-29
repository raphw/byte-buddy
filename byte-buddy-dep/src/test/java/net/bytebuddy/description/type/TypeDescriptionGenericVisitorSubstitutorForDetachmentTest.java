package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericVisitorSubstitutorForDetachmentTest {

    private static final String FOO = "foo";

    @Test
    public void testDetachment() throws Exception {
        TypeDescription.Generic original = TypeDefinition.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        TypeDescription.Generic detached = original.accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        assertThat(detached, not(sameInstance(original)));
        assertThat(detached.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(detached.asErasure(), is(TargetType.DESCRIPTION));
        assertThat(detached.getTypeArguments().size(), is(4));
        assertThat(detached.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getTypeArguments().get(0).getSymbol(), is("T"));
        assertThat(detached.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(detached.getTypeArguments().get(1).asErasure().represents(String.class), is(true));
        assertThat(detached.getTypeArguments().get(2).getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getTypeArguments().get(2).getSymbol(), is("U"));
        assertThat(detached.getTypeArguments().get(3).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(detached.getTypeArguments().get(3).getTypeArguments().size(), is(1));
        assertThat(detached.getTypeArguments().get(3).getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getTypeArguments().get(3).getTypeArguments().getOnly().getSymbol(), is("S"));
        assertThat(detached.getOwnerType(), notNullValue(TypeDescription.Generic.class));
        assertThat(detached.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(detached.getOwnerType().getTypeArguments().size(), is(1));
        assertThat(detached.getOwnerType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getOwnerType().getTypeArguments().getOnly().getSymbol(), is("T"));
    }

    @Test(expected = IllegalStateException.class)
    public void testDetachedNoSource() throws Exception {
        TypeDescription.Generic original = TypeDefinition.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        TypeDescription.Generic detached = original.accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        detached.getTypeArguments().get(0).getTypeVariableSource();
    }

    @SuppressWarnings("unused")
    public static class Foo<O> {

        public abstract class Inner<T, S extends CharSequence, U extends T, V> {

            Foo<T>.Inner<T, String, U, List<S>> foo;
        }
    }

    @SuppressWarnings("unused")
    public abstract static class Bar<A, T, S, V extends Number> {
        /* empty */
    }
}
