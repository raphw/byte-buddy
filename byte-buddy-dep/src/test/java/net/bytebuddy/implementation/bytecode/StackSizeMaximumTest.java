package net.bytebuddy.implementation.bytecode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class StackSizeMaximumTest {

    private final StackSize first, second, expected;

    public StackSizeMaximumTest(StackSize first, StackSize second, StackSize expected) {
        this.first = first;
        this.second = second;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {StackSize.DOUBLE, StackSize.ZERO, StackSize.DOUBLE},
                {StackSize.DOUBLE, StackSize.SINGLE, StackSize.DOUBLE},
                {StackSize.DOUBLE, StackSize.DOUBLE, StackSize.DOUBLE},
                {StackSize.SINGLE, StackSize.DOUBLE, StackSize.DOUBLE},
                {StackSize.SINGLE, StackSize.SINGLE, StackSize.SINGLE},
                {StackSize.SINGLE, StackSize.ZERO, StackSize.SINGLE},
                {StackSize.ZERO, StackSize.DOUBLE, StackSize.DOUBLE},
                {StackSize.ZERO, StackSize.SINGLE, StackSize.SINGLE},
                {StackSize.ZERO, StackSize.ZERO, StackSize.ZERO},
        });
    }

    @Test
    public void testMaximum() throws Exception {
        assertThat(first.maximum(second), is(expected));
    }
}
