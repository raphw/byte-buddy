package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.mockito.Mockito.when;

public abstract class AbstractAmbiguityResolverTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
