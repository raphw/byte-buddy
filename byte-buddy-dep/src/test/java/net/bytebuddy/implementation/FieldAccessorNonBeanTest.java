package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.CallTraceable;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldAccessorNonBeanTest extends AbstractImplementationTest {

    private static final String FOO = "foo";

    private static final String STRING_VALUE = "qux";

    private static final String STRING_DEFAULT_VALUE = "baz";

    @Test
    public void testExplicitNameSetter() throws Exception {
        DynamicType.Loaded<SampleSetter> loaded = implement(SampleSetter.class, FieldAccessor.ofField(FOO));
        SampleSetter sampleSetter = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Field field = SampleSetter.class.getDeclaredField(FOO);
        field.setAccessible(true);
        assertThat(field.get(sampleSetter), is((Object) STRING_DEFAULT_VALUE));
        sampleSetter.bar(STRING_VALUE);
        assertThat(field.get(sampleSetter), is((Object) STRING_VALUE));
        sampleSetter.assertZeroCalls();
    }

    @Test
    public void testExplicitNameGetter() throws Exception {
        DynamicType.Loaded<SampleGetter> loaded = implement(SampleGetter.class, FieldAccessor.ofField(FOO));
        SampleGetter sampleGetter = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Field field = SampleGetter.class.getDeclaredField(FOO);
        field.setAccessible(true);
        assertThat(field.get(sampleGetter), is((Object) STRING_VALUE));
        assertThat(sampleGetter.bar(), is((Object) STRING_VALUE));
        assertThat(field.get(sampleGetter), is((Object) STRING_VALUE));
        sampleGetter.assertZeroCalls();
    }

    public static class SampleGetter extends CallTraceable {

        protected Object foo = STRING_VALUE;

        public Object bar() {
            register(FOO);
            return STRING_DEFAULT_VALUE;
        }
    }

    public static class SampleSetter extends CallTraceable {

        protected Object foo = STRING_DEFAULT_VALUE;

        public void bar(Object foo) {
            register(FOO, foo);
        }
    }
}
