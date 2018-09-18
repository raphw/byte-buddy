package net.bytebuddy.test.precompiled;

public interface DelegationDefaultInterface {

    static final String FOO = "foo";

    default String foo() {
        return FOO;
    }
}
