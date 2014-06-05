package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Super;
import org.junit.Test;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testSuperInstance() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(Baz.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testSuperInterface() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(FooBar.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testSuperInstanceUnsafe() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(QuxBaz.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testBridgeMethodResolution() throws Exception {
        DynamicType.Loaded<Bar> loaded = instrument(Bar.class, MethodDelegation.to(GenericBaz.class));
        Bar instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(BAR), is(BAR + QUX));
    }

    @Test(expected = AbstractMethodError.class)
    public void testSuperCallOnAbstractMethod() throws Exception {
        DynamicType.Loaded<FooBarQuxBaz> loaded = instrument(FooBarQuxBaz.class, MethodDelegation.to(FooBar.class));
        loaded.getLoaded().newInstance().qux();
    }

    @Test
    public void testSerializableProxy() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(SerializationCheck.class));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

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
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux() + QUX;
        }
    }

    public static class FooBar {

        public static String baz(@Super Qux foo) {
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux() + QUX;
        }
    }

    public static class QuxBaz {

        public static String baz(@Super(strategy = Super.Instantiation.UNSAFE) Foo foo) {
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux() + QUX;
        }
    }

    public static abstract class FooBarQuxBaz implements Qux {

        @Override
        public abstract Object qux();
    }

    public static class GenericBase<T> {

        public T qux(T value) {
            return value;
        }
    }

    public static class Bar extends GenericBase<String> {

        @Override
        public String qux(String value) {
            return super.qux(value);
        }
    }

    public static class GenericBaz {

        public static String baz(@Super GenericBase<String> foo, String value) {
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux(value) + QUX;
        }
    }

    public static class SerializationCheck {
        public static String baz(@Super(serializableProxy = true) Foo foo) {
            assertThat(foo, instanceOf(Serializable.class));
            return foo.qux() + QUX;
        }

    }
}
