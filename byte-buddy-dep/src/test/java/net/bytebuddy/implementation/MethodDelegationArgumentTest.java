package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.Argument;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationArgumentTest {

    private static final String FOO = "bar", QUX = "qux", BAZ = "baz";

    private static final int BAR = 42;

    @Test
    public void testArgument() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(Bar.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(FOO, BAR), is((Object) (QUX + FOO + BAR)));
    }

    @Test
    public void testHierarchyDelegation() throws Exception {
        new ByteBuddy()
                .subclass(Baz.class)
                .method(named("foo"))
                .intercept(MethodDelegation.to(new Qux()))
                .make()
                .load(Baz.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .foo();
    }

    public static class Foo {

        public Object foo(String s, Integer i) {
            return null;
        }
    }

    public static class Bar {

        public static String qux(@Argument(1) Integer i, @Argument(0) String s) {
            return QUX + s + i;
        }

        public static String baz(String s, Object o) {
            return BAZ + s + o;
        }
    }

    public static class Baz {

        public void foo() {

        }
    }

    public static class Qux extends Baz {

        public void foo() {
            super.foo();
        }
    }
}
