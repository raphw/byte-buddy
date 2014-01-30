package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

public class MethodArgumentVoidTest {

    @Test(expected = IllegalArgumentException.class)
    public void testVoidArgument() throws Exception {
        MethodArgument.forType(new TypeDescription.ForLoadedType(void.class));
    }
}
