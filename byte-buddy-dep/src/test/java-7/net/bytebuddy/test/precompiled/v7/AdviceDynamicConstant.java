package net.bytebuddy.test.precompiled.v7;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;

public class AdviceDynamicConstant {

    public AdviceDynamicConstant foo() {
        return null;
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.DynamicConstant(
                                    bootstrapType = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                                    bootstrapName = "invokedynamic",
                                    bootstrapOwner = AdviceDynamicConstant.class,
                                    bootstrapReturnType = CallSite.class,
                                    bootstrapParameterTypes = Object[].class,
                                    invokedynamic = true) AdviceDynamicConstant constant,
                            @Advice.Return(readOnly = false) AdviceDynamicConstant returned) throws Throwable {
        returned = constant;
    }

    public static CallSite invokedynamic(Object... args) {
        return new ConstantCallSite(MethodHandles.constant(AdviceDynamicConstant.class, new AdviceDynamicConstant()));
    }
}
