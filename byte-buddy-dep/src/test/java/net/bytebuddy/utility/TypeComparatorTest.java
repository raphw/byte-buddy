package net.bytebuddy.utility;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeComparatorTest {

    @Test
    public void testTypeComparison() {
        assertThat(TypeComparator.INSTANCE.compare(Object.class, Object.class) == 0, is(true));
        assertThat(TypeComparator.INSTANCE.compare(Object.class, String.class) < 0, is(true));
        assertThat(TypeComparator.INSTANCE.compare(String.class, Object.class) > 0, is(true));
    }

    @Test
    public void testNullSafe() {
        assertThat(TypeComparator.INSTANCE.compare(null, null) == 0, is(true));
        assertThat(TypeComparator.INSTANCE.compare(null, Object.class) > 0, is(true));
        assertThat(TypeComparator.INSTANCE.compare(Object.class, null) < 0, is(true));
    }
}
