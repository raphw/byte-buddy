package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StackManipulationSizeTest {

    @Test
    public void testSizeGrowth() throws Exception {
        StackManipulation.Size first = new StackManipulation.Size(2, 5);
        StackManipulation.Size second = new StackManipulation.Size(1, 1);
        StackManipulation.Size merged = first.aggregate(second);
        assertThat(merged.getSizeImpact(), is(3));
        assertThat(merged.getMaximalSize(), is(5));
    }

    @Test
    public void testSizeReduction() throws Exception {
        StackManipulation.Size first = new StackManipulation.Size(-3, 0);
        StackManipulation.Size second = new StackManipulation.Size(-2, 0);
        StackManipulation.Size merged = first.aggregate(second);
        assertThat(merged.getSizeImpact(), is(-5));
        assertThat(merged.getMaximalSize(), is(0));
    }

    @Test
    public void testSizeGrowthAndReduction() throws Exception {
        StackManipulation.Size first = new StackManipulation.Size(3, 4);
        StackManipulation.Size second = new StackManipulation.Size(-5, 1);
        StackManipulation.Size merged = first.aggregate(second);
        assertThat(merged.getSizeImpact(), is(-2));
        assertThat(merged.getMaximalSize(), is(4));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(StackManipulation.Size.class).apply();
    }
}
