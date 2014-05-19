package net.bytebuddy.test.precompiled;

public class SingleDefaultMethodAbstractOverridingClass implements SingleDefaultMethodAbstractOverridingInterface {

    static final String BAR = "bar";

    @Override
    public Object foo() {
        return BAR;
    }
}
