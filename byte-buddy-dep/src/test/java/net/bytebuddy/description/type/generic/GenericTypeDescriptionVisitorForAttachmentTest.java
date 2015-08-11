package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class GenericTypeDescriptionVisitorForAttachmentTest {

    private static final String FOO = "foo";

    @Test
    public void testAttachment() throws Exception {
        GenericTypeDescription original = GenericTypeDescription.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        GenericTypeDescription detached = original.accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        TypeDescription target = new TypeDescription.ForLoadedType(Bar.class);
        GenericTypeDescription attached = detached.accept(new GenericTypeDescription.Visitor.Substitutor.ForAttachment(target, target));
        assertThat(attached.getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(attached.asErasure(), sameInstance(target));
        assertThat(attached.getParameters().size(), is(4));
        assertThat(attached.getParameters().get(0).getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(attached.getParameters().get(0).getSymbol(), is("T"));
        assertThat(attached.getParameters().get(0), is(target.getTypeVariables().filter(named("T")).getOnly()));
        assertThat(attached.getParameters().get(1).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(attached.getParameters().get(1).asErasure().represents(String.class), is(true));
        assertThat(attached.getParameters().get(2).getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(attached.getParameters().get(2).asErasure().represents(Object.class), is(true));
        assertThat(attached.getParameters().get(3).getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(attached.getParameters().get(3).asErasure().represents(List.class), is(true));
        assertThat(attached.getParameters().get(3).getParameters().size(), is(1));
        assertThat(attached.getParameters().get(3).getParameters().getOnly(), is(target.getTypeVariables().filter(named("S")).getOnly()));
        assertThat(attached.getOwnerType(), notNullValue(GenericTypeDescription.class));
        assertThat(attached.getOwnerType().getSort(), is(GenericTypeDescription.Sort.PARAMETERIZED));
        assertThat(attached.getOwnerType().getParameters().size(), is(1));
        assertThat(attached.getOwnerType().getParameters().getOnly().getSort(), is(GenericTypeDescription.Sort.VARIABLE));
        assertThat(attached.getOwnerType().getParameters().getOnly().getSymbol(), is("T"));
        assertThat(attached.getOwnerType().getParameters().getOnly(), is(target.getTypeVariables().filter(named("T")).getOnly()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.Substitutor.ForAttachment.class).apply();
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
