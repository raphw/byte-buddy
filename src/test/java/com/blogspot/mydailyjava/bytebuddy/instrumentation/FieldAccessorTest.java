package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldAccessorTest extends AbstractInstrumentationTest {

    public static class Foo {

        public int foo = 10;

        public int getFoo() {
            return 0;
        }
    }

    @Test
    public void testFieldAccessor() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, FieldAccessor.ofBeanProperty());
        Foo proxy = loaded.getLoaded().newInstance();
        assertThat(proxy.getFoo(), is(10));
    }
}
