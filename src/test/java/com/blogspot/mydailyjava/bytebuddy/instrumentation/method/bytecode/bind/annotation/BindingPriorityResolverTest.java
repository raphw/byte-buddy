package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
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
    private BindingPriority highPriority, lowPriority;

    @Before
    public void setUp() throws Exception {
        when(left.getTarget()).thenReturn(leftMethod);
        when(right.getTarget()).thenReturn(rightMethod);
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
        when(leftMethod.getAnnotation(BindingPriority.class)).thenReturn(highPriority);
        when(rightMethod.getAnnotation(BindingPriority.class)).thenReturn(lowPriority);
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT));
    }

    @Test
    public void testRightPrioritized() throws Exception {
        when(leftMethod.getAnnotation(BindingPriority.class)).thenReturn(lowPriority);
        when(rightMethod.getAnnotation(BindingPriority.class)).thenReturn(highPriority);
        assertThat(BindingPriority.Resolver.INSTANCE.resolve(source, left, right),
                is(MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT));
    }
}
