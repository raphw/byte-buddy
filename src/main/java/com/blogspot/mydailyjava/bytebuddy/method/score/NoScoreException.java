package com.blogspot.mydailyjava.bytebuddy.method.score;

public class NoScoreException extends RuntimeException {

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
