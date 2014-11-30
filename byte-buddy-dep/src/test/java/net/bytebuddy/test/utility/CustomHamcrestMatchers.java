package net.bytebuddy.test.utility;

import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

public class CustomHamcrestMatchers {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @SuppressWarnings("unchecked")
    public static <T> Matcher<Iterable<T>> containsAllOf(Collection<T> items) {
        // The Java compiler only accepts this type casting when it is confused by the additional object type casting
        return (Matcher<Iterable<T>>) (Object) hasItems(items.toArray(new Object[items.size()]));
    }

    @Test
    public void testContainMatcherSucceeds() throws Exception {
        assertThat(Arrays.asList(FOO, BAR, QUX), containsAllOf(Arrays.asList(QUX, FOO, BAR)));
    }

    @Test(expected = AssertionError.class)
    public void testContainMatcherFails() throws Exception {
        assertThat(Arrays.asList(FOO, BAR, QUX), containsAllOf(Arrays.asList(QUX, FOO, BAZ)));
    }
}
