package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MethodDelegationBinderAmbiguityResolverDirectionalTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription source;

    @Mock
    private MethodDelegationBinder.MethodBinding left, right;

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(source);
        verifyNoMoreInteractions(left);
        verifyNoMoreInteractions(right);
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
}
