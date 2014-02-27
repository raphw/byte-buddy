package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A builder for a method invocation.
 */
public enum MethodInvocation {

    VIRTUAL(Opcodes.INVOKEVIRTUAL),
    INTERFACE(Opcodes.INVOKEINTERFACE),
    STATIC(Opcodes.INVOKESTATIC),
    SPECIAL(Opcodes.INVOKESPECIAL);

    /**
     * Represents a method invocation where the invocation type (static, virtual, special, interface) is derived
     * from the
     */
    public static interface WithImplicitInvocationTargetType extends StackManipulation {

        /**
         * Transforms this method invocation into a virtual (or interface) method invocation on the given type. If the
         * represented method cannot be dispatched on the given invocation target type, an exception is thrown.
         *
         * @param invocationTarget The type on which the method is to be invoked virtually on.
         * @return A stack manipulation representing this method invocation.
         */
        StackManipulation virtual(TypeDescription invocationTarget);

        /**
         * Transforms this method invocation into a virtual (or interface) method invocation on the given type. If the
         * represented method cannot be dispatched on the given invocation target type, an exception is thrown.
         *
         * @param invocationTarget The type on which the method is to be invoked specially on.
         * @return A stack manipulation representing this method invocation.
         */
        StackManipulation special(TypeDescription invocationTarget);

        /**
         * Returns the invocation type that was determined implicitly for the given method.
         *
         * @return The method invocation type that was determined implicitly for the given method.
         */
        MethodInvocation getImplicitInvocationType();
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
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitMethodInsn(invocationOpcode,
                    typeDescription.getInternalName(),
                    methodDescription.getInternalName(),
                    methodDescription.getDescriptor());
            return size;
        }

        @Override
        public StackManipulation virtual(TypeDescription invocationTarget) {
            validateNonStaticAndTypeCompatibleCall(invocationTarget);
            if (methodDescription.isPrivate() || methodDescription.isConstructor()) {
                throw new IllegalArgumentException("Cannot invoke " + invocationTarget + " virtually");
            }
            if (invocationTarget.isInterface()) {
                return INTERFACE.new Invocation(methodDescription, invocationTarget);
            } else {
                return VIRTUAL.new Invocation(methodDescription, invocationTarget);
            }
        }

        @Override
        public StackManipulation special(TypeDescription invocationTarget) {
            validateNonStaticAndTypeCompatibleCall(invocationTarget);
            if ((methodDescription.isPrivate() || methodDescription.isConstructor())) {
                if (this.typeDescription.equals(invocationTarget)) {
                    return this;
                } else {
                    throw new IllegalArgumentException("Cannot invoke " + methodDescription + " on any other type");
                }
            }
            if (invocationTarget.isInterface()) {
                throw new IllegalArgumentException("Cannot call INVOKESPECIAL on interface type");
            } else {
                return SPECIAL.new Invocation(methodDescription, invocationTarget);
            }
        }

        private void validateNonStaticAndTypeCompatibleCall(TypeDescription typeDescription) {
            if (methodDescription.isStatic()) {
                throw new IllegalStateException("Method " + methodDescription + " is bound to " + this.typeDescription);
            } else if (!this.typeDescription.isAssignableFrom(typeDescription)) {
                throw new IllegalArgumentException("Method " + methodDescription + " cannot be called on " + typeDescription);
            }
        }

        @Override
        public MethodInvocation getImplicitInvocationType() {
            return MethodInvocation.this;
        }
    }

    /**
     * Creates a method invocation with an implicitly determined invocation type.
     *
     * @param methodDescription The method to be invoked.
     * @return A stack manipulation with implicitly determined invocation type.
     */
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
