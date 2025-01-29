package net.bytebuddy.test.precompiled.v11;

import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.utility.JavaConstant;

import java.lang.invoke.MethodHandles;

public class MemberSubstitutionDynamicConstant {

    public MemberSubstitutionDynamicConstant foo;

    public MemberSubstitutionDynamicConstant foo() {
        return foo;
    }

    public static MemberSubstitutionDynamicConstant intercept(@MemberSubstitution.DynamicConstant(
            bootstrapType = JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
            bootstrapName = "constantdynamic",
            bootstrapOwner = MemberSubstitutionDynamicConstant.class,
            bootstrapReturnType = MemberSubstitutionDynamicConstant.class,
            bootstrapParameterTypes = {MethodHandles.Lookup.class, String.class, Object[].class}) MemberSubstitutionDynamicConstant constant) throws Throwable {
        return constant;
    }

    public static MemberSubstitutionDynamicConstant constantdynamic(MethodHandles.Lookup lookup, String name, Object... args) {
        return new MemberSubstitutionDynamicConstant();
    }
}
