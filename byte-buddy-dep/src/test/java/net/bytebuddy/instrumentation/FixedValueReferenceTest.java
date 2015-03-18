package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.CallTraceable;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotEquals;

public class FixedValueReferenceTest extends AbstractInstrumentationTest {

    private static final String BAR = "bar";

    private Bar bar;

    @Before
    public void setUp() throws Exception {
        bar = new Bar();
    }

    @Test
    public void testReferenceCall() throws Exception {
        assertType(instrument(Foo.class, FixedValue.reference(bar)));
    }

    @Test
    public void testValueCall() throws Exception {
        assertType(instrument(Foo.class, FixedValue.value(bar)));
    }

    private void assertType(DynamicType.Loaded<Foo> loaded) throws Exception {
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Foo instance = loaded.getLoaded().newInstance();
        assertNotEquals(Foo.class, instance.getClass());
        assertThat(instance, instanceOf(Foo.class));
        assertThat((Bar) loaded.getLoaded().getDeclaredMethod(BAR).invoke(instance), is(bar));
        instance.assertZeroCalls();
    }

    public static class Foo extends CallTraceable {

        public Bar bar() {
            register(BAR);
            return new Bar();
        }
    }

    public static class Bar {
        /* empty */
    }
}
