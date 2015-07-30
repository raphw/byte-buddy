package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class MethodGraphNodeTest {

    @Test
    public void testSortResolved() throws Exception {
        assertThat(MethodGraph.Node.Sort.RESOLVED.isResolved(), is(true));
        assertThat(MethodGraph.Node.Sort.RESOLVED.isUnique(), is(true));
    }

    @Test
    public void testAmbiguous() throws Exception {
        assertThat(MethodGraph.Node.Sort.AMBIGUOUS.isResolved(), is(true));
        assertThat(MethodGraph.Node.Sort.AMBIGUOUS.isUnique(), is(false));
    }

    @Test
    public void testUnresolved() throws Exception {
        assertThat(MethodGraph.Node.Sort.UNRESOLVED.isResolved(), is(false));
        assertThat(MethodGraph.Node.Sort.UNRESOLVED.isUnique(), is(false));
    }

    @Test
    public void testIllegalNodeSort() throws Exception {
        assertThat(MethodGraph.Node.Illegal.INSTANCE.getSort(), is(MethodGraph.Node.Sort.UNRESOLVED));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalNodeBridgesThrowsException() throws Exception {
        MethodGraph.Node.Illegal.INSTANCE.getBridges();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalNodeRepresentativeThrowsException() throws Exception {
        MethodGraph.Node.Illegal.INSTANCE.getRepresentative();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Node.Sort.class).apply();
        ObjectPropertyAssertion.of(MethodGraph.Node.Illegal.class).apply();
    }
}