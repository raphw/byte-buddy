package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;

public interface UnboxingResponsible {

    Assignment unboxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType);
}
