package net.bytebuddy.implementation.bind;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodBindingAmbiguityResolutionTest {

    private static final MethodDelegationBinder.AmbiguityResolver.Resolution LEFT =
            MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT;

    private static final MethodDelegationBinder.AmbiguityResolver.Resolution RIGHT =
            MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT;

    private static void testConflictMerge(MethodDelegationBinder.AmbiguityResolver.Resolution first,
                                          MethodDelegationBinder.AmbiguityResolver.Resolution second) {
        assertThat(first.merge(second), is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
    }

    private static void testSelfMerge(MethodDelegationBinder.AmbiguityResolver.Resolution resolution) {
        assertThat(resolution.merge(resolution), is(resolution));
    }

    @Test
    public void testSelfMerge() throws Exception {
        testSelfMerge(LEFT);
        testSelfMerge(RIGHT);
    }

    @Test
    public void testConflictMerge() throws Exception {
        testConflictMerge(LEFT, RIGHT);
        testConflictMerge(RIGHT, LEFT);
    }
}
