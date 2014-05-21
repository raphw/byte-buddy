package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A builder for a method invocation.
 */
public enum MethodInvocation {

    /**
     * A virtual method invocation.
     */
    VIRTUAL(Opcodes.INVOKEVIRTUAL),

    /**
     * An interface-typed virtual method invocation.
     */
    INTERFACE(Opcodes.INVOKEINTERFACE),

    /**
     * A static method invocation.
     */
    STATIC(Opcodes.INVOKESTATIC),

    /**
     * A specialized virtual method invocation.
     */
    SPECIAL(Opcodes.INVOKESPECIAL);
    private final int invocationOpcode;

    private MethodInvocation(int callOpcode) {
        this.invocationOpcode = callOpcode;
    }

    /**
     * Creates a method invocation with an implicitly determined invocation type.
     *
     * @param methodDescription The method to be invoked.
     * @return A stack manipulation with implicitly determined invocation type.
     */
    public static WithImplicitInvocationTargetType invoke(MethodDescription methodDescription) {
        if (methodDescription.isStatic()) { // Check this property first, private static methods must use INVOKESTATIC
            return STATIC.new Invocation(methodDescription);
        } else if (methodDescription.isPrivate() || methodDescription.isConstructor() || methodDescription.isDefaultMethod()) {
            return SPECIAL.new Invocation(methodDescription);
        } else if (methodDescription.getDeclaringType().isInterface()) { // Check this property last, default methods must be called by INVOKESPECIAL
            return INTERFACE.new Invocation(methodDescription);
        } else {
            return VIRTUAL.new Invocation(methodDescription);
        }
    }

    /**
     * Represents a method invocation where the invocation type (static, virtual, special, interface) is derived
     * from the given method's description.
     */
    public static interface WithImplicitInvocationTargetType extends StackManipulation {

        /**
         * Transforms this method invocation into a virtual (or interface) method invocation on the given type. If the
         * represented method cannot be dispatched on the given invocation target type using virtual invocation,
         * an exception is thrown.
         *
         * @param invocationTarget The type on which the method is to be invoked virtually on.
         * @return A stack manipulation representing this method invocation.
         */
        StackManipulation virtual(TypeDescription invocationTarget);

        /**
         * Transforms this method invocation into a special invocation on the given type. If the represented method
         * cannot be dispatched on the given invocation target type using special invocation, an exception is thrown.
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
                    methodDescription.getDescriptor(),
                    typeDescription.isInterface());
            return size;
        }

        @Override
        public StackManipulation virtual(TypeDescription invocationTarget) {
            if (methodDescription.isPrivate() || methodDescription.isConstructor() || methodDescription.isStatic()) {
                throw new IllegalStateException("Cannot invoke " + methodDescription + " virtually");
            }
            if (invocationTarget.isInterface()) {
                return INTERFACE.new Invocation(methodDescription, invocationTarget);
            } else {
                return VIRTUAL.new Invocation(methodDescription, invocationTarget);
            }
        }

        @Override
        public StackManipulation special(TypeDescription invocationTarget) {
            if (!methodDescription.isSpecializableFor(invocationTarget)) {
                throw new IllegalArgumentException("Cannot invoke " + invocationTarget + " on " + methodDescription);
            }
            return SPECIAL.new Invocation(methodDescription, invocationTarget);
        }

        @Override
        public MethodInvocation getImplicitInvocationType() {
            return MethodInvocation.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodDescription.equals(((Invocation) other).methodDescription)
                    && MethodInvocation.this.equals(((Invocation) other).getImplicitInvocationType())
                    && typeDescription.equals(((Invocation) other).typeDescription);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * MethodInvocation.this.hashCode() + typeDescription.hashCode()) + methodDescription.hashCode();
        }

        @Override
        public String toString() {
            return "MethodInvocation.Invocation{" +
                    "typeDescription=" + typeDescription +
                    ", methodDescription=" + methodDescription +
                    ", size=" + size +
                    '}';
        }
    }
}
