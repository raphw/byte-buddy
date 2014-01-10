package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind;

import java.lang.reflect.Method;

public interface CallBinder {

    BoundCall bind(Method target);
}
