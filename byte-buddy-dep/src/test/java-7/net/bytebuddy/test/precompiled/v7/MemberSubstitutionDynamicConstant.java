package net.bytebuddy.test.precompiled.v7;

import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;

public class MemberSubstitutionDynamicConstant {

    public MemberSubstitutionDynamicConstant foo;

    public MemberSubstitutionDynamicConstant foo() {
        return foo;
    }

    public static MemberSubstitutionDynamicConstant intercept(@MemberSubstitution.DynamicConstant(
            bootstrapType = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
            bootstrapName = "invokedynamic",
            bootstrapOwner = MemberSubstitutionDynamicConstant.class,
            bootstrapReturnType = CallSite.class,
            bootstrapParameterTypes = Object[].class,
            invokedynamic = true) MemberSubstitutionDynamicConstant constant) throws Throwable {
        return constant;
    }

    public static CallSite invokedynamic(Object... args) {
        return new ConstantCallSite(MethodHandles.constant(MemberSubstitutionDynamicConstant.class, new MemberSubstitutionDynamicConstant()));
    }
}
