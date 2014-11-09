package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Morph;
import org.junit.Test;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationMorphTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testMorph() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(SimpleMorph.class)
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO), is(QUX + BAR));
    }

    @Test
    public void testMorphSerializable() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(SimpleMorphSerializable.class)
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO), is(QUX + BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testMorphIllegal() throws Exception {
        instrument(Foo.class, MethodDelegation.to(SimpleMorphIllegal.class)
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
    }

    public static class Foo {

        public String foo(String foo) {
            return foo + BAR;
        }
    }

    public static class SimpleMorph {

        public static String intercept(@Morph Morphing<String> morphing) {
            assertThat(morphing, not(instanceOf(Serializable.class)));
            return morphing.morph(QUX);
        }
    }

    public static class SimpleMorphSerializable {

        public static String intercept(@Morph(serializableProxy = true) Morphing<String> morphing) {
            assertThat(morphing, instanceOf(Serializable.class));
            return morphing.morph(QUX);
        }
    }

    public static class SimpleMorphIllegal {

        public static String intercept(@Morph Void morphing) {
            return null;
        }
    }

    public static interface Morphing<T> {

        T morph(Object... arguments);
    }
}
