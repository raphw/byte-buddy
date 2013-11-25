package com.blogspot.mydailyjava.bytebuddy.asm.interceptor;

import com.blogspot.mydailyjava.bytebuddy.asm.method.MethodUtility;
import com.blogspot.mydailyjava.bytebuddy.instrument.FixedValue;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodCallInterceptor extends ClassVisitor {

    private static final String FIELD_NAME = "callback$bytebuddy";
    private static final String FIXED_VALUE_INTERNAL_NAME = "com/blogspot/mydailyjava/bytebuddy/instrument/FixedValue";
    private static final String FIXED_VALUE_TYPE_NAME = "L" + FIXED_VALUE_INTERNAL_NAME + ";";
    private static final String FIXED_VALUE_METHOD_NAME = "getValue";
    private static final String FIXED_VALUE_DESCRIPTOR = "()Ljava/lang/Object;";

    private static final int SYNTHETIC_PRIVATE_STATIC_FINAL = Opcodes.ACC_SYNTHETIC + Opcodes.ACC_PRIVATE
            + Opcodes.ACC_STATIC; // + FINAL after field set

    private final FixedValue<?> fixedValue;

    private boolean addFields = true;
    private String name;

    public MethodCallInterceptor(ClassVisitor cv, FixedValue<?> fixedValue) {
        super(Opcodes.ASM4, cv);
        this.fixedValue = fixedValue;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = name;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (addFields) {
            addCallbackField();
            addFields = false;
        }
        if (MethodUtility.isOverridable(access)) {
            writeFixedValue(cv.visitMethod(access, name, desc, signature, exceptions));
        }
        return null;
    }

    private void addCallbackField() {
        cv.visitField(SYNTHETIC_PRIVATE_STATIC_FINAL, FIELD_NAME, FIXED_VALUE_TYPE_NAME, null, null).visitEnd();
    }

    private void writeFixedValue(MethodVisitor methodVisitor) {
        if (methodVisitor == null) {
            return;
        }
        methodVisitor.visitCode();
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, name, FIELD_NAME, FIXED_VALUE_TYPE_NAME);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, FIXED_VALUE_INTERNAL_NAME, FIXED_VALUE_METHOD_NAME, FIXED_VALUE_DESCRIPTOR);
        methodVisitor.visitInsn(Opcodes.POP);
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
    }
}
