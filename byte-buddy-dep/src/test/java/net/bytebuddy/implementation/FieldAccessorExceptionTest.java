package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class FieldAccessorExceptionTest {

    @Test(expected = IllegalArgumentException.class)
    public void testFinalFieldSetter() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(FieldAccessor.ofBeanProperty())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNoVisibleField() throws Exception {
        new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(FieldAccessor.ofBeanProperty())
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldNoBeanMethodName() throws Exception {
        new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(FieldAccessor.ofBeanProperty())
                .make();
    }

    @SuppressWarnings("unused")
    public static class Foo {

        protected final Object foo = null;

        public void setFoo(Object o) {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class Bar {

        private Object bar = null;

        public void setBar(Object o) {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class Qux {

        private Object qux = null;

        public void qux(Object o) {
            /* empty */
        }
    }
}
