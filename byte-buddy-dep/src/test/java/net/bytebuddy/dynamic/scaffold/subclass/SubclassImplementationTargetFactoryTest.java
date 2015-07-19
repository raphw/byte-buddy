package net.bytebuddy.dynamic.scaffold.subclass;

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

public class SubclassImplementationTargetFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private BridgeMethodResolver.Factory bridgeMethodResolverFactory;

    @Mock
    private MethodLookupEngine.Finding finding;

    @Mock
    private MethodList<?> methodList;

    @Mock
    private TypeDescription instrumentedType, superType;

    private Implementation.Target.Factory factory;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(finding.getInvokableMethods()).thenReturn((MethodList) new MethodList.Empty());
        when(finding.getInvokableDefaultMethods()).thenReturn(Collections.<TypeDescription, Set<MethodDescription>>emptyMap());
        when(finding.getTypeDescription()).thenReturn(instrumentedType);
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.asRawType()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        factory = new SubclassImplementationTarget.Factory(bridgeMethodResolverFactory,
                SubclassImplementationTarget.OriginTypeIdentifier.SUPER_TYPE);
    }

    @Test
    public void testReturnsSubclassimplementationTarget() throws Exception {
        assertThat(factory.make(finding, methodList) instanceof SubclassImplementationTarget, is(true));
    }

    @Test
    public void testOriginTypeSuperType() throws Exception {
        assertThat(new SubclassImplementationTarget.Factory(bridgeMethodResolverFactory,
                SubclassImplementationTarget.OriginTypeIdentifier.SUPER_TYPE).make(finding, methodList)
                .getOriginType(), is(superType));
    }

    @Test
    public void testOriginTypeLevelType() throws Exception {
        assertThat(new SubclassImplementationTarget.Factory(bridgeMethodResolverFactory,
                SubclassImplementationTarget.OriginTypeIdentifier.LEVEL_TYPE).make(finding, methodList)
                .getOriginType(), is(instrumentedType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassImplementationTarget.Factory.class).apply();
    }
}
