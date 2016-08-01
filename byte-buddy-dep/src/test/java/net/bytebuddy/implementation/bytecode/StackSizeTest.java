package net.bytebuddy.implementation.bytecode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class StackSizeTest {

    private final Class<?> type;

    private final int size;

    private final StackSize stackSize;

    public StackSizeTest(Class<?> type, int size, StackSize stackSize) {
        this.type = type;
        this.size = size;
        this.stackSize = stackSize;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {void.class, 0, StackSize.ZERO},
                {boolean.class, 1, StackSize.SINGLE},
                {byte.class, 1, StackSize.SINGLE},
                {short.class, 1, StackSize.SINGLE},
                {int.class, 1, StackSize.SINGLE},
                {char.class, 1, StackSize.SINGLE},
                {float.class, 1, StackSize.SINGLE},
                {long.class, 2, StackSize.DOUBLE},
                {double.class, 2, StackSize.DOUBLE},
                {Object.class, 1, StackSize.SINGLE},
        });
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(StackSize.of(type), is(stackSize));
    }

    @Test
    public void testStackSizeValue() throws Exception {
        assertThat(StackSize.of(type).getSize(), is(size));
    }

    @Test
    public void testStackSizeResolution() throws Exception {
        assertThat(StackSize.of(size), is(stackSize));
    }
}
