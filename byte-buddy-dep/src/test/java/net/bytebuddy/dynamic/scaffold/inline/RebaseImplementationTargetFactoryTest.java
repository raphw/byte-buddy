package net.bytebuddy.dynamic.scaffold.inline;

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

public class RebaseImplementationTargetFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodRebaseResolver methodRebaseResolver;

    @Mock
    private MethodGraph.Linked methodGraph;

    @Mock
    private TypeDescription instrumentedType, superType;

    private Implementation.Target.Factory factory;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(instrumentedType.getSuperType()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        factory = new RebaseImplementationTarget.Factory(new MethodList.Empty(), methodRebaseResolver);
    }

    @Test
    public void testReturnsRebaseImplementationTarget() throws Exception {
        assertThat(factory.make(instrumentedType, methodGraph) instanceof RebaseImplementationTarget, is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(RebaseImplementationTarget.Factory.class).apply();
    }
}
