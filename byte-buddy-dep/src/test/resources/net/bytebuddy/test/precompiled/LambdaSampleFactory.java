package net.bytebuddy.test.precompiled;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class LambdaSampleFactory {

    private static final String FOO = "foo";

    private String foo = FOO;

    public Callable<String> nonCapturing() {
        return () -> FOO;
    }

    public Callable<String> argumentCapturing(String foo) {
        return () -> foo;
    }

    public Callable<String> instanceCapturing() {
        return () -> foo;
    }

    public Function<String, String> nonCapturingWithArguments() {
        return argument -> argument;
    }

    public Function<String, String> capturingWithArguments(String foo) {
        return argument -> argument + this.foo + foo;
    }

    public Callable<String> serializable(String foo) {
        return (Callable<String> & Serializable) () -> foo;
    }

    public Runnable returnTypeTransforming() {
        return this::nonCapturing;
    }

    public Callable<Object> instanceReturning() {
        return Object::new;
    }
}