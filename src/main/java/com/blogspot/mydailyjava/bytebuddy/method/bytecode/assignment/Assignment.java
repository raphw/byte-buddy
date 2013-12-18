package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.OperandStackValueLoader;

public interface Assignment extends OperandStackValueLoader {

    boolean isAssignable();
}
