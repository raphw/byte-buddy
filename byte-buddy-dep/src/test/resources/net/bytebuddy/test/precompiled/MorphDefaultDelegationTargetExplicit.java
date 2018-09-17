package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.MethodDelegationMorphTest;
import net.bytebuddy.implementation.bind.annotation.Morph;

public class MorphDefaultDelegationTargetExplicit {

    private static final String BAR = "bar";

    public static String intercept(@Morph(defaultTarget = MorphDefaultInterface.class)
                                           MethodDelegationMorphTest.Morphing<String> morphing) {
        return morphing.morph(BAR);
    }
}
