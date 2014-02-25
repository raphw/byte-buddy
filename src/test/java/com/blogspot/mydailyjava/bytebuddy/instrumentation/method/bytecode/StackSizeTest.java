package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class StackSizeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {void.class, StackSize.ZERO},
                {boolean.class, StackSize.SINGLE},
                {byte.class, StackSize.SINGLE},
                {short.class, StackSize.SINGLE},
                {int.class, StackSize.SINGLE},
                {char.class, StackSize.SINGLE},
                {float.class, StackSize.SINGLE},
                {long.class, StackSize.DOUBLE},
                {double.class, StackSize.DOUBLE},
                {Object.class, StackSize.SINGLE},
        });
    }

    private final Class<?> type;
    private final StackSize stackSize;

    public StackSizeTest(Class<?> type, StackSize stackSize) {
        this.type = type;
        this.stackSize = stackSize;
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(StackSize.of(type), is(stackSize));
    }
}
