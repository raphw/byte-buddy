package com.transformations;

import net.bytebuddy.asm.Advice;

public class BasicAdvice {

    @Advice.OnMethodExit
    public static void enter(@Advice.Return(readOnly = false) String value) {
        value = "Instrumented message in lib";
    }
}