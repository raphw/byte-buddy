package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ImplementationTargetMethodLookupDefaultTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private Map<String, MethodDescription> invokableMethods;

    @Mock
    private BridgeMethodResolver bridgeMethodResolver;

    @Test
    public void testExactLookup() throws Exception {
        assertThat(Implementation.Target.MethodLookup.Default.EXACT.resolve(methodDescription, invokableMethods, bridgeMethodResolver),
                is(methodDescription));
        verifyZeroInteractions(methodDescription);
        verifyZeroInteractions(invokableMethods);
        verifyZeroInteractions(bridgeMethodResolver);
    }

    @Test
    public void testMostSpecificLookup() throws Exception {
        when(methodDescription.getUniqueSignature()).thenReturn(FOO);
        when(invokableMethods.get(FOO)).thenReturn(methodDescription);
        when(bridgeMethodResolver.resolve(methodDescription)).thenReturn(methodDescription);
        when(invokableMethods.get(FOO)).thenReturn(methodDescription);
        assertThat(Implementation.Target.MethodLookup.Default.MOST_SPECIFIC.resolve(methodDescription, invokableMethods, bridgeMethodResolver),
                is(methodDescription));
        verify(methodDescription).getUniqueSignature();
        verifyNoMoreInteractions(methodDescription);
        verify(invokableMethods).get(FOO);
        verifyNoMoreInteractions(invokableMethods);
        verify(bridgeMethodResolver).resolve(methodDescription);
        verifyNoMoreInteractions(bridgeMethodResolver);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.Target.MethodLookup.Default.class).apply();
    }
}
