package net.bytebuddy.utility;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConstructorComparatorTest {

    @Test
    public void testComparisonSame() throws Exception {
        assertThat(ConstructorComparator.INSTANCE.compare(
                Sample.class.getDeclaredConstructor(),
                Sample.class.getDeclaredConstructor()), is(0));
        assertThat(ConstructorComparator.INSTANCE.compare(
                Sample.class.getDeclaredConstructor(String.class),
                Sample.class.getDeclaredConstructor(String.class)), is(0));
    }

    @Test
    public void testComparisonEmptySingle() throws Exception {
        assertThat(ConstructorComparator.INSTANCE.compare(
                Sample.class.getDeclaredConstructor(),
                Sample.class.getDeclaredConstructor(String.class)), is(-1));
        assertThat(ConstructorComparator.INSTANCE.compare(
                Sample.class.getDeclaredConstructor(String.class),
                Sample.class.getDeclaredConstructor()), is(1));
    }

    @Test
    public void testComparisonSingleSingle() throws Exception {
        assertThat(ConstructorComparator.INSTANCE.compare(
                Sample.class.getDeclaredConstructor(String.class),
                Sample.class.getDeclaredConstructor(Integer.class)), is(10));
        assertThat(ConstructorComparator.INSTANCE.compare(
                Sample.class.getDeclaredConstructor(Integer.class),
                Sample.class.getDeclaredConstructor(String.class)), is(-10));
    }

    @SuppressWarnings("unused")
    private static class Sample {

        private Sample() {
            /* empty */
        }

        private Sample(String ignored) {
            /* empty */
        }

        private Sample(Integer ignored) {
            /* empty */
        }
    }
}
