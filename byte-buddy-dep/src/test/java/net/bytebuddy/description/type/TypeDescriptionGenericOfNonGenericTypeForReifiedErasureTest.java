package net.bytebuddy.description.type;

import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class TypeDescriptionGenericOfNonGenericTypeForReifiedErasureTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testNonGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.of(TypeDescription.OBJECT);
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSort(), not(instanceOf(TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.class)));
    }

    @Test
    public void testGenerifiedType() throws Exception {
        TypeDescription.Generic typeDescription = TypeDescription.Generic.OfNonGenericType.ForReifiedErasure.of(new TypeDescription.ForLoadedType(Qux.class));
        assertThat(typeDescription.getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Qux.class)));
        assertThat(typeDescription.getSuperClass().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getSuperClass().asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Foo.class)));
        assertThat(typeDescription.getSuperClass().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getTypeArguments().getOnly().asErasure(),
                is((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredFields().getOnly().getType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredFields().getOnly().getType().asErasure(),
                is((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) new TypeDescription.ForLoadedType(List.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(typeDescription.getInterfaces().getOnly().asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().size(), is(1));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().getOnly().getSort(), is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getTypeArguments().getOnly().asErasure(),
                is((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) new TypeDescription.ForLoadedType(Number.class)));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().getSort(),
                is(TypeDefinition.Sort.NON_GENERIC));
        assertThat(typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(named(BAR)).getOnly().getReturnType().asErasure(),
                is((TypeDescription) new TypeDescription.ForLoadedType(List.class)));
    }

    public static class Foo<T> {

        T foo;

        public T foo() {
            return null;
        }

        public List<?> bar() {
            return null;
        }
    }

    public interface Bar<T> {

        T foo();

        List<?> bar();
    }

    public static class Qux<T extends Number> extends Foo<T> implements Bar<T>{

        T foo;

        @Override
        public T foo() {
            return null;
        }

        public List<?> bar() {
            return null;
        }
    }
}