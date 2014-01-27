package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodInvocation {

    CONCRETE(Opcodes.INVOKEVIRTUAL),
    INTERFACE(Opcodes.INVOKEINTERFACE),
    STATIC(Opcodes.INVOKESTATIC),
    SPECIAL(Opcodes.INVOKESPECIAL);

    private class Invocation implements Assignment {

        private final MethodDescription methodDescription;
        private final Size size;

        private Invocation(MethodDescription methodDescription) {
            this.methodDescription = methodDescription;
            int parameterSize = TypeSize.sizeOf(methodDescription);
            int returnValueSize = TypeSize.of(methodDescription.getReturnType()).getSize();
            this.size = new Size(returnValueSize - parameterSize, Math.max(0, returnValueSize - parameterSize));
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitMethodInsn(invocationOpcode,
                    methodDescription.getDeclaringClassInternalName(),
                    methodDescription.getInternalName(),
                    methodDescription.getDescriptor());
            return size;
        }
    }

    public static Assignment of(MethodDescription methodDescription) {
        if (methodDescription.isStatic()) {
            return STATIC.new Invocation(methodDescription);
        } else if (methodDescription.isInterfaceMethod()) {
            return INTERFACE.new Invocation(methodDescription);
        } else {
            return CONCRETE.new Invocation(methodDescription);
        }
    }

    public static Assignment special(MethodDescription methodDescription) {
        if (methodDescription.isStatic()) {
            throw new IllegalArgumentException("Cannot invoke static method via INVOKESPECIAL");
        } else {
            return SPECIAL.new Invocation(methodDescription);
        }
    }

    private final int invocationOpcode;

    private MethodInvocation(int callOpcode) {
        this.invocationOpcode = callOpcode;
    }
}
