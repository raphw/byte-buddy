package net.bytebuddy.test.precompiled;

public interface MorphDefaultInterface {

    static final String FOO = "foo";

    default String foo(String value) {
        return FOO + value;
    }
}
