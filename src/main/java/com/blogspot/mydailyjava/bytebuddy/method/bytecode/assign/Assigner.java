package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

public interface Assigner {

    Assignment assign(Class<?> superType, Class subType, boolean considerRuntimeType);
}
