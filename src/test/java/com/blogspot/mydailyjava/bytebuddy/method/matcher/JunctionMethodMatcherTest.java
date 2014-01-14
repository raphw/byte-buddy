package com.blogspot.mydailyjava.bytebuddy.method.matcher;

import com.blogspot.mydailyjava.bytebuddy.method.JavaMethod;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JunctionMethodMatcherTest {

    @SuppressWarnings("unused")
    public static class TestClass {

        public void foo() {
            /* empty */
        }
    }

    private static final String FOO_METHOD_NAME = "foo";

    private Method testClass$foo;

    @Before
    public void setUp() throws Exception {
        testClass$foo = TestClass.class.getDeclaredMethod(FOO_METHOD_NAME);
    }

    @Test
    public void testAnd() throws Exception {
        assertThat(new JunctionMethodMatcher.Conjunction(MethodMatchers.named(FOO_METHOD_NAME),
                MethodMatchers.returns(void.class)).matches(new JavaMethod.ForMethod(testClass$foo)), is(true));
        assertThat(new JunctionMethodMatcher.Conjunction(MethodMatchers.named(FOO_METHOD_NAME),
                MethodMatchers.none()).matches(new JavaMethod.ForMethod(testClass$foo)), is(false));
    }

    @Test
    public void testOr() throws Exception {
        assertThat(new JunctionMethodMatcher.Disjunction(MethodMatchers.named(FOO_METHOD_NAME),
                MethodMatchers.returns(void.class)).matches(new JavaMethod.ForMethod(testClass$foo)), is(true));
        assertThat(new JunctionMethodMatcher.Disjunction(MethodMatchers.not(MethodMatchers.named(FOO_METHOD_NAME)),
                MethodMatchers.none()).matches(new JavaMethod.ForMethod(testClass$foo)), is(false));
    }
}
