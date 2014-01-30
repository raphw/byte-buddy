package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

public interface Assigner {

    Assignment assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType);
}
