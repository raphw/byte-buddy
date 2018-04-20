package net.bytebuddy.description.type;

import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericOfParameterizedTypeForReifiedTypeTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testSuperType() throws Exception {
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.OfParameterizedType.ForReifiedType(TypeDescription.ForLoadedType.of(Sample.class)
                .getSuperClass());
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Bar.class)));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getSuperClass().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getSuperClass().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Foo.class)));
        assertThat(typeDescription.getSuperClass().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
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
    }

    @Test
    public void testInterfaceType() throws Exception {
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.OfParameterizedType.ForReifiedType(TypeDescription.ForLoadedType.of(Sample.class)
                .getInterfaces().getOnly());
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Baz.class)));
        assertThat(typeDescription.getTypeArguments().size(), is(1));
        assertThat(typeDescription.getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getTypeArguments().getOnly().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Number.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getInterfaces().getOnly().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Qux.class)));
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
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.of(TypeDescription.ForLoadedType.of(NonGenericSample.class))
                .getSuperClass()
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
        assertThat(typeDescription.getInterfaces().getOnly().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(Qux.class)));
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

    private static class Bar<T extends Number> extends Foo<T> {
        /* empty */
    }

    private interface Qux<T> {

        T foo();

        List<?> bar();
    }

    private interface Baz<T extends Number> extends Qux<T> {
        /* empty */
    }

    private abstract static class Sample extends Bar<Number> implements Baz<Number> {
        /* empty */
    }

    private class NonGenericIntermediate extends Foo<Number> implements Qux<Number> {
        /* empty */
    }

    private class RawTypeIntermediate<T> extends NonGenericIntermediate {
        /* empty */
    }

    private class NonGenericSample extends RawTypeIntermediate<Number> {
        /* empty */
    }
}
