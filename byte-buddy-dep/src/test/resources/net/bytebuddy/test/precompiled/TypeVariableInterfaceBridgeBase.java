package net.bytebuddy.test.precompiled;

public interface TypeVariableInterfaceBridgeBase<T> {

    default T foo(T t) {
        return t;
    }
}
