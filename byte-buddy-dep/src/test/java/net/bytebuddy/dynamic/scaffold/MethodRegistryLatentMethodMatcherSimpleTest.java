package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodRegistryLatentMethodMatcherSimpleTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodMatcher first, second;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodRegistry.LatentMethodMatcher.Simple(first).hashCode(),
                is(new MethodRegistry.LatentMethodMatcher.Simple(first).hashCode()));
        assertThat(new MethodRegistry.LatentMethodMatcher.Simple(first),
                is(new MethodRegistry.LatentMethodMatcher.Simple(first)));
        assertThat(new MethodRegistry.LatentMethodMatcher.Simple(first).hashCode(),
                not(is(new MethodRegistry.LatentMethodMatcher.Simple(second).hashCode())));
        assertThat(new MethodRegistry.LatentMethodMatcher.Simple(first),
                not(is(new MethodRegistry.LatentMethodMatcher.Simple(second))));
    }
}
