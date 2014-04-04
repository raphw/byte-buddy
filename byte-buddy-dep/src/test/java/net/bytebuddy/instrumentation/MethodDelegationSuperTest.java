package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassInstrumentationContextDelegate;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Super;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MethodDelegationSuperTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    public static interface Qux {

        Object qux();
    }

    public static class Foo implements Qux {

        @Override
        public Object qux() {
            return FOO;
        }
    }

    public static class Baz {

        public static String baz(@Super Foo foo) {
            return foo.qux() + QUX;
        }
    }

    @Test
    public void testSuperInstance() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Baz.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    public static class FooBar {

        public static String baz(@Super Qux foo) {
            return foo.qux() + QUX;
        }
    }

    @Test
    public void testSuperInterface() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(FooBar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    public static class QuxBaz {

        public static String baz(@Super(strategy = Super.Instantiation.UNSAFE) Foo foo) {
            return foo.qux() + QUX;
        }
    }

    @Test
    public void testSuperInstanceUnsafe() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(QuxBaz.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    public static class Bar extends Foo {

        @Override
        public String qux() {
            return BAR;
        }
    }

    @Test
    public void testBridgeMethodResolution() throws Exception {
        DynamicType.Loaded<Bar> loaded = instrument(Bar.class, MethodDelegation.to(Baz.class));
        Bar instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is(BAR + QUX));
    }

    public static abstract class FooBarQuxBaz implements Qux {

        @Override
        public abstract Object qux();
    }

    @Test
    public void testSuperCallOnAbstractMethod() throws Exception {
        DynamicType.Loaded<FooBarQuxBaz> loaded = instrument(FooBarQuxBaz.class, MethodDelegation.to(FooBar.class));
        FooBarQuxBaz instance = loaded.getLoaded().newInstance();
        try {
            instance.qux();
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), startsWith(SubclassInstrumentationContextDelegate.ABSTRACT_METHOD_WARNING_PREFIX));
            assertEquals(RuntimeException.class, e.getClass());
        }
    }
}
