package com.blogspot.mydailyjava.bytebuddy.asm.method.stack;

public class ArgumentReference implements MethodCallStackValue {

    private final int index;

    public ArgumentReference(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
