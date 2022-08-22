package net.bytebuddy.plugin.test.jar;

import net.bytebuddy.asm.Advice;

public class JarAdvice {

    @Advice.OnMethodExit
    public static void enter(@Advice.Return(readOnly = false) String value) {
        value = "Instrumented message in lib";
    }
}
