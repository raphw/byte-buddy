package net.bytebuddy.instrumentation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeInitializerForStaticFieldTest {

    private static final String FOO = "foo";

    @Test
    public void testAccessibleField() throws Exception {
        Object object = new Object();
        TypeInitializer typeInitializer = TypeInitializer.ForStaticField.accessible(FOO, object);
        assertThat(typeInitializer.isAlive(), is(true));
        typeInitializer.onLoad(Foo.class);
        assertThat(Foo.foo, is(object));
    }

    @Test
    public void testNonAccessibleField() throws Exception {
        Object object = new Object();
        TypeInitializer typeInitializer = TypeInitializer.ForStaticField.nonAccessible(FOO, object);
        assertThat(typeInitializer.isAlive(), is(true));
        typeInitializer.onLoad(Bar.class);
        assertThat(Bar.foo, is(object));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonAccessibleFieldThrowsException() throws Exception {
        Object object = new Object();
        TypeInitializer typeInitializer = TypeInitializer.ForStaticField.accessible(FOO, object);
        assertThat(typeInitializer.isAlive(), is(true));
        typeInitializer.onLoad(Bar.class);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        Object first = new Object(), second = new Object();
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first).hashCode(),
                is(TypeInitializer.ForStaticField.accessible(FOO, first).hashCode()));
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first),
                is(TypeInitializer.ForStaticField.accessible(FOO, first)));
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first).hashCode(),
                not(is(TypeInitializer.ForStaticField.nonAccessible(FOO, first).hashCode())));
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first),
                not(is(TypeInitializer.ForStaticField.nonAccessible(FOO, first))));
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first).hashCode(),
                not(is(TypeInitializer.ForStaticField.accessible(FOO, second).hashCode())));
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first),
                not(is(TypeInitializer.ForStaticField.accessible(FOO, second))));
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first).hashCode(),
                not(is(TypeInitializer.ForStaticField.accessible(FOO + FOO, first).hashCode())));
        assertThat(TypeInitializer.ForStaticField.accessible(FOO, first),
                not(is(TypeInitializer.ForStaticField.accessible(FOO + FOO, first))));
    }

    @SuppressWarnings("unused")
    public static class Foo {

        public static Object foo;
    }

    @SuppressWarnings("unused")
    public static class Bar {

        private static Object foo;
    }
}
