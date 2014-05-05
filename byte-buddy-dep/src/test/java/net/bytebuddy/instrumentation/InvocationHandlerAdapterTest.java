package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.CallTraceable;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class InvocationHandlerAdapterTest extends AbstractInstrumentationTest {

    public static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testStaticAdapter() throws Exception {
        DynamicType.Loaded<Bar> loaded = instrument(Bar.class, InvocationHandlerAdapter.of(new Foo()));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Bar instance = loaded.getLoaded().newInstance();
        assertThat(instance.bar(FOO), is((Object) instance));
        instance.assertZeroCalls();
    }

    @Test
    public void testInstanceAdapter() throws Exception {
        DynamicType.Loaded<Bar> loaded = instrument(Bar.class, InvocationHandlerAdapter.of(QUX));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Field field = loaded.getLoaded().getDeclaredField(QUX);
        assertThat(field.getModifiers(), is(Modifier.PUBLIC));
        field.setAccessible(true);
        Bar instance = loaded.getLoaded().newInstance();
        field.set(instance, new Foo());
        assertThat(instance.bar(FOO), is((Object) instance));
        instance.assertZeroCalls();
    }

    @Test
    public void testEqualsHashCodeStaticAdapter() throws Exception {
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO)).hashCode(), is(InvocationHandlerAdapter.of(new Foo(FOO)).hashCode()));
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO)), is(InvocationHandlerAdapter.of(new Foo(FOO))));
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO)).hashCode(), not(is(InvocationHandlerAdapter.of(new Foo(BAR)).hashCode())));
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO)), not(is(InvocationHandlerAdapter.of(new Foo(BAR)))));
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO)).hashCode(), not(is(InvocationHandlerAdapter.of(new Foo(FOO), QUX).hashCode())));
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO)), not(is(InvocationHandlerAdapter.of(new Foo(FOO), QUX))));
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO), QUX).hashCode(), not(is(InvocationHandlerAdapter.of(QUX).hashCode())));
        assertThat(InvocationHandlerAdapter.of(new Foo(FOO), QUX), not(is(InvocationHandlerAdapter.of(QUX))));
    }

    @Test
    public void testEqualsHashCodeInstanceAdapter() throws Exception {
        assertThat(InvocationHandlerAdapter.of(QUX).hashCode(), is(InvocationHandlerAdapter.of(QUX).hashCode()));
        assertThat(InvocationHandlerAdapter.of(QUX), is(InvocationHandlerAdapter.of(QUX)));
        assertThat(InvocationHandlerAdapter.of(QUX).hashCode(), not(is(InvocationHandlerAdapter.of(FOO).hashCode())));
        assertThat(InvocationHandlerAdapter.of(QUX), not(is(InvocationHandlerAdapter.of(FOO))));
        assertThat(InvocationHandlerAdapter.of(QUX).hashCode(), not(is(InvocationHandlerAdapter.of(new Foo(BAR), QUX).hashCode())));
        assertThat(InvocationHandlerAdapter.of(QUX), not(is(InvocationHandlerAdapter.of(new Foo(BAR), QUX))));
    }

    private static class Foo implements InvocationHandler {

        private final String marker;

        private Foo() {
            marker = FOO;
        }

        private Foo(String marker) {
            this.marker = marker;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            assertThat(args.length, is(1));
            assertThat(args[0], is((Object) FOO));
            assertThat(method.getName(), is(BAR));
            assertThat(proxy, instanceOf(Bar.class));
            return proxy;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && marker.equals(((Foo) other).marker);
        }

        @Override
        public int hashCode() {
            return marker.hashCode();
        }
    }

    public static class Bar extends CallTraceable {

        public Object bar(Object o) {
            register(BAR);
            return o;
        }
    }
}
