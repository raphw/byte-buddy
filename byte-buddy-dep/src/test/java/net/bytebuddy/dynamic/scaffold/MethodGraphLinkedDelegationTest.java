package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodGraphLinkedDelegationTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodGraph methodGraph, superGraph, interfaceGraph;

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private MethodGraph.Node node;

    @Mock
    private MethodGraph.NodeList nodeList;

    private MethodGraph.Linked linkedMethodGraph;

    @Before
    public void setUp() throws Exception {
        when(methodGraph.locate(token)).thenReturn(node);
        when(methodGraph.listNodes()).thenReturn(nodeList);
        linkedMethodGraph = new MethodGraph.Linked.Delegation(methodGraph, superGraph, Collections.singletonMap(typeDescription, interfaceGraph));
    }

    @Test
    public void testLocateNode() throws Exception {
        assertThat(linkedMethodGraph.locate(token), is(node));
    }

    @Test
    public void testNodeList() throws Exception {
        assertThat(linkedMethodGraph.listNodes(), is(nodeList));
    }

    @Test
    public void testSuperGraph() throws Exception {
        assertThat(linkedMethodGraph.getSuperClassGraph(), is(superGraph));
    }

    @Test
    public void testKnownInterfaceGraph() throws Exception {
        assertThat(linkedMethodGraph.getInterfaceGraph(typeDescription), is(interfaceGraph));
    }

    @Test
    public void testUnknownInterfaceGraph() throws Exception {
        assertThat(linkedMethodGraph.getInterfaceGraph(mock(TypeDescription.class)), is((MethodGraph) MethodGraph.Empty.INSTANCE));
    }
}
