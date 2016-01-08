package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderProcessorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private MethodDescription source;

    @Mock
    private MethodDescription bindableTarget, unbindableTarget, dominantBindableTarget, invisibleTarget;

    @Mock
    private MethodDelegationBinder.MethodBinding boundDelegation, unboundDelegation, dominantBoundDelegation;

    @Mock
    private MethodDelegationBinder methodDelegationBinder;

    @Mock
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Mock
    private TypeDescription instrumentedType;

    @Before
    public void setUp() throws Exception {
        when(boundDelegation.isValid()).thenReturn(true);
        when(unboundDelegation.isValid()).thenReturn(false);
        when(dominantBoundDelegation.isValid()).thenReturn(true);
        when(methodDelegationBinder.bind(implementationTarget, source, bindableTarget))
                .thenReturn(boundDelegation);
        when(methodDelegationBinder.bind(implementationTarget, source, invisibleTarget))
                .thenReturn(boundDelegation);
        when(methodDelegationBinder.bind(implementationTarget, source, unbindableTarget))
                .thenReturn(unboundDelegation);
        when(methodDelegationBinder.bind(implementationTarget, source, dominantBindableTarget))
                .thenReturn(dominantBoundDelegation);
        ambiguityResolver = mock(MethodDelegationBinder.AmbiguityResolver.class);
        when(ambiguityResolver.resolve(source, dominantBoundDelegation, boundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
        when(ambiguityResolver.resolve(source, boundDelegation, dominantBoundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
        when(ambiguityResolver.resolve(source, boundDelegation, boundDelegation))
                .thenReturn(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
        when(implementationTarget.getInstrumentedType()).thenReturn(instrumentedType);
        when(unbindableTarget.isVisibleTo(instrumentedType)).thenReturn(true);
        when(bindableTarget.isVisibleTo(instrumentedType)).thenReturn(true);
        when(dominantBindableTarget.isVisibleTo(instrumentedType)).thenReturn(true);
        when(invisibleTarget.isVisibleTo(instrumentedType)).thenReturn(false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoBindableTarget() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(unbindableTarget, unbindableTarget, unbindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        processor.process(implementationTarget, source, methodDescriptions);
    }

    @Test
    public void testOneBindableTarget() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(unbindableTarget, bindableTarget, unbindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.process(implementationTarget, source, methodDescriptions);
        assertThat(result, is(boundDelegation));
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, bindableTarget);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(2)).bind(implementationTarget, source, unbindableTarget);
        verify(unboundDelegation, atLeast(2)).isValid();
        verifyZeroInteractions(ambiguityResolver);
    }

    @Test
    public void testTwoBindableTargetsWithDominant() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(unbindableTarget, bindableTarget, dominantBindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.process(implementationTarget, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, unbindableTarget);
        verify(unboundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, bindableTarget);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTwoBindableTargetsWithoutDominant() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(unbindableTarget, bindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        processor.process(implementationTarget, source, methodDescriptions);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableFirst() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(dominantBindableTarget, bindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.process(implementationTarget, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(implementationTarget, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver, times(2)).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableMid() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(bindableTarget, dominantBindableTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.process(implementationTarget, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(implementationTarget, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, dominantBoundDelegation);
        verify(ambiguityResolver).resolve(source, dominantBoundDelegation, boundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test
    public void testThreeBindableTargetsDominantBindableLast() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(bindableTarget, bindableTarget, dominantBindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.process(implementationTarget, source, methodDescriptions);
        assertThat(result, is(dominantBoundDelegation));
        verify(methodDelegationBinder, times(2)).bind(implementationTarget, source, bindableTarget);
        verify(boundDelegation, atLeast(2)).isValid();
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, dominantBindableTarget);
        verify(dominantBoundDelegation, atLeast(1)).isValid();
        verify(ambiguityResolver).resolve(source, boundDelegation, boundDelegation);
        verify(ambiguityResolver, times(2)).resolve(source, boundDelegation, dominantBoundDelegation);
        verifyNoMoreInteractions(ambiguityResolver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvisibleDoesNotBind() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(invisibleTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        processor.process(implementationTarget, source, methodDescriptions);
    }

    @Test
    public void testInvisibleDoesNotBindButBindable() throws Exception {
        MethodList<?> methodDescriptions = new MethodList.Explicit<MethodDescription>(invisibleTarget, bindableTarget);
        MethodDelegationBinder.Processor processor = new MethodDelegationBinder.Processor(methodDelegationBinder, ambiguityResolver);
        MethodDelegationBinder.MethodBinding result = processor.process(implementationTarget, source, methodDescriptions);
        assertThat(result, is(boundDelegation));
        verify(methodDelegationBinder, times(1)).bind(implementationTarget, source, bindableTarget);
        verify(boundDelegation, atLeast(1)).isValid();
        verify(methodDelegationBinder, times(0)).bind(implementationTarget, source, invisibleTarget);
        verifyZeroInteractions(ambiguityResolver);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegationBinder.Processor.class).apply();
    }
}
