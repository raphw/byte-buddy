package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import org.junit.Before;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAmbiguityResolverTest {

    protected MethodDescription source;
    protected MethodDelegationBinder.BoundMethodDelegation left;
    protected MethodDescription leftMethod;
    protected MethodDelegationBinder.BoundMethodDelegation right;
    protected MethodDescription rightMethod;

    @Before
    public void setUp() throws Exception {
        source = mock(MethodDescription.class);
        leftMethod = mock(MethodDescription.class);
        left = mock(MethodDelegationBinder.BoundMethodDelegation.class);
        when(left.getBindingTarget()).thenReturn(leftMethod);
        right = mock(MethodDelegationBinder.BoundMethodDelegation.class);
        rightMethod = mock(MethodDescription.class);
        when(right.getBindingTarget()).thenReturn(rightMethod);
    }
}
