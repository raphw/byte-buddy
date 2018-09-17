package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodDelegationBinderAmbiguityResolverNoOpTest {

    @Test
    public void testResolution() throws Exception {
        assertThat(MethodDelegationBinder.AmbiguityResolver.NoOp.INSTANCE.resolve(mock(MethodDescription.class),
                mock(MethodDelegationBinder.MethodBinding.class),
                mock(MethodDelegationBinder.MethodBinding.class)),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.UNKNOWN));
    }
}
