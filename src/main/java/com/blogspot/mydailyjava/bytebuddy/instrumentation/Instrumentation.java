package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public interface Instrumentation {

    static interface Context {

        AuxiliaryType.Named register(AuxiliaryType auxiliaryType);
    }

    static interface ClassLoadingCallback {

        static class Compound implements Instrumentation.ClassLoadingCallback {

            public static Instrumentation.ClassLoadingCallback of(List<? extends ClassLoadingCallback> classLoadingCallbacks) {
                boolean serializable = true;
                Iterator<? extends ClassLoadingCallback> iterator = classLoadingCallbacks.iterator();
                while (serializable && iterator.hasNext()) {
                    serializable = iterator.next() instanceof Serializable;
                }
                return serializable ? new SerializableForm(classLoadingCallbacks) : new Compound(classLoadingCallbacks);
            }

            private static class SerializableForm extends Compound implements Serializable {

                protected SerializableForm(List<? extends Instrumentation.ClassLoadingCallback> classLoadingCallbacks) {
                    super(classLoadingCallbacks);
                }
            }

            private final List<? extends Instrumentation.ClassLoadingCallback> classLoadingCallbacks;

            public Compound(List<? extends Instrumentation.ClassLoadingCallback> classLoadingCallbacks) {
                this.classLoadingCallbacks = classLoadingCallbacks;
            }

            @Override
            public void onLoad(Class<?> type) {
                for (Instrumentation.ClassLoadingCallback callback : classLoadingCallbacks) {
                    callback.onLoad(type);
                }
            }
        }

        void onLoad(Class<?> type);
    }

    InstrumentedType prepare(InstrumentedType instrumentedType);

    ByteCodeAppender appender(TypeDescription instrumentedType, Context context);
}
