package net.bytebuddy.asm;

import org.objectweb.asm.MethodVisitor;

public interface MethodVisitorWrapper {

    MethodVisitor wrap(MethodVisitor methodVisitor);
}
