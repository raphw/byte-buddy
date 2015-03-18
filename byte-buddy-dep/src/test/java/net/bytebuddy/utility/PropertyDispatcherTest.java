package net.bytebuddy.utility;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class PropertyDispatcherTest {

    private static final String FOO = "foo";

    private final Object value, other;

    private final PropertyDispatcher expectedPropertyDispatcher;

    private final int expectedHashCode;

    private final String expectedToString;

    public PropertyDispatcherTest(Object value,
                                  Object other,
                                  PropertyDispatcher expectedPropertyDispatcher,
                                  int expectedHashCode,
                                  String expectedToString) {
        this.value = value;
        this.other = other;
        this.expectedPropertyDispatcher = expectedPropertyDispatcher;
        this.expectedHashCode = expectedHashCode;
        this.expectedToString = expectedToString;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new boolean[]{true}, new boolean[]{true}, PropertyDispatcher.BOOLEAN_ARRAY, Arrays.hashCode(new boolean[]{true}), Arrays.toString(new boolean[]{true})},
                {new byte[]{42}, new byte[]{42}, PropertyDispatcher.BYTE_ARRAY, Arrays.hashCode(new byte[]{42}), Arrays.toString(new byte[]{42})},
                {new short[]{42}, new short[]{42}, PropertyDispatcher.SHORT_ARRAY, Arrays.hashCode(new short[]{42}), Arrays.toString(new short[]{42})},
                {new char[]{42}, new char[]{42}, PropertyDispatcher.CHARACTER_ARRAY, Arrays.hashCode(new char[]{42}), Arrays.toString(new char[]{42})},
                {new int[]{42}, new int[]{42}, PropertyDispatcher.INTEGER_ARRAY, Arrays.hashCode(new int[]{42}), Arrays.toString(new int[]{42})},
                {new long[]{42L}, new long[]{42L}, PropertyDispatcher.LONG_ARRAY, Arrays.hashCode(new long[]{42L}), Arrays.toString(new long[]{42L})},
                {new float[]{42f}, new float[]{42f}, PropertyDispatcher.FLOAT_ARRAY, Arrays.hashCode(new float[]{42f}), Arrays.toString(new float[]{42f})},
                {new double[]{42d}, new double[]{42d}, PropertyDispatcher.DOUBLE_ARRAY, Arrays.hashCode(new double[]{42d}), Arrays.toString(new double[]{42d})},
                {new Object[]{FOO}, new Object[]{FOO}, PropertyDispatcher.REFERENCE_ARRAY, Arrays.hashCode(new Object[]{FOO}), Arrays.toString(new Object[]{FOO})},
                {FOO, FOO, PropertyDispatcher.NON_ARRAY, FOO.hashCode(), FOO}
        });
    }

    @Test
    public void testPropertyDispatcher() throws Exception {
        assertThat(PropertyDispatcher.of(value.getClass()), is(expectedPropertyDispatcher));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(PropertyDispatcher.of(value.getClass()).hashCode(value), is(expectedHashCode));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(PropertyDispatcher.of(value.getClass()).toString(value), is(expectedToString));
    }

    @Test
    public void testEquals() throws Exception {
        assertThat(PropertyDispatcher.of(value.getClass()).equals(value, value), is(true));
        assertThat(PropertyDispatcher.of(value.getClass()).equals(value, null), is(false));
        assertThat(PropertyDispatcher.of(value.getClass()).equals(value, new Object()), is(false));
        assertThat(PropertyDispatcher.of(value.getClass()).equals(value, other), is(true));
    }

    @Test
    public void testConditionalClone() throws Exception {
        assertThat(PropertyDispatcher.of(value.getClass()).conditionalClone(value), is(value));
        if (value.getClass().isArray()) {
            assertThat(PropertyDispatcher.of(value.getClass()).conditionalClone(value), not(sameInstance(value)));
        } else {
            assertThat(PropertyDispatcher.of(value.getClass()).conditionalClone(value), sameInstance(value));
        }
    }
}
