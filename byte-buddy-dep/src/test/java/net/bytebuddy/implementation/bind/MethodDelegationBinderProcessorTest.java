package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderProcessorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription source;

    @Mock
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Mock
    private MethodDelegationBinder.MethodBinding boundDelegation, unboundDelegation, dominantBoundDelegation;

    @Mock
    private MethodDelegationBinder.Record boundRecord, unboundRecord, dominantBoundRecord;

    @Mock
    private MethodDelegationBinder.TerminationHandler terminationHandler;

    @Mock
    private MethodDelegationBinder.MethodInvoker methodInvoker;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private Assigner assigner;

    @Before
    public void setUp() throws Exception {
        when(boundDelegation.isValid()).thenReturn(true);
        when(unboundDelegation.isValid()).thenReturn(false);
        when(dominantBoundDelegation.isValid()).thenReturn(true);
        when(boundRecord.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner)).thenReturn(boundDelegation);
        when(unboundRecord.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner)).thenReturn(unboundDelegation);
        when(dominantBoundRecord.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner)).thenReturn(dominantBoundDelegation);
        when(ambiguityResolver.resolve(source, dominantBoundDelegation, boundDelegation)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        when(ambiguityResolver.resolve(source, boundDelegation, dominantBoundDelegation)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        when(ambiguityResolver.resolve(source, boundDelegation, boundDelegation)).thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoBindableTarget() throws Exception {
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(Arrays.asList(unboundRecord, unboundRecord, unboundRecord), ambiguityResolver);
        processor.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
    }

    @Test
    public void testOneBindableTarget() throws Exception {
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(Arrays.asList(unboundRecord, boundRecord, unboundRecord), ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        assertThat(result, is(boundDelegation));
        verify(boundRecord, times(1)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(unboundRecord, times(2)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(unboundDelegation, atLeast(2)).isValid();
        verifyZeroInteractions(ambiguityResolver);
    }

    @Test
    public void testTwoBindableTargetsWithDominant() throws Exception {
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(Arrays.asList(unboundRecord, boundRecord, dominantBoundRecord), ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        assertThat(result, is(dominantBoundDelegation));
        verify(unboundRecord, times(1)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(unboundDelegation, atLeast(1)).isValid();
        verify(boundRecord, times(1)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(dominantBoundRecord, times(1)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTwoBindableTargetsWithoutDominant() throws Exception {
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(Arrays.asList(unboundRecord, boundRecord, boundRecord), ambiguityResolver);
        processor.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableFirst() throws Exception {
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(Arrays.asList(dominantBoundRecord, boundRecord, boundRecord), ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        assertThat(result, is(dominantBoundDelegation));
        verify(boundRecord, times(2)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(dominantBoundRecord, times(1)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver, times(2)).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableMid() throws Exception {
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(Arrays.asList(boundRecord, dominantBoundRecord, boundRecord), ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        assertThat(result, is(dominantBoundDelegation));
        verify(boundRecord, times(2)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(dominantBoundRecord, times(1)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verify(ambiguityResolver).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableLast() throws Exception {
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(Arrays.asList(boundRecord, boundRecord, dominantBoundRecord), ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        assertThat(result, is(dominantBoundDelegation));
        verify(boundRecord, times(2)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(dominantBoundRecord, times(1)).bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, boundDelegation);
        verify(ambiguityResolver, times(2)).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegationBinder.Processor.class).apply();
    }
}
