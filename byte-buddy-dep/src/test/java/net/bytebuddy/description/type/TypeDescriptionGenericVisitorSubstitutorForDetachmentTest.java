package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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
        assertThat(detached.getParameters().size(), is(4));
        assertThat(detached.getParameters().get(0).getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getParameters().get(0).getSymbol(), is("T"));
        assertThat(detached.getParameters().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(detached.getParameters().get(1).asErasure().represents(String.class), is(true));
        assertThat(detached.getParameters().get(2).getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getParameters().get(2).getSymbol(), is("U"));
        assertThat(detached.getParameters().get(3).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(detached.getParameters().get(3).getParameters().size(), is(1));
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getSymbol(), is("S"));
        assertThat(detached.getOwnerType(), notNullValue(TypeDescription.Generic.class));
        assertThat(detached.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(detached.getOwnerType().getParameters().size(), is(1));
        assertThat(detached.getOwnerType().getParameters().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE_SYMBOLIC));
        assertThat(detached.getOwnerType().getParameters().getOnly().getSymbol(), is("T"));
    }

    @Test(expected = IllegalStateException.class)
    public void testDetachedNoSource() throws Exception {
        TypeDescription.Generic original = TypeDefinition.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        TypeDescription.Generic detached = original.accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        detached.getParameters().get(0).getVariableSource();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Substitutor.ForDetachment.class).apply();
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
