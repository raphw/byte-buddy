package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

public interface Assigner {

    Assignment assign(Class<?> sourceType, Class<?> targetType, boolean considerRuntimeType);
}
