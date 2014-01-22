package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.junit.Test;

public class MethodArgumentVoidTest {

    @Test(expected = IllegalArgumentException.class)
    public void testVoidArgument() throws Exception {
        MethodArgument.forType(void.class);
    }
}
