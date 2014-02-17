package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryClass;

public interface Instrumentation {

    static interface Context {

        AuxiliaryClass.Named register(AuxiliaryClass auxiliaryClass);
    }

    InstrumentedType prepare(InstrumentedType instrumentedType);

    ByteCodeAppender appender(TypeDescription instrumentedType, Context context);
}
