package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodGraphLinkedDelegationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodGraph methodGraph, superGraph, interfaceGraph;

    @Mock
    private MethodDescription.Token methodToken;

    @Mock
    private MethodGraph.Node node;

    @Mock
    private MethodGraph.NodeList nodeList;

    private MethodGraph.Linked linkedMethodGraph;

    @Before
    public void setUp() throws Exception {
        when(methodGraph.locate(methodToken)).thenReturn(node);
        when(methodGraph.listNodes()).thenReturn(nodeList);
        linkedMethodGraph = new MethodGraph.Linked.Delegation(methodGraph, superGraph, Collections.singletonMap(typeDescription, interfaceGraph));
    }

    @Test
    public void testLocateNode() throws Exception {
        assertThat(linkedMethodGraph.locate(methodToken), is(node));
    }

    @Test
    public void testNodeList() throws Exception {
        assertThat(linkedMethodGraph.listNodes(), is(nodeList));
    }

    @Test
    public void testSuperGraph() throws Exception {
        assertThat(linkedMethodGraph.getSuperGraph(), is(superGraph));
    }

    @Test
    public void testKnownInterfaceGraph() throws Exception {
        assertThat(linkedMethodGraph.getInterfaceGraph(typeDescription), is(interfaceGraph));
    }

    @Test
    public void testUnknownInterfaceGraph() throws Exception {
        assertThat(linkedMethodGraph.getInterfaceGraph(mock(TypeDescription.class)), is((MethodGraph) MethodGraph.Empty.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Linked.Delegation.class).apply();
    }
}
