package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class InvocationHandlerAdapterTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final int BAZ = 42;

    @Test
    public void testStaticAdapterWithoutCache() throws Exception {
        Foo foo = new Foo();
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(InvocationHandlerAdapter.of(foo))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(1));
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(2));
        assertThat(foo.methods.get(0), not(sameInstance(foo.methods.get(1))));
        instance.assertZeroCalls();
    }

    @Test
    public void testStaticAdapterWithoutCacheForPrimitiveValue() throws Exception {
        Qux qux = new Qux();
        DynamicType.Loaded<Baz> loaded = new ByteBuddy()
                .subclass(Baz.class)
                .method(isDeclaredBy(Baz.class))
                .intercept(InvocationHandlerAdapter.of(qux))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Baz instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.bar(BAZ), is(BAZ * 2L));
        instance.assertZeroCalls();
    }

    @Test
    public void testStaticAdapterWithMethodCache() throws Exception {
        Foo foo = new Foo();
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(InvocationHandlerAdapter.of(foo).withMethodCache())
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(1));
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(2));
        assertThat(foo.methods.get(0), sameInstance(foo.methods.get(1)));
        instance.assertZeroCalls();
    }

    @Test
    public void testInstanceAdapterWithoutCache() throws Exception {
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(InvocationHandlerAdapter.toInstanceField(QUX))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Field field = loaded.getLoaded().getDeclaredField(QUX);
        assertThat(field.getModifiers(), is(Modifier.PUBLIC | Opcodes.ACC_SYNTHETIC));
        field.setAccessible(true);
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Foo foo = new Foo();
        field.set(instance, foo);
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(1));
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(2));
        assertThat(foo.methods.get(0), not(sameInstance(foo.methods.get(1))));
        instance.assertZeroCalls();
    }

    @Test
    public void testInstanceAdapterWithMethodCache() throws Exception {
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(InvocationHandlerAdapter.toInstanceField(QUX).withMethodCache())
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Field field = loaded.getLoaded().getDeclaredField(QUX);
        assertThat(field.getModifiers(), is(Modifier.PUBLIC | Opcodes.ACC_SYNTHETIC));
        field.setAccessible(true);
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Foo foo = new Foo();
        field.set(instance, foo);
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(1));
        assertThat(instance.bar(FOO), is((Object) instance));
        assertThat(foo.methods.size(), is(2));
        assertThat(foo.methods.get(0), sameInstance(foo.methods.get(1)));
        instance.assertZeroCalls();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InvocationHandlerAdapter.ForInstanceDelegation.class).apply();
        ObjectPropertyAssertion.of(InvocationHandlerAdapter.ForInstanceDelegation.Appender.class).apply();
        ObjectPropertyAssertion.of(InvocationHandlerAdapter.ForStaticDelegation.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(InvocationHandlerAdapter.ForStaticDelegation.Appender.class).apply();
    }

    private static class Foo implements InvocationHandler {

        private final String marker;

        public List<Method> methods;

        private Foo() {
            marker = FOO;
            methods = new ArrayList<Method>();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            methods.add(method);
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

    private static class Qux implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return ((Integer) args[0]) * 2L;
        }
    }

    public static class Baz extends CallTraceable {

        public long bar(int o) {
            register(BAR);
            return o;
        }
    }
}
