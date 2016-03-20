package net.bytebuddy.implementation;

import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldAccessorExceptionTest extends AbstractImplementationTest {

    private static final String FOO = "foo";

    @Test(expected = IllegalArgumentException.class)
    public void testFinalFieldSetter() throws Exception {
        implement(Foo.class, FieldAccessor.ofBeanProperty());
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNoVisibleField() throws Exception {
        implement(Bar.class, FieldAccessor.ofBeanProperty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldNoBeanMethodName() throws Exception {
        implement(Qux.class, FieldAccessor.ofBeanProperty());
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonStaticOnInterface() throws Exception {
        InstrumentedType instrumentedType = mock(InstrumentedType.class);
        when(instrumentedType.isInterface()).thenReturn(true);
        FieldAccessor.ofField(FOO).defineAs(String.class, Visibility.PUBLIC).prepare(instrumentedType);
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonPublicOnInterface() throws Exception {
        InstrumentedType instrumentedType = mock(InstrumentedType.class);
        when(instrumentedType.isInterface()).thenReturn(true);
        FieldAccessor.ofField(FOO).defineAs(String.class, Ownership.STATIC).prepare(instrumentedType);
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
