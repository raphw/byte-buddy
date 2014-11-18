package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class BindingPriorityResolverTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription source, leftMethod, rightMethod;
    @Mock
    private MethodDelegationBinder.MethodBinding left, right;
    @Mock
    private AnnotationList leftAnnotations, rightAnnotations;
    @Mock
    private BindingPriority highPriority, lowPriority;

    @Before
    public void setUp() throws Exception {
        when(left.getTarget()).thenReturn(leftMethod);
        when(right.getTarget()).thenReturn(rightMethod);
        when(leftMethod.getDeclaredAnnotations()).thenReturn(leftAnnotations);
        when(rightMethod.getDeclaredAnnotations()).thenReturn(rightAnnotations);
        when(highPriority.value()).thenReturn(BindingPriority.DEFAULT * 2d);
        when(lowPriority.value()).thenReturn(BindingPriority.DEFAULT / 2d);
    }

    @Test
    public void testNoPriorities() throws Exception {
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
    }

    @Test
    public void testLeftPrioritized() throws Exception {
        when(leftAnnotations.ofType(BindingPriority.class)).thenReturn(AnnotationDescription.ForLoadedAnnotation.of(highPriority));
        when(rightAnnotations.ofType(BindingPriority.class)).thenReturn(AnnotationDescription.ForLoadedAnnotation.of(lowPriority));
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
    }

    @Test
    public void testRightPrioritized() throws Exception {
        when(leftAnnotations.ofType(BindingPriority.class)).thenReturn(AnnotationDescription.ForLoadedAnnotation.of(lowPriority));
        when(rightAnnotations.ofType(BindingPriority.class)).thenReturn(AnnotationDescription.ForLoadedAnnotation.of(highPriority));
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
    }
}
