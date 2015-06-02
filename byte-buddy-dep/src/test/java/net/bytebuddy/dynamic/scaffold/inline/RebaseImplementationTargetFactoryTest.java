package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.MethodLookupEngine;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class RebaseImplementationTargetFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private BridgeMethodResolver.Factory bridgeMethodResolverFactory;

    @Mock
    private MethodRebaseResolver methodRebaseResolver;

    @Mock
    private MethodLookupEngine.Finding finding;

    @Mock
    private MethodList instrumentedMethods;

    @Mock
    private TypeDescription instrumentedType, superType;

    private Implementation.Target.Factory factory;

    @Before
    public void setUp() throws Exception {
        when(finding.getInvokableMethods()).thenReturn(new MethodList.Empty());
        when(finding.getInvokableDefaultMethods()).thenReturn(Collections.<TypeDescription, Set<MethodDescription>>emptyMap());
        when(finding.getTypeDescription()).thenReturn(instrumentedType);
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        factory = new RebaseImplementationTarget.Factory(bridgeMethodResolverFactory, methodRebaseResolver);
    }

    @Test
    public void testReturnsRebaseimplementationTarget() throws Exception {
        assertThat(factory.make(finding, instrumentedMethods) instanceof RebaseImplementationTarget, is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(RebaseImplementationTarget.Factory.class).apply();
    }
}
