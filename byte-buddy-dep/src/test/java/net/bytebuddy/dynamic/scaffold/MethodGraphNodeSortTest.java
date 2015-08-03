package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MethodGraphNodeSortTest {

    @Test
    public void testSortVisible() throws Exception {
        assertThat(MethodGraph.Node.Sort.VISIBLE.isResolved(), is(true));
        assertThat(MethodGraph.Node.Sort.VISIBLE.isUnique(), is(true));
        assertThat(MethodGraph.Node.Sort.VISIBLE.isMadeVisible(), is(true));
    }

    @Test
    public void testSortResolved() throws Exception {
        assertThat(MethodGraph.Node.Sort.RESOLVED.isResolved(), is(true));
        assertThat(MethodGraph.Node.Sort.RESOLVED.isUnique(), is(true));
        assertThat(MethodGraph.Node.Sort.RESOLVED.isMadeVisible(), is(false));
    }

    @Test
    public void testAmbiguous() throws Exception {
        assertThat(MethodGraph.Node.Sort.AMBIGUOUS.isResolved(), is(true));
        assertThat(MethodGraph.Node.Sort.AMBIGUOUS.isUnique(), is(false));
        assertThat(MethodGraph.Node.Sort.AMBIGUOUS.isMadeVisible(), is(false));
    }

    @Test
    public void testUnresolved() throws Exception {
        assertThat(MethodGraph.Node.Sort.UNRESOLVED.isResolved(), is(false));
        assertThat(MethodGraph.Node.Sort.UNRESOLVED.isUnique(), is(false));
        assertThat(MethodGraph.Node.Sort.UNRESOLVED.isMadeVisible(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Node.Sort.class).apply();
    }
}