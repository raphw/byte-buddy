package net.bytebuddy.plugin.test.aar;

import net.bytebuddy.asm.Advice;

public class AarAdvice {

    @Advice.OnMethodExit
    public static void onExit(@Advice.Return(readOnly = false) String value) {
        value = "Instrumented message in lib";
    }
}
