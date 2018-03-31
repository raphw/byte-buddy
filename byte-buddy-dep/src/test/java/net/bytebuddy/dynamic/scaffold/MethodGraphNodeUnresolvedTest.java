package net.bytebuddy.dynamic.scaffold;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
}
