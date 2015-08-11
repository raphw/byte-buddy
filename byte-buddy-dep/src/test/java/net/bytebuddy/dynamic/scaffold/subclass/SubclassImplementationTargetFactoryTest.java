package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class SubclassImplementationTargetFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodGraph.Linked methodGraph;

    @Mock
    private TypeDescription instrumentedType, superType;

    private Implementation.Target.Factory factory;

    @Before
    public void setUp() throws Exception {
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.asErasure()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        factory = new SubclassImplementationTarget.Factory(SubclassImplementationTarget.OriginTypeResolver.SUPER_TYPE);
    }

    @Test
    public void testReturnsSubclassimplementationTarget() throws Exception {
        assertThat(factory.make(instrumentedType, methodGraph) instanceof SubclassImplementationTarget, is(true));
    }

    @Test
    public void testOriginTypeSuperType() throws Exception {
        assertThat(new SubclassImplementationTarget.Factory(SubclassImplementationTarget.OriginTypeResolver.SUPER_TYPE)
                .make(instrumentedType, methodGraph).getOriginType(), is(superType));
    }

    @Test
    public void testOriginTypeLevelType() throws Exception {
        assertThat(new SubclassImplementationTarget.Factory(SubclassImplementationTarget.OriginTypeResolver.LEVEL_TYPE)
                .make(instrumentedType, methodGraph).getOriginType(), is(instrumentedType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassImplementationTarget.Factory.class).apply();
    }
}
