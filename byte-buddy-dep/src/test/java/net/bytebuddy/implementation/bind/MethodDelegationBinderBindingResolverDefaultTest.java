package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MethodDelegationBinderBindingResolverDefaultTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
        verifyNoMoreInteractions(ambiguityResolver);
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
