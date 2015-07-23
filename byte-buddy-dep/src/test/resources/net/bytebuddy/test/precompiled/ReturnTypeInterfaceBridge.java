package net.bytebuddy.test.precompiled;

public interface ReturnTypeInterfaceBridge extends ReturnTypeInterfaceBridgeBase {

    String BAR = "bar";

    @Override
    default String foo() {
        return BAR;
    }
}
