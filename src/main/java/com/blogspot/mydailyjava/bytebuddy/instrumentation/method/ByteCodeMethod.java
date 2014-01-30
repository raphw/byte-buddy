package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ByteCodeElement;

public interface ByteCodeMethod extends ByteCodeElement {

    String getUniqueSignature();
}
