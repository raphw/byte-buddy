package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.StubValue;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationStubValueTest extends AbstractImplementationTest {

    private static final String FOO = "FOO";

    @Test
    public void testVoidMethod() throws Exception {
        DynamicType.Loaded<VoidMethod> loaded = implement(VoidMethod.class, MethodDelegation.to(new Interceptor(null)));
        VoidMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
    }

    @Test
    public void testReference() throws Exception {
        DynamicType.Loaded<ReferenceMethod> loaded = implement(ReferenceMethod.class, MethodDelegation.to(new Interceptor(null)));
        ReferenceMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), nullValue(Object.class));
    }

    @Test
    public void testLongValue() throws Exception {
        DynamicType.Loaded<LongMethod> loaded = implement(LongMethod.class, MethodDelegation.to(new Interceptor(0L)));
        LongMethod instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(), is(0L));
    }

    @Test
    public void tesIntegerValue() throws Exception {
        DynamicType.Loaded<IntegerMethod> loaded = implement(IntegerMethod.class, MethodDelegation.to(new Interceptor(0)));
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
