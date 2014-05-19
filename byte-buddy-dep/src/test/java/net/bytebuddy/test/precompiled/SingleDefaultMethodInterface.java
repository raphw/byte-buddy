package net.bytebuddy.test.precompiled;

public interface SingleDefaultMethodInterface {

    static final String FOO = "foo";

    default Object foo() {
        return FOO;
    }
}
