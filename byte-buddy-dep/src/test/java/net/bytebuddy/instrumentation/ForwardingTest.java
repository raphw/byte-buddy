package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotEquals;

public class ForwardingTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testStaticInstanceDelegation() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, Forwarding.to(new Bar()));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
    }

    @Test
    public void testInstanceFieldDelegation() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, Forwarding.toInstanceField(FOO, Foo.class));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().newInstance();
        Field field = loaded.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, new Bar());
        assertThat(instance.foo(), is(BAR));
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
    }

    @Test
    public void testStaticFieldDelegation() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, Forwarding.toStaticField(FOO, Foo.class));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().newInstance();
        Field field = loaded.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(null, new Bar());
        assertThat(instance.foo(), is(BAR));
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        Foo foo = new Foo();
        assertThat(Forwarding.to(foo).hashCode(), is(Forwarding.to(foo).hashCode()));
        assertThat(Forwarding.to(foo), is(Forwarding.to(foo)));
        assertThat(Forwarding.to(foo).hashCode(), not(is(Forwarding.to(new Foo()).hashCode())));
        assertThat(Forwarding.to(foo), not(is(Forwarding.to(new Foo()))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDifferentInstanceForwardingThrowsException() throws Exception {
        instrument(Foo.class, Forwarding.toStaticField(FOO, Qux.class));
    }

    public static class Foo {

        public String foo() {
            return FOO;
        }
    }

    public static class Bar extends Foo {

        @Override
        public String foo() {
            return BAR;
        }
    }

    public static class Qux {
        /* empty */
    }
}
