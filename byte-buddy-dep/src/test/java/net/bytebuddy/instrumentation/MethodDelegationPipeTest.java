package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Pipe;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationPipeTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testPipeToIdenticalType() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(new ForwardingInterceptor(new Foo(FOO)))
                .defineParameterBinder(Pipe.Binder.install(ForwardingType.class)));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(QUX), is(FOO + QUX));
    }

    @Test
    public void testPipeToSubtype() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(new ForwardingInterceptor(new Bar(FOO)))
                .defineParameterBinder(Pipe.Binder.install(ForwardingType.class)));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(QUX), is(FOO + QUX));
    }

    @Test
    public void testPipeSerialization() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(new SerializableForwardingInterceptor(new Foo(FOO)))
                .defineParameterBinder(Pipe.Binder.install(ForwardingType.class)));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(QUX), is(FOO + QUX));
    }

    @Test(expected = ClassCastException.class)
    public void testPipeToIncompatibleTypeThrowsException() throws Exception {
        DynamicType.Loaded<Foo> loaded = instrument(Foo.class, MethodDelegation.to(new ForwardingInterceptor(new Object()))
                .defineParameterBinder(Pipe.Binder.install(ForwardingType.class)));
        Foo instance = loaded.getLoaded().newInstance();
        instance.foo(QUX);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeDoesNotDeclareCorrectMethodThrowsException() throws Exception {
        MethodDelegation.to(new ForwardingInterceptor(new Object()))
                .defineParameterBinder(Pipe.Binder.install(Serializable.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeDoesInheritFromOtherTypeThrowsException() throws Exception {
        MethodDelegation.to(new ForwardingInterceptor(new Object()))
                .defineParameterBinder(Pipe.Binder.install(InheritingForwardingType.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeIsNotPublicThrowsException() throws Exception {
        MethodDelegation.to(new ForwardingInterceptor(new Object()))
                .defineParameterBinder(Pipe.Binder.install(PackagePrivateForwardingType.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeDoesNotDeclareCorrectMethodSignatureThrowsException() throws Exception {
        MethodDelegation.to(new ForwardingInterceptor(new Object()))
                .defineParameterBinder(Pipe.Binder.install(WrongParametersForwardingType.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeIsNotInterfaceThrowsException() throws Exception {
        MethodDelegation.to(new ForwardingInterceptor(new Object()))
                .defineParameterBinder(Pipe.Binder.install(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testPipeTypeOnTargetInterceptorThrowsException() throws Exception {
        instrument(Foo.class, MethodDelegation.to(WrongParameterTypeTarget.class)
                .defineParameterBinder(Pipe.Binder.install(ForwardingType.class)));
    }

    public static interface ForwardingType<T, S> {

        S doPipe(T target);
    }

    public static interface InheritingForwardingType extends ForwardingType<Object, Object> {
        /* empty */
    }

    static interface PackagePrivateForwardingType<T, S> {

        S doPipe(T target);
    }

    public static interface WrongParametersForwardingType<T extends Number, S> {

        S doPipe(T target);
    }

    public static class ForwardingInterceptor {

        private final Object target;

        public ForwardingInterceptor(Object target) {
            this.target = target;
        }

        public String intercept(@Pipe ForwardingType<Object, String> pipe) {
            assertThat(pipe, not(instanceOf(Serializable.class)));
            return pipe.doPipe(target);
        }
    }

    public static class Foo {

        private final String prefix;

        public Foo() {
            prefix = BAR;
        }

        public Foo(String prefix) {
            this.prefix = prefix;
        }

        public String foo(String input) {
            return prefix + input;
        }
    }

    public static class Bar extends Foo {

        public Bar(String prefix) {
            super(prefix);
        }
    }

    public static class WrongParameterTypeTarget {

        public static String intercept(@Pipe Callable<?> pipe) {
            return null;
        }
    }

    public static class SerializableForwardingInterceptor {

        private final Object target;

        public SerializableForwardingInterceptor(Object target) {
            this.target = target;
        }

        public String intercept(@Pipe(serializableProxy = true) ForwardingType<Object, String> pipe) {
            assertThat(pipe, instanceOf(Serializable.class));
            return pipe.doPipe(target);
        }
    }
}
