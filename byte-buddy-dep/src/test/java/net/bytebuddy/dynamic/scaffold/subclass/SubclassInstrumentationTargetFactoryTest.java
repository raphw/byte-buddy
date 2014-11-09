package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
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

public class SubclassInstrumentationTargetFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private BridgeMethodResolver.Factory bridgeMethodResolverFactory;
    @Mock
    private MethodLookupEngine.Finding finding;
    @Mock
    private TypeDescription instrumentedType, superType;

    private Instrumentation.Target.Factory factory;

    @Before
    public void setUp() throws Exception {
        when(finding.getInvokableMethods()).thenReturn(new MethodList.Empty());
        when(finding.getInvokableDefaultMethods()).thenReturn(Collections.<TypeDescription, Set<MethodDescription>>emptyMap());
        when(finding.getTypeDescription()).thenReturn(instrumentedType);
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        factory = new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                SubclassInstrumentationTarget.OriginTypeIdentifier.SUPER_TYPE);
    }

    @Test
    public void testReturnsSubclassInstrumentationTarget() throws Exception {
        assertThat(factory.make(finding) instanceof SubclassInstrumentationTarget, is(true));
    }

    @Test
    public void testOriginTypeSuperType() throws Exception {
        assertThat(new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                SubclassInstrumentationTarget.OriginTypeIdentifier.SUPER_TYPE).make(finding)
                .getOriginType(), is(superType));
    }

    @Test
    public void testOriginTypeLevelType() throws Exception {
        assertThat(new SubclassInstrumentationTarget.Factory(bridgeMethodResolverFactory,
                SubclassInstrumentationTarget.OriginTypeIdentifier.LEVEL_TYPE).make(finding)
                .getOriginType(), is(instrumentedType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassInstrumentationTarget.Factory.class).apply();
    }
}
