package net.bytebuddy.test.precompiled;

public class SingleDefaultMethodAmbiguousAbstractOverridingClass
        implements SingleDefaultMethodNonOverridingInterface, SingleDefaultMethodAbstractOverridingInterface {

    private static final String FOO = "foo";

    @Override
    public Object foo() {
        return FOO;
    }
}
