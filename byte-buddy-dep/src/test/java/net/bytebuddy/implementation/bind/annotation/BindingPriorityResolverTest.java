package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class BindingPriorityResolverTest extends AbstractAnnotationTest<BindingPriority> {

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

    public BindingPriorityResolverTest() {
        super(BindingPriority.class);
    }

    @Before
    public void setUp() throws Exception {
        when(left.getTarget()).thenReturn(leftMethod);
        when(right.getTarget()).thenReturn(rightMethod);
        when(leftMethod.getDeclaredAnnotations()).thenReturn(leftAnnotations);
        when(rightMethod.getDeclaredAnnotations()).thenReturn(rightAnnotations);
        when(highPriority.value()).thenReturn(BindingPriority.DEFAULT * 3);
        when(lowPriority.value()).thenReturn(BindingPriority.DEFAULT * 2);
    }

    @Test
    public void testNoPriorities() throws Exception {
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS));
    }

    @Test
    public void testLeftPrioritized() throws Exception {
        AnnotationDescription.Loadable<BindingPriority> highPriority = AnnotationDescription.ForLoadedAnnotation.of(this.highPriority);
        when(leftAnnotations.ofType(BindingPriority.class)).thenReturn(highPriority);
        AnnotationDescription.Loadable<BindingPriority> lowPriority = AnnotationDescription.ForLoadedAnnotation.of(this.lowPriority);
        when(rightAnnotations.ofType(BindingPriority.class)).thenReturn(lowPriority);
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right), is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
    }

    @Test
    public void testRightPrioritized() throws Exception {
        AnnotationDescription.Loadable<BindingPriority> lowPriority = AnnotationDescription.ForLoadedAnnotation.of(this.lowPriority);
        when(leftAnnotations.ofType(BindingPriority.class)).thenReturn(lowPriority);
        AnnotationDescription.Loadable<BindingPriority> highPriority = AnnotationDescription.ForLoadedAnnotation.of(this.highPriority);
        when(rightAnnotations.ofType(BindingPriority.class)).thenReturn(highPriority);
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right), is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
    }
}
