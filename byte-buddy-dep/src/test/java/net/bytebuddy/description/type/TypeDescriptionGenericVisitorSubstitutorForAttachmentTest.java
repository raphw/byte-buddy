package net.bytebuddy.description.type;

import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

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
        assertThat(attached.getTypeArguments().size(), is(4));
        assertThat(attached.getTypeArguments().get(0).getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(attached.getTypeArguments().get(0).getSymbol(), is("T"));
        assertThat(attached.getTypeArguments().get(0), is(target.getTypeVariables().filter(named("T")).getOnly()));
        assertThat(attached.getTypeArguments().get(1).getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(attached.getTypeArguments().get(1).asErasure().represents(String.class), is(true));
        assertThat(attached.getTypeArguments().get(2).getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(attached.getTypeArguments().get(2).getSymbol(), is("U"));
        assertThat(attached.getTypeArguments().get(2), is(target.getTypeVariables().filter(named("U")).getOnly()));
        assertThat(attached.getTypeArguments().get(3).getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(attached.getTypeArguments().get(3).asErasure().represents(List.class), is(true));
        assertThat(attached.getTypeArguments().get(3).getTypeArguments().size(), is(1));
        assertThat(attached.getTypeArguments().get(3).getTypeArguments().getOnly(), is(target.getTypeVariables().filter(named("S")).getOnly()));
        assertThat(attached.getOwnerType(), notNullValue(TypeDescription.Generic.class));
        assertThat(attached.getOwnerType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(attached.getOwnerType().getTypeArguments().size(), is(1));
        assertThat(attached.getOwnerType().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.VARIABLE));
        assertThat(attached.getOwnerType().getTypeArguments().getOnly().getSymbol(), is("T"));
        assertThat(attached.getOwnerType().getTypeArguments().getOnly(), is(target.getTypeVariables().filter(named("T")).getOnly()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAttachment() throws Exception {
        TypeDescription.Generic original = TypeDefinition.Sort.describe(Foo.Inner.class.getDeclaredField(FOO).getGenericType());
        TypeDescription.Generic detached = original.accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(ElementMatchers.is(Foo.Inner.class)));
        detached.accept(new TypeDescription.Generic.Visitor.Substitutor.ForAttachment(TypeDescription.Generic.OBJECT, TypeDescription.OBJECT));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.class)
                .refine(new ObjectPropertyAssertion.Refinement<TypeDefinition>() {
                    @Override
                    public void apply(TypeDefinition mock) {
                        when(mock.asErasure()).thenReturn(Mockito.mock(TypeDescription.class));
                    }
                }).apply();
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
