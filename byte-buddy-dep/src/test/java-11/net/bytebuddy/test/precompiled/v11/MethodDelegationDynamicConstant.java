package net.bytebuddy.test.precompiled.v11;

import net.bytebuddy.implementation.bind.annotation.DynamicConstant;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.MethodHandles;

public class MethodDelegationDynamicConstant {

    public MethodDelegationDynamicConstant foo() {
        return null;
    }

    public static MethodDelegationDynamicConstant intercept(@DynamicConstant(
                                    bootstrapType = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                                    bootstrapName = "constantdynamic",
                                    bootstrapOwner = MethodDelegationDynamicConstant.class,
                                    bootstrapReturnType = MethodDelegationDynamicConstant.class,
                                    bootstrapParameterTypes = {MethodHandles.Lookup.class, String.class, Object[].class}) MethodDelegationDynamicConstant constant) throws Throwable {
        return constant;
    }

    public static MethodDelegationDynamicConstant constantdynamic(MethodHandles.Lookup lookup, String name, Object... args) {
        return new MethodDelegationDynamicConstant();
    }
}
