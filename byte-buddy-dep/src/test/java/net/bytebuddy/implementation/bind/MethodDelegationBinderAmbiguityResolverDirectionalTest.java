package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MethodDelegationBinderAmbiguityResolverDirectionalTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription source;

    @Mock
    private MethodDelegationBinder.MethodBinding left, right;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(source);
        verifyZeroInteractions(left);
        verifyZeroInteractions(right);
    }

    @Test
    public void testLeft() throws Exception {
        assertThat(MethodDelegationBinder.AmbiguityResolver.Directional.LEFT.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
    }

    @Test
    public void testRight() throws Exception {
        assertThat(MethodDelegationBinder.AmbiguityResolver.Directional.RIGHT.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegationBinder.AmbiguityResolver.Directional.class).apply();
    }
}
