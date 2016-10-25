package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.StubValue;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationStubValueTest {

    @Test
    public void testVoidMethod() throws Exception {
        DynamicType.Loaded<VoidMethod> loaded = new ByteBuddy()
                .subclass(VoidMethod.class)
                .method(isDeclaredBy(VoidMethod.class))
                .intercept(MethodDelegation.to(new Interceptor(null)))
                .make()
                .load(VoidMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        VoidMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
    }

    @Test
    public void testReference() throws Exception {
        DynamicType.Loaded<ReferenceMethod> loaded = new ByteBuddy()
                .subclass(ReferenceMethod.class)
                .method(isDeclaredBy(ReferenceMethod.class))
                .intercept(MethodDelegation.to(new Interceptor(null)))
                .make()
                .load(ReferenceMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ReferenceMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), nullValue(Object.class));
    }

    @Test
    public void testLongValue() throws Exception {
        DynamicType.Loaded<LongMethod> loaded = new ByteBuddy()
                .subclass(LongMethod.class)
                .method(isDeclaredBy(LongMethod.class))
                .intercept(MethodDelegation.to(new Interceptor(0L)))
                .make()
                .load(LongMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        LongMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(0L));
    }

    @Test
    public void tesIntegerValue() throws Exception {
        DynamicType.Loaded<IntegerMethod> loaded = new ByteBuddy()
                .subclass(IntegerMethod.class)
                .method(isDeclaredBy(IntegerMethod.class))
                .intercept(MethodDelegation.to(new Interceptor(0)))
                .make()
                .load(LongMethod.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        IntegerMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(0));
    }

    public static class VoidMethod {

        public void foo() {
            throw new AssertionError();
        }
    }

    public static class ReferenceMethod {

        public Object foo() {
            throw new AssertionError();
        }
    }

    public static class LongMethod {

        public long foo() {
            throw new AssertionError();
        }
    }

    public static class IntegerMethod {

        public int foo() {
            throw new AssertionError();
        }
    }

    public static class Interceptor {

        private final Object expectedValue;

        public Interceptor(Object expectedValue) {
            this.expectedValue = expectedValue;
        }

        @RuntimeType
        public Object intercept(@StubValue Object value) {
            if (expectedValue == null) {
                assertThat(value, nullValue());
            } else {
                assertThat(value, is(expectedValue));
            }
            return value;
        }
    }
}
