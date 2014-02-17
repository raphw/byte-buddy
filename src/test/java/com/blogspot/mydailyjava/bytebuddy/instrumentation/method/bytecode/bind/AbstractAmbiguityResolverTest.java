package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.test.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public abstract class AbstractAmbiguityResolverTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    protected MethodDescription source;
    @Mock
    protected MethodDescription leftMethod,rightMethod;
    @Mock
    protected MethodDelegationBinder.Binding left, right;

    @Before
    public void setUp() throws Exception {
        when(left.getTarget()).thenReturn(leftMethod);
        when(right.getTarget()).thenReturn(rightMethod);
    }
}
