package com.blogspot.mydailyjava.bytebuddy.instrument;

public interface FixedValue<T> extends Callback {

    T getValue();
}
