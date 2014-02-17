package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

public interface Assigner {

    StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType);
}
