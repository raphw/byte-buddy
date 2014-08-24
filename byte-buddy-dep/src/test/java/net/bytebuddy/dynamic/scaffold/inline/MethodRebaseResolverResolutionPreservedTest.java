package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class MethodRebaseResolverResolutionPreservedTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription, otherMethodDescription;

    @Test
    public void testPreservation() throws Exception {
        MethodRebaseResolver.Resolution resolution = new MethodRebaseResolver.Resolution.Preserved(methodDescription);
        assertThat(resolution.isRebased(), is(false));
        assertThat(resolution.getResolvedMethod(), is(methodDescription));
        try {
            resolution.getAdditionalArguments();
            fail();
        } catch (IllegalStateException ignored) {
            // expected
        }
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new MethodRebaseResolver.Resolution.Preserved(methodDescription).hashCode(),
                is(new MethodRebaseResolver.Resolution.Preserved(methodDescription).hashCode()));
        assertThat(new MethodRebaseResolver.Resolution.Preserved(methodDescription),
                is(new MethodRebaseResolver.Resolution.Preserved(methodDescription)));
        assertThat(new MethodRebaseResolver.Resolution.Preserved(otherMethodDescription).hashCode(),
                not(is(new MethodRebaseResolver.Resolution.Preserved(methodDescription).hashCode())));
        assertThat(new MethodRebaseResolver.Resolution.Preserved(methodDescription),
                not(is(new MethodRebaseResolver.Resolution.Preserved(otherMethodDescription))));
    }
}
