package com.blogspot.mydailyjava.bytebuddy.asm;

import org.objectweb.asm.ClassVisitor;

public interface ClassVisitorWrapper {

    ClassVisitor wrap(ClassVisitor classVisitor);
}
