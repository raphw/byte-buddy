package net.bytebuddy.instrumentation.method.bytecode.bind;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.*;

public class AmbiguityResolverChainTest extends AbstractAmbiguityResolverTest {

    @Mock
    private MethodDelegationBinder.AmbiguityResolver first, second, third;

    private MethodDelegationBinder.AmbiguityResolver chain;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        chain = new MethodDelegationBinder.AmbiguityResolver.Chain(first, second);
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

    @Test
    public void testEqualsHashCode() throws Exception {
        MethodDelegationBinder.AmbiguityResolver firstChain = MethodDelegationBinder.AmbiguityResolver.Chain
                .of(MethodDelegationBinder.AmbiguityResolver.Chain.of(first, second), third);
        MethodDelegationBinder.AmbiguityResolver secondChain = MethodDelegationBinder.AmbiguityResolver.Chain
                .of(first, second, third);
        assertThat(firstChain.hashCode(), is(secondChain.hashCode()));
        assertThat(firstChain, is(secondChain));
        assertThat(firstChain.hashCode(), not(is(chain.hashCode())));
        assertThat(firstChain, not(is(chain)));
    }
}
