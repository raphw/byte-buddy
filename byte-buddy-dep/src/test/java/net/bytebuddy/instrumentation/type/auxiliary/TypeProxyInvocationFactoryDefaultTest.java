package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeProxyInvocationFactoryDefaultTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation.Target instrumentationTarget;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private Instrumentation.SpecialMethodInvocation specialMethodInvocation;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getUniqueSignature()).thenReturn(FOO);
    }

    @Test
    public void testSuperMethod() throws Exception {
        when(instrumentationTarget.invokeSuper(methodDescription, Instrumentation.Target.MethodLookup.Default.MOST_SPECIFIC))
                .thenReturn(specialMethodInvocation);
        assertThat(TypeProxy.InvocationFactory.Default.SUPER_METHOD.invoke(instrumentationTarget, typeDescription, methodDescription),
                is(specialMethodInvocation));
        verify(instrumentationTarget).invokeSuper(methodDescription, Instrumentation.Target.MethodLookup.Default.MOST_SPECIFIC);
        verifyNoMoreInteractions(instrumentationTarget);
    }

    @Test
    public void testDefaultMethod() throws Exception {
        when(instrumentationTarget.invokeDefault(typeDescription, FOO)).thenReturn(specialMethodInvocation);
        assertThat(TypeProxy.InvocationFactory.Default.DEFAULT_METHOD.invoke(instrumentationTarget, typeDescription, methodDescription),
                is(specialMethodInvocation));
        verify(instrumentationTarget).invokeDefault(typeDescription, FOO);
        verifyNoMoreInteractions(instrumentationTarget);
    }
}
