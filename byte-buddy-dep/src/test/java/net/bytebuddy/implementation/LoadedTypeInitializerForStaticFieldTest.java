package net.bytebuddy.implementation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LoadedTypeInitializerForStaticFieldTest {

    private static final String FOO = "foo";

    @Test
    public void testAccessibleField() throws Exception {
        Object object = new Object();
        LoadedTypeInitializer loadedTypeInitializer = new LoadedTypeInitializer.ForStaticField(FOO, object);
        assertThat(loadedTypeInitializer.isAlive(), is(true));
        loadedTypeInitializer.onLoad(Foo.class);
        assertThat(Foo.foo, is(object));
    }

    @Test
    public void testNonAccessibleField() throws Exception {
        Object object = new Object();
        LoadedTypeInitializer loadedTypeInitializer = new LoadedTypeInitializer.ForStaticField(FOO, object);
        assertThat(loadedTypeInitializer.isAlive(), is(true));
        loadedTypeInitializer.onLoad(Bar.class);
        assertThat(Bar.foo, is(object));
    }

    @Test
    public void testNonAccessibleType() throws Exception {
        Object object = new Object();
        LoadedTypeInitializer loadedTypeInitializer = new LoadedTypeInitializer.ForStaticField(FOO, object);
        assertThat(loadedTypeInitializer.isAlive(), is(true));
        loadedTypeInitializer.onLoad(Qux.class);
        assertThat(Qux.foo, is(object));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonAssignableField() throws Exception {
        new LoadedTypeInitializer.ForStaticField(FOO, new Object()).onLoad(FooBar.class);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LoadedTypeInitializer.ForStaticField.class).apply();
    }

    @SuppressWarnings("unused")
    public static class Foo {

        public static Object foo;
    }

    @SuppressWarnings("unused")
    public static class Bar {

        private static Object foo;
    }

    @SuppressWarnings("unused")
    private static class Qux {

        public static Object foo;
    }

    @SuppressWarnings("unused")
    private static class Baz {

        String foo, bar;
    }

    @SuppressWarnings("unused")
    public static class FooBar {

        public static String foo;
    }
}
