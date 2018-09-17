package net.bytebuddy.description.type;

import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericOfNonGenericTypeForReifiedErasureTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testNonGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.of(TypeDescription.OBJECT);
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSort(), not(instanceOf(TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.class)));
    }

    @Test
    public void testGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.of(TypeDescription.ForLoadedType.of(Qux.class));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Qux.class)));
        assertThat(typeDescription.getDeclaredFields().getOnly().getType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredFields().getOnly().getType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getDeclaredMethods().filter(named(QUX)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(named(QUX)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(List.class)));
        assertThat(typeDescription.getSuperClass().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getSuperClass().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Foo.class)));
        assertThat(typeDescription.getSuperClass().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredFields().getOnly().getType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredFields().getOnly().getType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(List.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getInterfaces().getOnly().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Bar.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().getOnly().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(List.class)));
    }

    @Test
    public void testNonGenericIntermediateType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.of(TypeDescription.ForLoadedType.of(GenericIntermediate.class))
                .getSuperClass();
        assertThat(typeDescription.getSuperClass().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getSuperClass().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Foo.class)));
        assertThat(typeDescription.getSuperClass().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredFields().getOnly().getType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredFields().getOnly().getType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(List.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getInterfaces().getOnly().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Bar.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().getOnly().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) TypeDescription.ForLoadedType.of(List.class)));
    }

    private static class Foo<T> {

        T foo;

        public T foo() {
            return null;
        }

        public List<?> bar() {
            return null;
        }
    }

    private interface Bar<T> {

        T foo();

        List<?> bar();
    }

    private static class Qux<T extends Number> extends Foo<T> implements Bar<T> {

        T foo;

        public T qux() {
            return null;
        }

        public List<?> bar() {
            return null;
        }
    }

    private static class NonGenericIntermediate extends Foo<Number> implements Bar<Number> {
        /* empty */
    }

    private static class GenericIntermediate<T> extends NonGenericIntermediate {
        /* empty */
    }
}