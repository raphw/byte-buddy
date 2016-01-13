package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodGraphEmptyTest {

    @Test
    public void testNode() throws Exception {
        assertThat(MethodGraph.Empty.INSTANCE.locate(mock(MethodDescription.SignatureToken.class)).getSort(), is(MethodGraph.Node.Sort.UNRESOLVED));
    }

    @Test
    public void testListNode() throws Exception {
        assertThat(MethodGraph.Empty.INSTANCE.listNodes().size(), is(0));
    }

    @Test
    public void testSuperGraph() throws Exception {
        assertThat(MethodGraph.Empty.INSTANCE.getSuperClassGraph(), is((MethodGraph) MethodGraph.Empty.INSTANCE));
    }

    @Test
    public void testInterfaceGraph() throws Exception {
        assertThat(MethodGraph.Empty.INSTANCE.getInterfaceGraph(mock(TypeDescription.class)), is((MethodGraph) MethodGraph.Empty.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Empty.class).apply();
    }
}
