package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class MethodDelegationCheckedExceptionTest {

    @Test(expected = Exception.class)
    public void testUndeclaredCheckedException() throws Exception {
        new ByteBuddy().subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(Foo.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .bar();
    }

    @SuppressWarnings("unused")
    public static class Foo {

        public static void doThrow() throws Exception {
            throw new Exception();
        }

        public void bar() {
            /* do nothing */
        }
    }
}
