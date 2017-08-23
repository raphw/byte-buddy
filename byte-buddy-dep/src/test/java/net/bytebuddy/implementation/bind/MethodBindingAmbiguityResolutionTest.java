package net.bytebuddy.implementation.bind;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodBindingAmbiguityResolutionTest {

    private static void testConflictMerge(MethodDelegationBinder.AmbiguityResolver.Resolution first,
                                          MethodDelegationBinder.AmbiguityResolver.Resolution second) {
        assertThat(first.merge(second), is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
    }

    private static void testSelfMerge(MethodDelegationBinder.AmbiguityResolver.Resolution resolution) {
        assertThat(resolution.merge(resolution), is(resolution));
    }

    private static void testUnknownMerge(MethodDelegationBinder.AmbiguityResolver.Resolution resolution) {
        assertThat(MethodDelegationBinder.AmbiguityResolver.Resolution.UNKNOWN.merge(resolution), is(resolution));
        assertThat(resolution.merge(MethodDelegationBinder.AmbiguityResolver.Resolution.UNKNOWN), is(resolution));
    }

    @Test
    public void testUnknownMerge() throws Exception {
        testUnknownMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        testUnknownMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        testUnknownMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.UNKNOWN);
    }

    @Test
    public void testSelfMerge() throws Exception {
        testSelfMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        testSelfMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        testSelfMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.UNKNOWN);
    }

    @Test
    public void testConflictMerge() throws Exception {
        testConflictMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT, MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        testConflictMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT, MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
        testConflictMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS, MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        testConflictMerge(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT, MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
    }
}
