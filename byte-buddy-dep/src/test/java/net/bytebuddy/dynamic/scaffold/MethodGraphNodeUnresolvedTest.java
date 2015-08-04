package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MethodGraphNodeUnresolvedTest {

    @Test
    public void testSort() throws Exception {
        assertThat(MethodGraph.Node.Unresolved.INSTANCE.getSort(), is(MethodGraph.Node.Sort.UNRESOLVED));
    }

    @Test(expected = IllegalStateException.class)
    public void testBridgesThrowsException() throws Exception {
        MethodGraph.Node.Unresolved.INSTANCE.getMethodTypes();
    }

    @Test(expected = IllegalStateException.class)
    public void testRepresentativeThrowsException() throws Exception {
        MethodGraph.Node.Unresolved.INSTANCE.getRepresentative();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Node.Unresolved.class).apply();
    }
}
