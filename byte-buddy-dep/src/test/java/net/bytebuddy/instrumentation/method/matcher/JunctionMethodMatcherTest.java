package net.bytebuddy.instrumentation.method.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JunctionMethodMatcherTest {

    private static final String FOO_METHOD_NAME = "foo";
    private Method testClass$foo;

    @Before
    public void setUp() throws Exception {
        testClass$foo = MatchedClass.class.getDeclaredMethod(FOO_METHOD_NAME);
    }

    @Test
    public void testAnd() throws Exception {
        assertThat(new JunctionMethodMatcher.Conjunction(MethodMatchers.named(FOO_METHOD_NAME),
                MethodMatchers.returns(void.class)).matches(new MethodDescription.ForLoadedMethod(testClass$foo)), is(true));
        assertThat(new JunctionMethodMatcher.Conjunction(MethodMatchers.named(FOO_METHOD_NAME),
                MethodMatchers.none()).matches(new MethodDescription.ForLoadedMethod(testClass$foo)), is(false));
    }

    @Test
    public void testOr() throws Exception {
        assertThat(new JunctionMethodMatcher.Disjunction(MethodMatchers.named(FOO_METHOD_NAME),
                MethodMatchers.returns(void.class)).matches(new MethodDescription.ForLoadedMethod(testClass$foo)), is(true));
        assertThat(new JunctionMethodMatcher.Disjunction(MethodMatchers.not(MethodMatchers.named(FOO_METHOD_NAME)),
                MethodMatchers.none()).matches(new MethodDescription.ForLoadedMethod(testClass$foo)), is(false));
    }

    @SuppressWarnings("unused")
    public static class MatchedClass {

        public void foo() {
            /* empty */
        }
    }
}
