package net.bytebuddy.test.precompiled;

public class SingleDefaultMethodConflictingClass implements SingleDefaultMethodInterface, SingleDefaultMethodConflictingInterface {

    static final String BAR = "BAR";

    @Override
    public Object foo() {
        return BAR;
    }
}
