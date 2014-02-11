package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryClass;

public interface Instrumentation {

    static interface Context {

        AuxiliaryClass.Named register(AuxiliaryClass auxiliaryClass);

        MethodDescription accessor(MethodDescription methodDescription);

        void append(ClassLoadingCallback classLoadingCallback);
    }

    static interface ClassLoadingCallback {

        void onLoad(Class<?> type);
    }

    InstrumentedType prepare(InstrumentedType instrumentedType);

    ByteCodeAppender appender(InstrumentedType instrumentedType, Context context);
}
