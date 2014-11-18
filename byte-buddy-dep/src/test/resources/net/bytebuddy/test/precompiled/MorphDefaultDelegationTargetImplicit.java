package net.bytebuddy.test.precompiled;

import net.bytebuddy.instrumentation.MethodDelegationMorphTest;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Morph;

public class MorphDefaultDelegationTargetImplicit {

    private static final String BAR = "bar";

    public static String intercept(@Morph(defaultMethod = true)
                                   MethodDelegationMorphTest.Morphing<String> morphing) {
        return morphing.morph(BAR);
    }
}
