package net.bytebuddy.utility;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldComparatorTest {

    @Test
    public void testComparisonSame() throws Exception {
        assertThat(FieldComparator.INSTANCE.compare(
                Sample.class.getDeclaredField("first"),
                Sample.class.getDeclaredField("first")), is(0));
    }

    @Test
    public void testComparisonDifferent() throws Exception {
        assertThat(FieldComparator.INSTANCE.compare(
                Sample.class.getDeclaredField("first"),
                Sample.class.getDeclaredField("second")), is(-13));
        assertThat(FieldComparator.INSTANCE.compare(
                Sample.class.getDeclaredField("second"),
                Sample.class.getDeclaredField("first")), is(13));
    }

    @SuppressWarnings("unused")
    private static class Sample {

        private Void first;

        private Void second;
    }
}
