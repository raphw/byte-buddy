package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import org.junit.Before;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAmbiguityResolverTest {

    protected MethodDescription source;
    protected MethodDelegationBinder.Binding left;
    protected MethodDescription leftMethod;
    protected MethodDelegationBinder.Binding right;
    protected MethodDescription rightMethod;

    @Before
    public void setUp() throws Exception {
        source = mock(MethodDescription.class);
        leftMethod = mock(MethodDescription.class);
        left = mock(MethodDelegationBinder.Binding.class);
        when(left.getTarget()).thenReturn(leftMethod);
        right = mock(MethodDelegationBinder.Binding.class);
        rightMethod = mock(MethodDescription.class);
        when(right.getTarget()).thenReturn(rightMethod);
    }
}
