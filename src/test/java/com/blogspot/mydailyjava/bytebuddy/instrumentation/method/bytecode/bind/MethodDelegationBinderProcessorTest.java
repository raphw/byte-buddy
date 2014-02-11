package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType0;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderProcessorTest {

    private InstrumentedType0 typeDescription;
    private MethodDescription source;

    private MethodDescription bindableTarget, unbindableTarget, dominantBindableTarget;
    private MethodDelegationBinder.Binding boundDelegation, unboundDelegation, dominantBoundDelegation;

    private MethodDelegationBinder methodDelegationBinder;
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Before
    public void setUp() throws Exception {
        typeDescription = mock(InstrumentedType0.class);
        source = mock(MethodDescription.class);
        bindableTarget = mock(MethodDescription.class);
        unbindableTarget = mock(MethodDescription.class);
        dominantBindableTarget = mock(MethodDescription.class);
        boundDelegation = mock(MethodDelegationBinder.Binding.class);
        when(boundDelegation.isValid()).thenReturn(true);
        unboundDelegation = mock(MethodDelegationBinder.Binding.class);
        when(unboundDelegation.isValid()).thenReturn(false);
        dominantBoundDelegation = mock(MethodDelegationBinder.Binding.class);
        when(dominantBoundDelegation.isValid()).thenReturn(true);
        methodDelegationBinder = mock(MethodDelegationBinder.class);
        when(methodDelegationBinder.bind(typeDescription, source, bindableTarget))
                .thenReturn(boundDelegation);
        when(methodDelegationBinder.bind(typeDescription, source, unbindableTarget))
                .thenReturn(unboundDelegation);
        when(methodDelegationBinder.bind(typeDescription, source, dominantBindableTarget))
                .thenReturn(dominantBoundDelegation);
        ambiguityResolver = mock(MethodDelegationBinder.AmbiguityResolver.class);
        when(ambiguityResolver.resolve(source, dominantBoundDelegation, boundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        when(ambiguityResolver.resolve(source, boundDelegation, dominantBoundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        when(ambiguityResolver.resolve(source, boundDelegation, boundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoBindableTarget() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, unbindableTarget, unbindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        processor.process(typeDescription, source, methodDescriptions);
    }

    @Test
    public void testOneBindableTarget() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, bindableTarget, unbindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(boundDelegation));
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, unbindableTarget);
        verify(unboundDelegation, atLeast(2)).isValid();
        verifyZeroInteractions(ambiguityResolver);
    }

    @Test
    public void testTwoBindableTargetsWithDominant() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, bindableTarget, dominantBindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, unbindableTarget);
        verify(unboundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTwoBindableTargetsWithoutDominant() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(unbindableTarget, bindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        processor.process(typeDescription, source, methodDescriptions);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableFirst() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(dominantBindableTarget, bindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver, times(2)).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableMid() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(bindableTarget, dominantBindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verify(ambiguityResolver).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableLast() throws Exception {
        List<MethodDescription> methodDescriptions = Arrays.asList(bindableTarget, bindableTarget, dominantBindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.Binding result = processor.process(typeDescription, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(typeDescription, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(typeDescription, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, boundDelegation);
        verify(ambiguityResolver, times(2)).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }
}
