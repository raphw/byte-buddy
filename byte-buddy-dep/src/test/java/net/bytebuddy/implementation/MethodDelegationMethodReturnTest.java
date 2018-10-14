package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationMethodReturnTest {

    private final static String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testDelegationToMethodReturn() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(SampleTarget.class)
                .method(named(BAR))
                .intercept(MethodDelegation.toMethodReturnOf(FOO))
                .make()
                .load(SampleTarget.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getConstructor()
                .newInstance()
                .bar(), is(42));
    }

    @Test
    public void testDelegationToMethodReturnStatic() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(SampleTargetStatic.class)
                .defineMethod(FOO, SampleDelegation.class, Ownership.STATIC)
                .intercept(FixedValue.value(new SampleDelegation(42)))
                .method(named(BAR))
                .intercept(MethodDelegation.toMethodReturnOf(FOO))
                .make()
                .load(SampleTargetStatic.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getConstructor()
                .newInstance()
                .bar(), is(42));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalTargetNoMatch() throws Exception {
        new ByteBuddy()
                .subclass(IllegalTarget.class)
                .method(named(BAR))
                .intercept(MethodDelegation.toMethodReturnOf(FOO))
                .make();
    }

    public static class SampleTarget {

        public SampleDelegation foo() {
            return new SampleDelegation(42);
        }

        public int bar() {
            throw new AssertionError();
        }
    }

    public static class SampleTargetStatic {

        public int bar() {
            throw new AssertionError();
        }
    }

    public static class IllegalTarget {

        public SampleDelegation foo(int value) {
            return new SampleDelegation(value);
        }

        public int bar() {
            throw new AssertionError();
        }
    }

    public static class SampleDelegation {

        private final int value;

        public SampleDelegation(int value) {
            this.value = value;
        }

        public int foo() {
            return value;
        }
    }
}
