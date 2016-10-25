package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationBindingPriorityTest {

    private static final String FOO = "FOO", BAR = "bar";

    private static final int PRIORITY = 10;

    @Test
    public void testBindingPriority() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(Bar.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(FOO));
    }

    public static class Foo {

        public String foo() {
            return null;
        }
    }

    public static class Bar {

        @BindingPriority(PRIORITY)
        public static String bar() {
            return FOO;
        }

        public static String qux() {
            return BAR;
        }
    }
}
