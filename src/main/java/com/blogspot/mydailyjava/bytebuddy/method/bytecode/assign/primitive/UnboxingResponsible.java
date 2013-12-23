package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;

public interface UnboxingResponsible {

    static final String BOOLEAN_TYPE_NAME = "Ljava/lang/Boolean;";
    static final String BYTE_TYPE_NAME = "Ljava/lang/Byte;";
    static final String SHORT_TYPE_NAME = "Ljava/lang/Short;";
    static final String CHARACTER_TYPE_NAME = "Ljava/lang/Character;";
    static final String INTEGER_TYPE_NAME = "Ljava/lang/Integer;";
    static final String LONG_TYPE_NAME = "Ljava/lang/Long;";
    static final String FLOAT_TYPE_NAME = "Ljava/lang/Float;";
    static final String DOUBLE_TYPE_NAME = "Ljava/lang/Double;";

    Assignment unboxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType);

    Assignment unboxAndAssignTo(String subTypeName, Assigner assigner, boolean considerRuntimeType);
}
