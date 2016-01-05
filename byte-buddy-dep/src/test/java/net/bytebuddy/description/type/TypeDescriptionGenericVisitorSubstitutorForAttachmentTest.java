package net.bytebuddy.description.type;

import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericVisitorSubstitutorForAttachmentTest {

    private static final String FOO = "foo";

    @Test
    public void testAttachment() throws Exception {
        TypeDescription.Generic original = TypeDefinition.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        TypeDescription.Generic detached = original.accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        TypeDescription target = new TypeDescription.ForLoadedType(Bar.class);
        TypeDescription.Generic attached = detached.accept(new TypeDescription.Generic.Visitor.Substitutor.ForAttachment(target.asGenericType(), target));
        assertThat(attached.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(attached.asErasure(), sameInstance(target));
        assertThat(attached.getParameters().size(), is(4));
        assertThat(attached.getParameters().get(0).getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(attached.getParameters().get(0).getSymbol(), is("T"));
        assertThat(attached.getParameters().get(0), is(target.getTypeVariables().filter(named("T")).getOnly()));
        assertThat(attached.getParameters().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(attached.getParameters().get(1).asErasure().represents(String.class), is(true));
        assertThat(attached.getParameters().get(2).getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(attached.getParameters().get(2).getSymbol(), is("U"));
        assertThat(attached.getParameters().get(2), is(target.getTypeVariables().filter(named("U")).getOnly()));
        assertThat(attached.getParameters().get(3).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(attached.getParameters().get(3).asErasure().represents(List.class), is(true));
        assertThat(attached.getParameters().get(3).getParameters().size(), is(1));
        assertThat(attached.getParameters().get(3).getParameters().getOnly(), is(target.getTypeVariables().filter(named("S")).getOnly()));
        assertThat(attached.getOwnerType(), notNullValue(TypeDescription.Generic.class));
        assertThat(attached.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(attached.getOwnerType().getParameters().size(), is(1));
        assertThat(attached.getOwnerType().getParameters().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(attached.getOwnerType().getParameters().getOnly().getSymbol(), is("T"));
        assertThat(attached.getOwnerType().getParameters().getOnly(), is(target.getTypeVariables().filter(named("T")).getOnly()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAttachment() throws Exception {
        TypeDescription.Generic original = TypeDefinition.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        TypeDescription.Generic detached = original.accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        detached.accept(new TypeDescription.Generic.Visitor.Substitutor.ForAttachment(TypeDescription.Generic.OBJECT, TypeDescription.OBJECT));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.class).apply();
    }

    @SuppressWarnings("unused")
    public static class Foo<O> {

        public abstract class Inner<T, S extends CharSequence, U extends T, V> {

            Foo<T>.Inner<T, String, U, List<S>> foo;
        }
    }

    @SuppressWarnings("unused")
    public abstract static class Bar<U, T, S, V extends Number> {
        /* empty */
    }
}
