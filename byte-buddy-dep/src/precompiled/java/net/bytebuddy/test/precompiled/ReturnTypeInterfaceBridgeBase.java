package net.bytebuddy.test.precompiled;

public interface ReturnTypeInterfaceBridgeBase {

    String FOO = "foo";

    default Object foo() {
        return FOO;
    }
}
