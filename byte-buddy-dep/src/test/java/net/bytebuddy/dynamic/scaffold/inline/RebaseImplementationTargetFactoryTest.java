package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
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
    private ClassFileVersion classFileVersion;

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private TypeDescription.Generic superClass;

    private Implementation.Target.Factory factory;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(instrumentedType.getSuperClass()).thenReturn(superClass);
        when(superClass.getDeclaredMethods()).thenReturn(new MethodList.Empty<MethodDescription.InGenericShape>());
        factory = new RebaseImplementationTarget.Factory(methodRebaseResolver);
    }

    @Test
    public void testReturnsRebaseImplementationTarget() throws Exception {
        assertThat(factory.make(instrumentedType, methodGraph, classFileVersion) instanceof RebaseImplementationTarget, is(true));
    }
}
