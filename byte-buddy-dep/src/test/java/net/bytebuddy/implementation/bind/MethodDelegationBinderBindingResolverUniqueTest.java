package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class MethodDelegationBinderBindingResolverUniqueTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription source, target;

    @Mock
    private MethodDelegationBinder.MethodBinding methodBinding;

    @Mock
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Before
    public void setUp() throws Exception {
        when(methodBinding.getTarget()).thenReturn(target);
    }

    @Test
    public void testUnique() throws Exception {
        assertThat(MethodDelegationBinder.BindingResolver.Unique.INSTANCE.resolve(ambiguityResolver,
                source,
                Collections.singletonList(methodBinding)), is(methodBinding));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonUnique() throws Exception {
        MethodDelegationBinder.BindingResolver.Unique.INSTANCE.resolve(ambiguityResolver,
                source,
                Arrays.asList(mock(MethodDelegationBinder.MethodBinding.class), methodBinding));
    }
}
