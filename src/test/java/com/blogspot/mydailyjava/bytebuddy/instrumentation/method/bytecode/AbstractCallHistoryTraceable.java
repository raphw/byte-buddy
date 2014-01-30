package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

public abstract class AbstractCallHistoryTraceable {

    public static final String METHOD_NAME = "getNumberOfCalls";

    private int numberOfCalls;

    @SuppressWarnings("unused")
    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    public void markCalled() {
        numberOfCalls++;
    }
}
