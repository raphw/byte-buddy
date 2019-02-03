package net.bytebuddy.test.packaging;

import net.bytebuddy.asm.Advice;

public class AdviceTestHelper {

    private Object object;

    @Advice.OnMethodEnter(inline = false)
    private static void enter() {
        /* empty */
    }
}
