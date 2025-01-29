package net.bytebuddy.test.precompiled.v11;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.MethodHandles;

public class AdviceDynamicConstant {

    public AdviceDynamicConstant foo() {
        return null;
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.DynamicConstant(
                                    bootstrapType = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                                    bootstrapName = "constantdynamic",
                                    bootstrapOwner = AdviceDynamicConstant.class,
                                    bootstrapReturnType = AdviceDynamicConstant.class,
                                    bootstrapParameterTypes = {MethodHandles.Lookup.class, String.class, Object[].class}) AdviceDynamicConstant constant,
                            @Advice.Return(readOnly = false) AdviceDynamicConstant returned) throws Throwable {
        returned = constant;
    }

    public static AdviceDynamicConstant constantdynamic(MethodHandles.Lookup lookup, String name, Object... args) {
        return new AdviceDynamicConstant();
    }
}
