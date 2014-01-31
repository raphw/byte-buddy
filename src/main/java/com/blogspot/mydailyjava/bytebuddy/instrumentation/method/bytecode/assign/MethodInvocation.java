package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodInvocation {

    VIRTUAL(Opcodes.INVOKEVIRTUAL),
    INTERFACE(Opcodes.INVOKEINTERFACE),
    STATIC(Opcodes.INVOKESTATIC),
    SPECIAL(Opcodes.INVOKESPECIAL);

    private class Invocation implements Assignment {

        private final MethodDescription methodDescription;
        private final Size size;

        private Invocation(MethodDescription methodDescription) {
            this.methodDescription = methodDescription;
            int parameterSize = methodDescription.getStackSize();
            int returnValueSize = methodDescription.getReturnType().getStackSize().getSize();
            this.size = new Size(returnValueSize - parameterSize, Math.max(0, returnValueSize - parameterSize));
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitMethodInsn(invocationOpcode,
                    methodDescription.getDeclaringType().getInternalName(),
                    methodDescription.getInternalName(),
                    methodDescription.getDescriptor());
            return size;
        }
    }

    public static Assignment of(MethodDescription methodDescription) {
        if (methodDescription.isStatic()) {
            return STATIC.new Invocation(methodDescription);
        } else if (methodDescription.isDeclaredInInterface()) {
            return INTERFACE.new Invocation(methodDescription);
        } else {
            return VIRTUAL.new Invocation(methodDescription);
        }
    }

    public static Assignment special(MethodDescription methodDescription) {
        if (methodDescription.isStatic() || methodDescription.isDeclaredInInterface()) {
            throw new IllegalArgumentException("Cannot invoke " + methodDescription + " via INVOKESPECIAL");
        } else {
            return SPECIAL.new Invocation(methodDescription);
        }
    }

    private final int invocationOpcode;

    private MethodInvocation(int callOpcode) {
        this.invocationOpcode = callOpcode;
    }
}
