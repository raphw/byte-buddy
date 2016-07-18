package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class SubclassImplementationTargetFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodGraph.Linked methodGraph;

    @Mock
    private TypeDescription instrumentedType, rawSuperClass;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private TypeDescription.Generic superClass;

    @Before
    public void setUp() throws Exception {
        when(instrumentedType.getSuperClass()).thenReturn(superClass);
        when(superClass.asErasure()).thenReturn(rawSuperClass);
        when(superClass.getDeclaredMethods()).thenReturn(new MethodList.Empty<MethodDescription.InGenericShape>());
    }

    @Test
    public void testReturnsSubclassImplementationTarget() throws Exception {
        assertThat(SubclassImplementationTarget.Factory.SUPER_CLASS.make(instrumentedType, methodGraph, classFileVersion), instanceOf(SubclassImplementationTarget.class));
        assertThat(SubclassImplementationTarget.Factory.LEVEL_TYPE.make(instrumentedType, methodGraph, classFileVersion), instanceOf(SubclassImplementationTarget.class));
    }

    @Test
    public void testOriginTypeSuperClass() throws Exception {
        assertThat(SubclassImplementationTarget.Factory.SUPER_CLASS.make(instrumentedType, methodGraph, classFileVersion).getOriginType(), is((TypeDefinition) superClass));
    }

    @Test
    public void testOriginTypeLevelType() throws Exception {
        assertThat(SubclassImplementationTarget.Factory.LEVEL_TYPE.make(instrumentedType, methodGraph, classFileVersion).getOriginType(), is((TypeDefinition) instrumentedType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SubclassImplementationTarget.Factory.class).apply();
    }
}
