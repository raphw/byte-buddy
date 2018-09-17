package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.CallTraceable;
import org.junit.Test;

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
                .intercept(InvocationHandlerAdapter.of(foo).withoutMethodCache())
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
    public void testStaticAdapterPrivileged() throws Exception {
        Foo foo = new Foo();
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(InvocationHandlerAdapter.of(foo).withoutMethodCache().withPrivilegedLookup())
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(1));
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
                .intercept(InvocationHandlerAdapter.of(qux).withoutMethodCache())
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
                .intercept(InvocationHandlerAdapter.of(foo))
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
                .defineField(QUX, InvocationHandler.class, Visibility.PUBLIC)
                .method(isDeclaredBy(Bar.class))
                .intercept(InvocationHandlerAdapter.toField(QUX).withoutMethodCache())
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Field field = loaded.getLoaded().getDeclaredField(QUX);
        assertThat(field.getModifiers(), is(Modifier.PUBLIC));
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
                .defineField(QUX, InvocationHandler.class, Visibility.PUBLIC)
                .method(isDeclaredBy(Bar.class))
                .intercept(InvocationHandlerAdapter.toField(QUX))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Field field = loaded.getLoaded().getDeclaredField(QUX);
        assertThat(field.getModifiers(), is(Modifier.PUBLIC));
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

    @Test(expected = IllegalStateException.class)
    public void testStaticMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(QUX, InvocationHandler.class)
                .defineMethod(FOO, void.class, Ownership.STATIC)
                .intercept(InvocationHandlerAdapter.toField(QUX))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testNonExistentField() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, void.class)
                .intercept(InvocationHandlerAdapter.toField(QUX))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleFieldType() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(QUX, Object.class)
                .defineMethod(FOO, void.class)
                .intercept(InvocationHandlerAdapter.toField(QUX))
                .make();
    }

    private static class Foo implements InvocationHandler {

        private final String marker;

        public List<Method> methods;

        private Foo() {
            marker = FOO;
            methods = new ArrayList<Method>();
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            methods.add(method);
            assertThat(args.length, is(1));
            assertThat(args[0], is((Object) FOO));
            assertThat(method.getName(), is(BAR));
            assertThat(proxy, instanceOf(Bar.class));
            return proxy;
        }

        @Override
        public int hashCode() {
            return marker.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && marker.equals(((Foo) other).marker);
        }
    }

    public static class Bar extends CallTraceable {

        public Object bar(Object o) {
            register(BAR);
            return o;
        }
    }

    private static class Qux implements InvocationHandler {

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
