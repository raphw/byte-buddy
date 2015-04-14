package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
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
    protected MethodDescription leftMethod, rightMethod;

    @Mock
    protected MethodDelegationBinder.MethodBinding left, right;

    @Before
    public void setUp() throws Exception {
        when(left.getTarget()).thenReturn(leftMethod);
        when(right.getTarget()).thenReturn(rightMethod);
    }
}
