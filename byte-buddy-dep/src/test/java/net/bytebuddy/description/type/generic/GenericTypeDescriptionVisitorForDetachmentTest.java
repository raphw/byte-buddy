package net.bytebuddy.description.type.generic;

import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class GenericTypeDescriptionVisitorForDetachmentTest {

    private static final String FOO = "foo";

    @Test
    public void testDetachment() throws Exception {
        GenericTypeDescription original = GenericTypeDescription.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        GenericTypeDescription detached = original.accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        assertThat(detached, not(sameInstance(original)));
        assertThat(detached.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(detached.asErasure(), is(TargetType.DESCRIPTION));
        assertThat(detached.getParameters().size(), is(4));
        assertThat(detached.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.VARIABLE_DETACHED));
        assertThat(detached.getParameters().get(0).getSymbol(), is("T"));
        assertThat(detached.getParameters().get(0).getUpperBounds().size(), is(1));
        assertThat(detached.getParameters().get(0).getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(detached.getParameters().get(0).getUpperBounds().getOnly().asErasure().represents(Object.class), is(true));
        assertThat(detached.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(detached.getParameters().get(1).asErasure().represents(String.class), is(true));
        assertThat(detached.getParameters().get(2).getSort(), is(GenericTypeDescription.Sort.VARIABLE_DETACHED));
        assertThat(detached.getParameters().get(2).getSymbol(), is("U"));
        assertThat(detached.getParameters().get(2).getUpperBounds().size(), is(1));
        assertThat(detached.getParameters().get(2).getUpperBounds().getOnly(), sameInstance(detached.getParameters().get(0)));
        assertThat(detached.getParameters().get(3).getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(detached.getParameters().get(3).getParameters().size(), is(1));
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE_DETACHED));
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getSymbol(), is("S"));
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getUpperBounds().size(), is(1));
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getUpperBounds().getOnly().asErasure().represents(CharSequence.class), is(true));
        assertThat(detached.getOwnerType(), notNullValue(GenericTypeDescription.class));
        assertThat(detached.getOwnerType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(detached.getOwnerType().getParameters().size(), is(1));
        assertThat(detached.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE_DETACHED));
        assertThat(detached.getOwnerType().getParameters().getOnly().getSymbol(), is("T"));
        assertThat(detached.getOwnerType().getParameters().getOnly(), sameInstance(detached.getParameters().get(0)));
    }

    @Test(expected = IllegalStateException.class)
    public void testDetachedNoSource() throws Exception {
        GenericTypeDescription original = GenericTypeDescription.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        GenericTypeDescription detached = original.accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        detached.getParameters().get(0).getVariableSource();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.Substitutor.ForDetachment.class).applyBasic();
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
