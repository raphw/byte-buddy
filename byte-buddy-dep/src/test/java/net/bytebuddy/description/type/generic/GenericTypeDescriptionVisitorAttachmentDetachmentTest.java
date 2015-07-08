package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class GenericTypeDescriptionVisitorAttachmentDetachmentTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testDetachment() throws Exception {
        GenericTypeDescription original = GenericTypeDescription.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        GenericTypeDescription detached = original.accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        assertThat(detached, not(sameInstance(original)));
        assertThat(detached.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(detached.asRawType(), is(TargetType.DESCRIPTION));
        assertThat(detached.getParameters().size(), is(4));
        assertThat(detached.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.VARIABLE_DETACHED));
        assertThat(detached.getParameters().get(0).getSymbol(), is("T"));
        assertThat(detached.getParameters().get(0).getUpperBounds().size(), is(1));
        assertThat(detached.getParameters().get(0).getUpperBounds().getOnly().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(detached.getParameters().get(0).getUpperBounds().getOnly().asRawType().represents(Object.class), is(true));
        assertThat(detached.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(detached.getParameters().get(1).asRawType().represents(String.class), is(true));
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
        assertThat(detached.getParameters().get(3).getParameters().getOnly().getUpperBounds().getOnly().asRawType().represents(CharSequence.class), is(true));
        assertThat(detached.getOwnerType(), notNullValue(GenericTypeDescription.class));
        assertThat(detached.getOwnerType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(detached.getOwnerType().getParameters().size(), is(1));
        assertThat(detached.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE_DETACHED));
        assertThat(detached.getOwnerType().getParameters().getOnly().getSymbol(), is("T"));
        assertThat(detached.getOwnerType().getParameters().getOnly(), sameInstance(detached.getParameters().get(0)));
    }

    @Test
    public void testAttachment() throws Exception {
        GenericTypeDescription original = GenericTypeDescription.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        GenericTypeDescription detached = original.accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        TypeDescription target = new TypeDescription.ForLoadedType(Bar.class);
        GenericTypeDescription attached = detached.accept(new GenericTypeDescription.Visitor.Substitutor.ForAttachment(target, target));
        assertThat(attached.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(attached.asRawType(), sameInstance(target));
        assertThat(attached.getParameters().size(), is(4));
        assertThat(attached.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(attached.getParameters().get(0).getSymbol(), is("T"));
        assertThat(attached.getParameters().get(0), is(target.getTypeVariables().filter(named("T")).getOnly()));
        assertThat(attached.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(attached.getParameters().get(1).asRawType().represents(String.class), is(true));
        assertThat(attached.getParameters().get(2).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(attached.getParameters().get(2).asRawType().represents(Object.class), is(true));
        assertThat(attached.getParameters().get(3).getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(attached.getParameters().get(3).asRawType().represents(List.class), is(true));
        assertThat(attached.getParameters().get(3).getParameters().size(), is(1));
        assertThat(attached.getParameters().get(3).getParameters().getOnly(), is(target.getTypeVariables().filter(named("S")).getOnly()));
        assertThat(attached.getOwnerType(), notNullValue(GenericTypeDescription.class));
        assertThat(attached.getOwnerType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(attached.getOwnerType().getParameters().size(), is(1));
        assertThat(attached.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(attached.getOwnerType().getParameters().getOnly().getSymbol(), is("T"));
        assertThat(attached.getOwnerType().getParameters().getOnly(), is(target.getTypeVariables().filter(named("T")).getOnly()));
    }

    @Test(expected = IllegalStateException.class)
    public void testDetachedNoSource() throws Exception {
        GenericTypeDescription original = GenericTypeDescription.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        GenericTypeDescription detached = original.accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        detached.getParameters().get(0).getUpperBounds();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.Substitutor.ForAttachment.class).apply();
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.Substitutor.ForDetachment.class).applyMutable();
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
