package net.bytebuddy.test.precompiled.v11;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.MethodHandles;

public class DynamicConstantAdvice {

    public DynamicConstantAdvice foo() {
        return null;
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.DynamicConstant(
                                    bootstrapType = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                                    bootstrapName = "constantdynamic",
                                    bootstrapOwner = DynamicConstantAdvice.class,
                                    bootstrapReturnType = DynamicConstantAdvice.class,
                                    bootstrapParameterTypes = {MethodHandles.Lookup.class, String.class, Object[].class}) DynamicConstantAdvice constant,
                            @Advice.Return(readOnly = false) DynamicConstantAdvice returned) throws Throwable {
        returned = constant;
    }

    public static DynamicConstantAdvice constantdynamic(MethodHandles.Lookup lookup, String name, Object... args) {
        return new DynamicConstantAdvice();
    }
}
