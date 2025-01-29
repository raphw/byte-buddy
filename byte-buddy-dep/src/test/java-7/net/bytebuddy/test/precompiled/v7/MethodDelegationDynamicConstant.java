package net.bytebuddy.test.precompiled.v7;

import net.bytebuddy.implementation.bind.annotation.DynamicConstant;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;

public class MethodDelegationDynamicConstant {

    public MethodDelegationDynamicConstant foo() {
        return null;
    }

    public static MethodDelegationDynamicConstant intercept(@DynamicConstant(
            bootstrapType = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
            bootstrapName = "invokedynamic",
            bootstrapOwner = MethodDelegationDynamicConstant.class,
            bootstrapReturnType = CallSite.class,
            bootstrapParameterTypes = Object[].class,
            invokedynamic = true) MethodDelegationDynamicConstant constant) throws Throwable {
        return constant;
    }

    public static CallSite invokedynamic(Object... args) {
        return new ConstantCallSite(MethodHandles.constant(MethodDelegationDynamicConstant.class, new MethodDelegationDynamicConstant()));
    }
}
