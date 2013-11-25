package com.blogspot.mydailyjava.bytebuddy.asm.method.score;

public class NoScoreException extends RuntimeException {

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
