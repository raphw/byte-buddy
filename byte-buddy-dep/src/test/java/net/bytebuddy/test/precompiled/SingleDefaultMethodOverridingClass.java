package net.bytebuddy.test.precompiled;

public class SingleDefaultMethodOverridingClass implements SingleDefaultMethodInterface {

    static final String BAR = "BAR";

    @Override
    public Object foo() {
        return BAR;
    }
}
