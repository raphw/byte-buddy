package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ForwardingTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testStaticInstanceForwarding() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(Forwarding.to(new Bar()))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
    }

    @Test
    public void testInstanceFieldForwarding() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .defineField(FOO, Foo.class, Visibility.PUBLIC)
                .method(isDeclaredBy(Foo.class))
                .intercept(Forwarding.toField(FOO))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Field field = loaded.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(instance, new Bar());
        assertThat(instance.foo(), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
    }

    @Test
    public void testStaticFieldForwarding() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .defineField(FOO, Foo.class, Visibility.PUBLIC, Ownership.STATIC)
                .method(isDeclaredBy(Foo.class))
                .intercept(Forwarding.toField(FOO))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Field field = loaded.getLoaded().getDeclaredField(FOO);
        field.setAccessible(true);
        field.set(null, new Bar());
        assertThat(instance.foo(), is(BAR));
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
    }

    @Test
    public void testForwardingComposition() throws Exception {
        Counter first = new Counter(), second = new Counter();
        DynamicType.Loaded<Counter> loaded = new ByteBuddy()
                .subclass(Counter.class)
                .method(isDeclaredBy(Counter.class))
                .intercept(Forwarding.to(first).andThen(Forwarding.to(second)))
                .make()
                .load(Counter.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Counter counter = loaded.getLoaded().getDeclaredConstructor().newInstance();
        counter.count();
        assertThat(counter.value, is(0));
        assertThat(first.value, is(1));
        assertThat(second.value, is(1));
    }

    @Test(expected = IllegalStateException.class)
    public void testDifferentInstanceForwardingThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .defineField(FOO, Qux.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(Forwarding.toField(FOO))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticForwardingThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, void.class, Ownership.STATIC)
                .intercept(Forwarding.to(new Object()))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTypeThrowsException() throws Exception {
        Forwarding.to("foo", Integer.class);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Forwarding.class).apply();
        ObjectPropertyAssertion.of(Forwarding.PreparationHandler.ForField.class).apply();
        ObjectPropertyAssertion.of(Forwarding.PreparationHandler.ForInstance.class).apply();
        ObjectPropertyAssertion.of(Forwarding.TerminationHandler.class).apply();
        ObjectPropertyAssertion.of(Forwarding.Appender.class).skipSynthetic().apply();
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

    public static class Counter {

        public int value;

        public void count() {
            value++;
        }
    }
}
