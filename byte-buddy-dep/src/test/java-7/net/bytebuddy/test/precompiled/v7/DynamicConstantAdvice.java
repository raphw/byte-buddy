package net.bytebuddy.test.precompiled.v7;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;

public class DynamicConstantAdvice {

    public DynamicConstantAdvice foo() {
        return null;
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.DynamicConstant(bootstrap = @Advice.Handle(
                                    type = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                                    name = "invokedynamic",
                                    owner = DynamicConstantAdvice.class,
                                    returnType = CallSite.class,
                                    parameterTypes = Object[].class), invokedynamic = true) DynamicConstantAdvice constant,
                            @Advice.Return(readOnly = false) DynamicConstantAdvice returned) throws Throwable {
        returned = constant;
    }

    public static CallSite invokedynamic(Object... args) {
        return new ConstantCallSite(MethodHandles.constant(DynamicConstantAdvice.class, new DynamicConstantAdvice()));
    }
}
