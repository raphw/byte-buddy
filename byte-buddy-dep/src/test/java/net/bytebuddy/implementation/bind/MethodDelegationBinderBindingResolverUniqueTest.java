package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodDelegationBinderBindingResolverUniqueTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
