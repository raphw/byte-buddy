package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeProxyInvocationFactoryDefaultTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private MethodDescription.TypeToken typeToken;

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asSignatureToken()).thenReturn(token);
        when(methodDescription.asTypeToken()).thenReturn(typeToken);
        when(specialMethodInvocation.withCheckedCompatibilityTo(typeToken)).thenReturn(specialMethodInvocation);
    }

    @Test
    public void testSuperMethod() throws Exception {
        when(implementationTarget.invokeDominant(token)).thenReturn(specialMethodInvocation);
        assertThat(TypeProxy.InvocationFactory.Default.SUPER_METHOD.invoke(implementationTarget, typeDescription, methodDescription),
                is(specialMethodInvocation));
        verify(implementationTarget).invokeDominant(token);
        verifyNoMoreInteractions(implementationTarget);
    }

    @Test
    public void testDefaultMethod() throws Exception {
        when(implementationTarget.invokeDefault(token, typeDescription)).thenReturn(specialMethodInvocation);
        assertThat(TypeProxy.InvocationFactory.Default.DEFAULT_METHOD.invoke(implementationTarget, typeDescription, methodDescription),
                is(specialMethodInvocation));
        verify(implementationTarget).invokeDefault(token, typeDescription);
        verifyNoMoreInteractions(implementationTarget);
    }
}
