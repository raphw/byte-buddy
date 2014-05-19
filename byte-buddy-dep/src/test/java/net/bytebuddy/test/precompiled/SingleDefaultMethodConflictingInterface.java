package net.bytebuddy.test.precompiled;

public interface SingleDefaultMethodConflictingInterface {

    static final String QUX = "qux";

    default Object foo() {
        return QUX;
    }
}
