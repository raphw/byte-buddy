package net.bytebuddy.instrumentation;

import org.junit.Test;

public class FieldAccessorExceptionTest extends AbstractInstrumentationTest {

    @Test(expected = IllegalArgumentException.class)
    public void testFinalFieldSetter() throws Exception {
        instrument(Foo.class, FieldAccessor.ofBeanProperty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldNoVisibleField() throws Exception {
        instrument(Bar.class, FieldAccessor.ofBeanProperty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldNoBeanMethodName() throws Exception {
        instrument(Qux.class, FieldAccessor.ofBeanProperty());
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
