package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;

public interface UnboxingResponsible {

    Assignment unboxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType);
}
