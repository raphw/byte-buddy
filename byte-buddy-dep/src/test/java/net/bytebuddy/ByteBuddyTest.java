package net.bytebuddy;

import net.bytebuddy.dynamic.TypeResolver;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Modifier;

import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ByteBuddyTest {

    @Test(expected = IllegalArgumentException.class)
    public void testEnumWithoutValuesIsIllegal() throws Exception {
        new ByteBuddy().makeEnumeration();
    }

    @Test
    public void testEnumeration() throws Exception {
        Class<?> type = new ByteBuddy()
                .makeEnumeration("foo")
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(Modifier.isPublic(type.getModifiers()), is(true));
        assertThat(type.isEnum(), is(true));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testInterface() throws Exception {
        Class<?> type = new ByteBuddy()
                .makeInterface()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(Modifier.isPublic(type.getModifiers()), is(true));
        assertThat(type.isEnum(), is(false));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(false));
    }

    @Test
    public void testAnnotation() throws Exception {
        Class<?> type = new ByteBuddy()
                .makeAnnotation()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(Modifier.isPublic(type.getModifiers()), is(true));
        assertThat(type.isEnum(), is(false));
        assertThat(type.isInterface(), is(true));
        assertThat(type.isAnnotation(), is(true));
    }

    @Test
    public void testTypeInitializerInstrumentation() throws Exception {
        Recorder recorder = new Recorder();
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .invokable(isTypeInitializer())
                .intercept(MethodDelegation.to(recorder))
                .make(TypeResolver.Active.INSTANCE)
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.newInstance(), instanceOf(type));
        assertThat(recorder.counter, is(1));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddy.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.EnumerationImplementation.class).apply();
    }

    public static class Recorder {

        public int counter;

        public void instrument() {
            counter++;
        }
    }
}
