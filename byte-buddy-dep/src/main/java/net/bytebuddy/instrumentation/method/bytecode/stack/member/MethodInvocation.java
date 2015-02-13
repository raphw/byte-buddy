package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * A builder for a method invocation.
 */
public enum MethodInvocation {

    /**
     * A virtual method invocation.
     */
    VIRTUAL(Opcodes.INVOKEVIRTUAL, Opcodes.H_INVOKEVIRTUAL),

    /**
     * An interface-typed virtual method invocation.
     */
    INTERFACE(Opcodes.INVOKEINTERFACE, Opcodes.H_INVOKEINTERFACE),

    /**
     * A static method invocation.
     */
    STATIC(Opcodes.INVOKESTATIC, Opcodes.H_INVOKESTATIC),

    /**
     * A specialized virtual method invocation.
     */
    SPECIAL(Opcodes.INVOKESPECIAL, Opcodes.H_INVOKESPECIAL),

    SPECIAL_CONSTRUCTOR(Opcodes.INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL);

    /**
     * The opcode for invoking a method.
     */
    private final int invocationOpcode;

    private final int handle;

    /**
     * Creates a new type of method invocation.
     *
     * @param callOpcode The opcode for invoking a method.
     */
    private MethodInvocation(int callOpcode, int handle) {
        this.invocationOpcode = callOpcode;
        this.handle = handle;
    }

    /**
     * Creates a method invocation with an implicitly determined invocation type.
     *
     * @param methodDescription The method to be invoked.
     * @return A stack manipulation with implicitly determined invocation type.
     */
    public static WithImplicitInvocationTargetType invoke(MethodDescription methodDescription) {
        if (methodDescription.isTypeInitializer()) {
            return IllegalInvocation.INSTANCE;
        } else if (methodDescription.isStatic()) { // Check this property first, private static methods must use INVOKESTATIC
            return STATIC.new Invocation(methodDescription);
        } else if (methodDescription.isConstructor()) {
            return SPECIAL_CONSTRUCTOR.new Invocation(methodDescription); // Check this property second, constructors might be private
        } else if (methodDescription.isPrivate() || methodDescription.isDefaultMethod()) {
            return SPECIAL.new Invocation(methodDescription);
        } else if (methodDescription.getDeclaringType().isInterface()) { // Check this property last, default methods must be called by INVOKESPECIAL
            return INTERFACE.new Invocation(methodDescription);
        } else {
            return VIRTUAL.new Invocation(methodDescription);
        }
    }

    /**
     * An illegal implicit method invocation.
     */
    protected static enum IllegalInvocation implements WithImplicitInvocationTargetType {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public StackManipulation virtual(TypeDescription invocationTarget) {
            return Illegal.INSTANCE;
        }

        @Override
        public StackManipulation special(TypeDescription invocationTarget) {
            return Illegal.INSTANCE;
        }

        @Override
        public StackManipulation dynamic(String methodName,
                                         TypeDescription returnType,
                                         List<? extends TypeDescription> methodType,
                                         List<?> arguments) {
            return Illegal.INSTANCE;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            return Illegal.INSTANCE.apply(methodVisitor, instrumentationContext);
        }
    }

    /**
     * Represents a method invocation where the invocation type (static, virtual, special, interface) is derived
     * from the given method's description.
     */
    public static interface WithImplicitInvocationTargetType extends StackManipulation {

        /**
         * Transforms this method invocation into a virtual (or interface) method invocation on the given type.
         *
         * @param invocationTarget The type on which the method is to be invoked virtually on.
         * @return A stack manipulation representing this method invocation.
         */
        StackManipulation virtual(TypeDescription invocationTarget);

        /**
         * Transforms this method invocation into a special invocation on the given type.
         *
         * @param invocationTarget The type on which the method is to be invoked specially on.
         * @return A stack manipulation representing this method invocation.
         */
        StackManipulation special(TypeDescription invocationTarget);

        StackManipulation dynamic(String methodName,
                                  TypeDescription returnType,
                                  List<? extends TypeDescription> methodType,
                                  List<?> arguments);
    }

    /**
     * An implementation of a method invoking stack manipulation.
     */
    protected class Invocation implements WithImplicitInvocationTargetType {

        /**
         * The method to be invoked.
         */
        private final TypeDescription typeDescription;

        /**
         * The type on which this method is to be invoked.
         */
        private final MethodDescription methodDescription;

        /**
         * Creates an invocation of a given method on its declaring type as an invocation target.
         *
         * @param methodDescription The method to be invoked.
         */
        protected Invocation(MethodDescription methodDescription) {
            this(methodDescription, methodDescription.getDeclaringType());
        }

        /**
         * Creates an invocation of a given method on a given invocation target type.
         *
         * @param methodDescription The method to be invoked.
         * @param typeDescription   The type on which this method is to be invoked.
         */
        protected Invocation(MethodDescription methodDescription, TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
            this.methodDescription = methodDescription;
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
            int parameterSize = methodDescription.getStackSize();
            int returnValueSize = methodDescription.getReturnType().getStackSize().getSize();
            return new Size(returnValueSize - parameterSize, Math.max(0, returnValueSize - parameterSize));
        }

        @Override
        public StackManipulation virtual(TypeDescription invocationTarget) {
            if (methodDescription.isPrivate() || methodDescription.isConstructor() || methodDescription.isStatic()) {
                return Illegal.INSTANCE;
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
                return Illegal.INSTANCE;
            }
            return SPECIAL.new Invocation(methodDescription, invocationTarget);
        }

        @Override
        public StackManipulation dynamic(String methodName,
                                         TypeDescription returnType,
                                         List<? extends TypeDescription> methodType,
                                         List<?> arguments) {
            return methodDescription.isBootstrap()
                    ? new DynamicInvocation(methodName, returnType, methodType, methodDescription, arguments)
                    : Illegal.INSTANCE;
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private MethodInvocation getOuterInstance() {
            return MethodInvocation.this;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Invocation that = (Invocation) other;
            return MethodInvocation.this.equals(((Invocation) other).getOuterInstance())
                    && methodDescription.getInternalName().equals(that.methodDescription.getInternalName())
                    && methodDescription.getReturnType().equals(((Invocation) other).methodDescription.getReturnType())
                    && methodDescription.getParameterTypes().equals(((Invocation) other).methodDescription.getParameterTypes())
                    && typeDescription.equals(that.typeDescription);
        }

        @Override
        public int hashCode() {
            int result = typeDescription.hashCode();
            result = 31 * result + MethodInvocation.this.hashCode();
            result = 31 * result + methodDescription.getInternalName().hashCode();
            result = 31 * result + methodDescription.getParameterTypes().hashCode();
            result = 31 * result + methodDescription.getReturnType().hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodInvocation.Invocation{" +
                    "typeDescription=" + typeDescription +
                    ", methodDescription=" + methodDescription +
                    '}';
        }
    }

    protected class DynamicInvocation implements StackManipulation {

        private final String methodName;

        private final String methodDescriptor;

        private final MethodDescription bootstrapMethod;

        private final List<?> arguments;

        public DynamicInvocation(String methodName,
                                 TypeDescription returnType,
                                 List<? extends TypeDescription> parameterTypes,
                                 MethodDescription bootstrapMethod,
                                 List<?> arguments) {
            this.methodName = methodName;
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : parameterTypes) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            methodDescriptor = stringBuilder.append(')').append(returnType.getDescriptor()).toString();
            this.bootstrapMethod = bootstrapMethod;
            this.arguments = arguments;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitInvokeDynamicInsn(methodName,
                    methodDescriptor,
                    new Handle(handle,
                            bootstrapMethod.getDeclaringType().getInternalName(),
                            bootstrapMethod.getInternalName(),
                            bootstrapMethod.getDescriptor()),
                    arguments.toArray(new Object[arguments.size()]));
            return (bootstrapMethod.isConstructor()
                    ? StackSize.SINGLE
                    : bootstrapMethod.getReturnType().getStackSize()).toIncreasingSize();
        }
    }
}
