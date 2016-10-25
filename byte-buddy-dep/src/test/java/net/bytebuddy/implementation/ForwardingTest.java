package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                .method(isDeclaredBy(Foo.class))
                .intercept(Forwarding.toInstanceField(FOO, Foo.class))
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
                .method(isDeclaredBy(Foo.class))
                .intercept(Forwarding.toStaticField(FOO, Foo.class))
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

    @Test(expected = IllegalStateException.class)
    public void testInstanceFieldForwardingForInterfaceThrowsException() throws Exception {
        InstrumentedType instrumentedType = mock(InstrumentedType.class);
        when(instrumentedType.isInterface()).thenReturn(true);
        Forwarding.toInstanceField(FOO, Foo.class).prepare(instrumentedType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDifferentInstanceForwardingThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(Forwarding.toStaticField(FOO, Qux.class))
                .make();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Forwarding.class).apply();
        ObjectPropertyAssertion.of(Forwarding.PreparationHandler.ForInstanceField.class).apply();
        ObjectPropertyAssertion.of(Forwarding.PreparationHandler.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(Forwarding.PreparationHandler.ForStaticInstance.class).apply();
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
}
