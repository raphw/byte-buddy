package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.Pipe;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.test.utility.CallTraceable;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationPipeTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final int BAZ = 42;

    @Test
    public void testPipeToIdenticalType() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(new ForwardingInterceptor(new Foo(FOO))).defineParameterBinder(Pipe.Binder.install(ForwardingType.class)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(QUX), is(FOO + QUX));
    }

    @Test
    public void testPipeToIdenticalTypeVoid() throws Exception {
        DynamicType.Loaded<Qux> loaded = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(MethodDelegation.to(new ForwardingInterceptor(new Qux())).defineParameterBinder(Pipe.Binder.install(ForwardingType.class)))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Qux instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        instance.assertZeroCalls();
    }

    @Test
    public void testPipeToIdenticalTypePrimitive() throws Exception {
        DynamicType.Loaded<Baz> loaded = new ByteBuddy()
                .subclass(Baz.class)
                .method(isDeclaredBy(Baz.class))
                .intercept(MethodDelegation.to(new PrimitiveForwardingInterceptor(new Baz())).defineParameterBinder(Pipe.Binder.install(ForwardingType.class)))
                .make()
                .load(Baz.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Baz instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(BAZ), is(BAZ * 2L));
        instance.assertZeroCalls();
    }

    @Test
    public void testPipeToSubtype() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(new ForwardingInterceptor(new Bar(FOO))).defineParameterBinder(Pipe.Binder.install(ForwardingType.class)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(QUX), is(FOO + QUX));
    }

    @Test
    public void testPipeSerialization() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(new SerializableForwardingInterceptor(new Foo(FOO)))
                        .defineParameterBinder(Pipe.Binder.install(ForwardingType.class)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(QUX), is(FOO + QUX));
    }

    @Test(expected = ClassCastException.class)
    public void testPipeToIncompatibleTypeThrowsException() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(new ForwardingInterceptor(new Object())).defineParameterBinder(Pipe.Binder.install(ForwardingType.class)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
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
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.to(WrongParameterTypeTarget.class).defineParameterBinder(Pipe.Binder.install(ForwardingType.class)))
                .make();
    }

    public interface ForwardingType<T, S> {

        S doPipe(T target);
    }

    public interface InheritingForwardingType extends ForwardingType<Object, Object> {
        /* empty */
    }

    interface PackagePrivateForwardingType<T, S> {

        S doPipe(T target);
    }

    public interface WrongParametersForwardingType<T extends Number, S> {

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

    public static class Qux extends CallTraceable {

        public void foo() {
            register(FOO);
        }
    }

    public static class PrimitiveForwardingInterceptor {

        private final Object target;

        public PrimitiveForwardingInterceptor(Object target) {
            this.target = target;
        }

        @RuntimeType
        public Object intercept(@Pipe ForwardingType<Object, Object> pipe) {
            assertThat(pipe, not(instanceOf(Serializable.class)));
            return pipe.doPipe(target);
        }
    }

    public static class Baz extends CallTraceable {

        public long foo(int value) {
            register(FOO);
            return value * 2L;
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
