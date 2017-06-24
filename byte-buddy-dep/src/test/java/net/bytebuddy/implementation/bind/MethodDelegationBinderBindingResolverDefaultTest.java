package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderBindingResolverDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription source, target;

    @Mock
    private MethodDelegationBinder.MethodBinding methodBinding;

    @Mock
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Mock
    private MethodDelegationBinder.MethodBinding boundDelegation, dominantBoundDelegation;

    @Before
    public void setUp() throws Exception {
        when(ambiguityResolver.resolve(source, dominantBoundDelegation, boundDelegation)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        when(ambiguityResolver.resolve(source, boundDelegation, dominantBoundDelegation)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        when(ambiguityResolver.resolve(source, boundDelegation, boundDelegation)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
        when(methodBinding.getTarget()).thenReturn(target);
    }

    @Test
    public void testOneBindableTarget() throws Exception {
        MethodDelegationBinder.MethodBinding result = MethodDelegationBinder.BindingResolver.Default.INSTANCE.resolve(ambiguityResolver,
                source,
                Collections.singletonList(boundDelegation));
        MatcherAssert.assertThat(result, is(boundDelegation));
        verifyZeroInteractions(ambiguityResolver);
    }

    @Test
    public void testTwoBindableTargetsWithDominant() throws Exception {
        MethodDelegationBinder.MethodBinding result = MethodDelegationBinder.BindingResolver.Default.INSTANCE.resolve(ambiguityResolver,
                source,
                Arrays.asList(boundDelegation, dominantBoundDelegation));
        MatcherAssert.assertThat(result, is(dominantBoundDelegation));
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTwoBindableTargetsWithoutDominant() throws Exception {
        MethodDelegationBinder.BindingResolver.Default.INSTANCE.resolve(ambiguityResolver,
                source,
                Arrays.asList(boundDelegation, boundDelegation));
    }

    @Test
    public void testThreeBindableTargetsDominantBindableFirst() throws Exception {
        MethodDelegationBinder.BindingResolver.Default.INSTANCE.resolve(ambiguityResolver,
                source,
                Arrays.asList(dominantBoundDelegation, boundDelegation, boundDelegation));
        verify(ambiguityResolver, times(2)).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableMid() throws Exception {
        MethodDelegationBinder.BindingResolver.Default.INSTANCE.resolve(ambiguityResolver,
                source,
                Arrays.asList(boundDelegation, dominantBoundDelegation, boundDelegation));
        verify(ambiguityResolver).resolve(source, dominantBoundDelegation, boundDelegation);
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableLast() throws Exception {
        MethodDelegationBinder.BindingResolver.Default.INSTANCE.resolve(ambiguityResolver,
                source,
                Arrays.asList(boundDelegation, boundDelegation, dominantBoundDelegation));
        verify(ambiguityResolver).resolve(source, boundDelegation, boundDelegation);
        verify(ambiguityResolver, times(2)).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }
}
