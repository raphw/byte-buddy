package net.bytebuddy.instrumentation.method.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Method;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JunctionMethodMatcherTest {


    private static final String FOO_METHOD_NAME = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private Method testClass$foo;

    @Mock
    private MethodMatcher first, second;

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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(JunctionMethodMatcher.Conjunction.class).skipToString().apply();
        ObjectPropertyAssertion.of(JunctionMethodMatcher.Disjunction.class).skipToString().apply();
    }

    @SuppressWarnings("unused")
    public static class MatchedClass {

        public void foo() {
            /* empty */
        }
    }
}
