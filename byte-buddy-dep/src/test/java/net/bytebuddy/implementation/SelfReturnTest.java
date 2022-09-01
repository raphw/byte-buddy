package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class SelfReturnTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testSelfReturn() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(SelfReturn.INSTANCE)
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(Foo.class)));
        assertThat(instance, instanceOf(Foo.class));
        assertThat(instance.foo(), sameInstance(instance));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonAssignableThrowsException() {
        new ByteBuddy()
                .subclass(Object.class)
                .method(isToString())
                .intercept(SelfReturn.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotUseOnConstructor() {
        new ByteBuddy()
                .subclass(Foo.class)
                .constructor(any())
                .intercept(SelfReturn.INSTANCE)
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotUseOnStaticMethod() {
        new ByteBuddy()
                .subclass(Foo.class)
                .defineMethod(BAR, Foo.class, Ownership.STATIC)
                .intercept(SelfReturn.INSTANCE)
                .make();
    }

    public static class Foo {

        public Foo foo() {
            return null;
        }
    }
}
