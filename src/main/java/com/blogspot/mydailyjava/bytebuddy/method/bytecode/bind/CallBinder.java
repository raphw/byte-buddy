package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;

import java.lang.reflect.Method;

public interface CallBinder {

    static interface Factory {

        CallBinder make(ClassContext classContext, MethodContext methodContext);
    }

    BoundCall bind(Method target);
}
