package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodInvocation {

    VIRTUAL(Opcodes.INVOKEVIRTUAL),
    INTERFACE(Opcodes.INVOKEINTERFACE),
    STATIC(Opcodes.INVOKESTATIC),
    SPECIAL(Opcodes.INVOKESPECIAL);

    public static interface WithImplicitInvocationTargetType extends Assignment {

        Assignment virtual(TypeDescription typeDescription);

        Assignment special(TypeDescription typeDescription);
    }

    private class Invocation implements WithImplicitInvocationTargetType {

        private final TypeDescription typeDescription;
        private final MethodDescription methodDescription;
        private final Size size;

        private Invocation(MethodDescription methodDescription) {
            this(methodDescription, methodDescription.getDeclaringType());
        }

        private Invocation(MethodDescription methodDescription, TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
            this.methodDescription = methodDescription;
            int parameterSize = methodDescription.getStackSize();
            int returnValueSize = methodDescription.getReturnType().getStackSize().getSize();
            size = new Size(returnValueSize - parameterSize, Math.max(0, returnValueSize - parameterSize));
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitMethodInsn(invocationOpcode,
                    typeDescription.getInternalName(),
                    methodDescription.getInternalName(),
                    methodDescription.getDescriptor());
            return size;
        }

        @Override
        public Assignment virtual(TypeDescription typeDescription) {
            validateNonStaticAndTypeCompatibleCall(typeDescription);
            if (methodDescription.isPrivate() || methodDescription.isConstructor()) {
                throw new IllegalArgumentException("Cannot invoke " + typeDescription + " virtually");
            }
            if (typeDescription.isInterface()) {
                return INTERFACE.new Invocation(methodDescription, typeDescription);
            } else {
                return VIRTUAL.new Invocation(methodDescription, typeDescription);
            }
        }

        @Override
        public Assignment special(TypeDescription typeDescription) {
            validateNonStaticAndTypeCompatibleCall(typeDescription);
            if((methodDescription.isPrivate() || methodDescription.isConstructor())) {
                if(this.typeDescription.equals(typeDescription)) {
                    return this;
                } else {
                    throw new IllegalArgumentException("Cannot invoke " + methodDescription + " on any other type");
                }
            }
            if (typeDescription.isInterface()) {
                throw new IllegalStateException("Cannot call INVOKESPECIAL on interface type");
            } else {
                return SPECIAL.new Invocation(methodDescription, typeDescription);
            }
        }

        private void validateNonStaticAndTypeCompatibleCall(TypeDescription typeDescription) {
            if (methodDescription.isStatic()) {
                throw new IllegalStateException("Method " + methodDescription + " is bound to " + this.typeDescription);
            } else if (!this.typeDescription.isAssignableFrom(typeDescription)) {
                throw new IllegalStateException("Method " + methodDescription + " cannot be called on " + typeDescription);
            }
        }
    }

    public static WithImplicitInvocationTargetType invoke(MethodDescription methodDescription) {
        if (methodDescription.isStatic()) { // Check this property first, private static methods use INVOKESTATIC.
            return STATIC.new Invocation(methodDescription);
        } else if (methodDescription.isPrivate() || methodDescription.isConstructor()) {
            return SPECIAL.new Invocation(methodDescription);
        } else if (methodDescription.getDeclaringType().isInterface()) {
            return INTERFACE.new Invocation(methodDescription);
        } else {
            return VIRTUAL.new Invocation(methodDescription);
        }
    }

    private final int invocationOpcode;

    private MethodInvocation(int callOpcode) {
        this.invocationOpcode = callOpcode;
    }
}
