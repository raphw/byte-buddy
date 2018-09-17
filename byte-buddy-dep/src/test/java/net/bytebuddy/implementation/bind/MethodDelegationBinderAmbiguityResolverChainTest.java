package net.bytebuddy.implementation.bind;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderAmbiguityResolverChainTest extends AbstractAmbiguityResolverTest {

    @Mock
    private MethodDelegationBinder.AmbiguityResolver first, second, third;

    private MethodDelegationBinder.AmbiguityResolver chain;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        chain = new MethodDelegationBinder.AmbiguityResolver.Compound(first, second);
    }

    @Test
    public void testFirstResolves() throws Exception {
        when(first.resolve(source, left, right)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        assertThat(chain.resolve(source, left, right), is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
        verify(first).resolve(source, left, right);
        verifyNoMoreInteractions(first);
        verifyZeroInteractions(second);
    }

    @Test
    public void testSecondResolves() throws Exception {
        when(first.resolve(source, left, right)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
        when(second.resolve(source, left, right)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        assertThat(chain.resolve(source, left, right), is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
        verify(first).resolve(source, left, right);
        verifyNoMoreInteractions(first);
        verify(second).resolve(source, left, right);
        verifyNoMoreInteractions(second);
    }
}
