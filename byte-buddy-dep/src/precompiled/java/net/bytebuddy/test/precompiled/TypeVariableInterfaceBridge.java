package net.bytebuddy.test.precompiled;

public interface TypeVariableInterfaceBridge extends TypeVariableInterfaceBridgeBase<String> {

    String FOO = "foo";

    @Override
    default String foo(String s) {
        return FOO;
    }
}
