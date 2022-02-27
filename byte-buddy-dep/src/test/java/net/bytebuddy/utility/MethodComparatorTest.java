package net.bytebuddy.utility;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodComparatorTest {

    @Test
    public void testComparisonSame() throws Exception {
        assertThat(MethodComparator.INSTANCE.compare(
                Sample.class.getDeclaredMethod("method"),
                Sample.class.getDeclaredMethod("method")), is(0));
        assertThat(MethodComparator.INSTANCE.compare(
                Sample.class.getDeclaredMethod("method", String.class),
                Sample.class.getDeclaredMethod("method", String.class)), is(0));
    }

    @Test
    public void testComparisonEmptySingle() throws Exception {
        assertThat(MethodComparator.INSTANCE.compare(
                Sample.class.getDeclaredMethod("method"),
                Sample.class.getDeclaredMethod("method", String.class)), is(-1));
        assertThat(MethodComparator.INSTANCE.compare(
                Sample.class.getDeclaredMethod("method", String.class),
                Sample.class.getDeclaredMethod("method")), is(1));
    }

    @Test
    public void testComparisonSingleSingle() throws Exception {
        assertThat(MethodComparator.INSTANCE.compare(
                Sample.class.getDeclaredMethod("method", String.class),
                Sample.class.getDeclaredMethod("method", Integer.class)), is(10));
        assertThat(MethodComparator.INSTANCE.compare(
                Sample.class.getDeclaredMethod("method", Integer.class),
                Sample.class.getDeclaredMethod("method", String.class)), is(-10));
    }

    @SuppressWarnings("unused")
    private static class Sample {

        private void method() {
            /* empty */
        }

        private void method(String ignored) {
            /* empty */
        }

        private void method(Integer ignored) {
            /* empty */
        }
    }
}
