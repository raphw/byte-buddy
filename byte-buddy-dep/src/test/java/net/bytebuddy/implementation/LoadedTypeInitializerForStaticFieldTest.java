package net.bytebuddy.implementation;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LoadedTypeInitializerForStaticFieldTest {

    private static final String FOO = "foo", BAR = "bar";

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
    public void testDeserialization() throws Exception {
        LoadedTypeInitializer.ForStaticField original = new LoadedTypeInitializer.ForStaticField(FOO, BAR);
        ByteArrayOutputStream serialization = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(serialization);
        try {
            outputStream.writeObject(original);
        } finally {
            outputStream.close();
        }
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(serialization.toByteArray()));
        try {
            Object loadedTypeInitializer = inputStream.readObject();
            assertThat(loadedTypeInitializer, instanceOf(LoadedTypeInitializer.ForStaticField.class));
            assertThat(loadedTypeInitializer, hasPrototype((Object) original));
        } finally {
            inputStream.close();
        }
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
