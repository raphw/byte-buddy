package com.blogspot.mydailyjava.bytebuddy.interceptor;

import com.blogspot.mydailyjava.bytebuddy.method.MethodUtility;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.concurrent.atomic.AtomicLong;

public class ProxyClassHierarchyInterceptor extends ClassVisitor {

    private static final AtomicLong UNIQUE_NAME_GENERATOR = new AtomicLong();

    private String superClassName;

    public ProxyClassHierarchyInterceptor(ClassVisitor cv) {
        super(Opcodes.ASM4, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.superClassName = name;
        cv.visit(version, access, makeSubclassName(name), signature, name, interfaces);
    }

    private static String makeSubclassName(String name) {
        return String.format("%s$bytebuddy$%d", name, UNIQUE_NAME_GENERATOR.getAndIncrement());
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (MethodUtility.CONSTRUCTOR_METHOD_NAME.equals(name)) {
            if (MethodUtility.isOverridable(access)) {
                callSuperConstructor(cv.visitMethod(access, MethodUtility.CONSTRUCTOR_METHOD_NAME, desc, signature, exceptions), desc);
            }
            return null;
        } else {
            return cv.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private void callSuperConstructor(MethodVisitor methodVisitor, String desc) {
        if (methodVisitor == null) {
            return;
        }
        methodVisitor.visitCode();
        int size = MethodUtility.loadThisAndArgumentsOnStack(methodVisitor, desc);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, MethodUtility.CONSTRUCTOR_METHOD_NAME, desc);
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(size, size);
        methodVisitor.visitEnd();
    }
}
