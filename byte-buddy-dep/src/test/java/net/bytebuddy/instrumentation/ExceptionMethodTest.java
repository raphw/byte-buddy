package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.CallTraceable;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class ExceptionMethodTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    public static class Foo extends CallTraceable {

        public void foo() {
            register(FOO);
        }
    }

    @Test
    public void testWithoutMessage() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, ExceptionMethod.throwing(RuntimeException.class));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
        try {
            instance.foo();
            fail();
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertThat(e.getMessage(), nullValue());
        }
        instance.assertZeroCalls();
    }

    @Test
    public void testWithMessage() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, ExceptionMethod.throwing(RuntimeException.class, BAR));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
        try {
            instance.foo();
            fail();
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertThat(e.getMessage(), is(BAR));
        }
        instance.assertZeroCalls();
    }

    @Test
    public void testWithNonDeclaredCheckedException() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, ExceptionMethod.throwing(Exception.class));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(0));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
        try {
            instance.foo();
            fail();
        } catch (Exception e) {
            assertEquals(Exception.class, e.getClass());
            assertThat(e.getMessage(), nullValue());
        }
        instance.assertZeroCalls();
    }

    @Test
    public void testEqualsHashCode() throws Exception {
        assertThat(ExceptionMethod.throwing(RuntimeException.class).hashCode(), is(ExceptionMethod.throwing(RuntimeException.class).hashCode()));
        assertThat(ExceptionMethod.throwing(RuntimeException.class), is(ExceptionMethod.throwing(RuntimeException.class)));
        assertThat(ExceptionMethod.throwing(RuntimeException.class).hashCode(), not(is(ExceptionMethod.throwing(Exception.class).hashCode())));
        assertThat(ExceptionMethod.throwing(RuntimeException.class), not(is(ExceptionMethod.throwing(Exception.class))));
        assertThat(ExceptionMethod.throwing(RuntimeException.class).hashCode(), not(is(ExceptionMethod.throwing(RuntimeException.class, FOO).hashCode())));
        assertThat(ExceptionMethod.throwing(RuntimeException.class), not(is(ExceptionMethod.throwing(RuntimeException.class, FOO))));
        assertThat(ExceptionMethod.throwing(RuntimeException.class, FOO).hashCode(), is(ExceptionMethod.throwing(RuntimeException.class, FOO).hashCode()));
        assertThat(ExceptionMethod.throwing(RuntimeException.class, FOO), is(ExceptionMethod.throwing(RuntimeException.class, FOO)));
        assertThat(ExceptionMethod.throwing(RuntimeException.class, FOO).hashCode(), not(is(ExceptionMethod.throwing(Exception.class, FOO).hashCode())));
        assertThat(ExceptionMethod.throwing(RuntimeException.class, FOO), not(is(ExceptionMethod.throwing(Exception.class, FOO))));
        assertThat(ExceptionMethod.throwing(RuntimeException.class, FOO).hashCode(), not(is(ExceptionMethod.throwing(RuntimeException.class, BAR).hashCode())));
        assertThat(ExceptionMethod.throwing(RuntimeException.class, FOO), not(is(ExceptionMethod.throwing(RuntimeException.class, BAR))));
    }
}
