package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
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
     * A specialized pseudo-virtual method invocation for a non-constructor.
     */
    SPECIAL(Opcodes.INVOKESPECIAL, Opcodes.H_INVOKESPECIAL),

    /**
     * A specialized pseudo-virtual method invocation for a constructor.
     */
    SPECIAL_CONSTRUCTOR(Opcodes.INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL);

    /**
     * The opcode for invoking a method.
     */
    private final int invocationOpcode;

    /**
     * The handle being used for a dynamic method invocation.
     */
    private final int handle;

    /**
     * Creates a new type of method invocation.
     *
     * @param callOpcode The opcode for invoking a method.
     * @param handle     The handle being used for a dynamic method invocation.
     */
    MethodInvocation(int callOpcode, int handle) {
        this.invocationOpcode = callOpcode;
        this.handle = handle;
    }

    /**
     * Creates a method invocation with an implicitly determined invocation type.
     *
     * @param methodDescription The method to be invoked.
     * @return A stack manipulation with implicitly determined invocation type.
     */
    public static WithImplicitInvocationTargetType invoke(MethodDescription.InDefinedShape methodDescription) {
        if (methodDescription.isTypeInitializer()) {
            return IllegalInvocation.INSTANCE;
        } else if (methodDescription.isStatic()) { // Check this property first, private static methods must use INVOKESTATIC
            return STATIC.new Invocation(methodDescription);
        } else if (methodDescription.isConstructor()) {
            return SPECIAL_CONSTRUCTOR.new Invocation(methodDescription); // Check this property second, constructors might be private
        } else if (methodDescription.isPrivate()) {
            return SPECIAL.new Invocation(methodDescription);
        } else if (methodDescription.getDeclaringType().asErasure().isInterface()) { // Check this property last, default methods must be called by INVOKESPECIAL
            return INTERFACE.new Invocation(methodDescription);
        } else {
            return VIRTUAL.new Invocation(methodDescription);
        }
    }

    /**
     * Creates a method invocation with an implicitly determined invocation type. If the method's return type derives from its declared shape, the value
     * is additionally casted to the value of the generically resolved method.
     *
     * @param methodDescription The method to be invoked.
     * @return A stack manipulation with implicitly determined invocation type.
     */
    public static WithImplicitInvocationTargetType invoke(MethodDescription methodDescription) {
        MethodDescription.InDefinedShape declaredMethod = methodDescription.asDefined();
        return declaredMethod.getReturnType().asErasure().equals(methodDescription.getReturnType().asErasure())
                ? invoke(declaredMethod)
                : OfGenericMethod.of(methodDescription, invoke(declaredMethod));
    }

    @Override
    public String toString() {
        return "MethodInvocation." + name();
    }

    /**
     * An illegal implicit method invocation.
     */
    protected enum IllegalInvocation implements WithImplicitInvocationTargetType {

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
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return Illegal.INSTANCE.apply(methodVisitor, implementationContext);
        }

        @Override
        public String toString() {
            return "MethodInvocation.IllegalInvocation." + name();
        }
    }

    /**
     * Represents a method invocation where the invocation type (static, virtual, special, interface) is derived
     * from the given method's description.
     */
    public interface WithImplicitInvocationTargetType extends StackManipulation {

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

        /**
         * Invokes the method as a bootstrap method to bind a call site with the given properties. Note that the
         * Java virtual machine currently only knows how to resolve bootstrap methods that link static methods
         * or a constructor.
         *
         * @param methodName The name of the method to be bound.
         * @param returnType The return type of the method to be bound.
         * @param methodType The parameter types of the method to be bound.
         * @param arguments  The arguments to be passed to the bootstrap method.
         * @return A stack manipulation that represents the dynamic method invocation.
         */
        StackManipulation dynamic(String methodName,
                                  TypeDescription returnType,
                                  List<? extends TypeDescription> methodType,
                                  List<?> arguments);
    }

    /**
     * A method invocation of a generically resolved method.
     */
    protected static class OfGenericMethod implements WithImplicitInvocationTargetType {

        /**
         * The generically resolved return type of the method.
         */
        private final TypeDescription targetType;

        /**
         * The invocation of the method in its defined shape.
         */
        private final WithImplicitInvocationTargetType invocation;

        /**
         * Creates a generic method invocation.
         *
         * @param targetType The generically resolved return type of the method.
         * @param invocation The invocation of the method in its defined shape.
         */
        protected OfGenericMethod(TypeDescription targetType, WithImplicitInvocationTargetType invocation) {
            this.targetType = targetType;
            this.invocation = invocation;
        }

        /**
         * Creates a generic access dispatcher for a given method.
         *
         * @param methodDescription The generically resolved return type of the method.
         * @param invocation        The invocation of the method in its defined shape.
         * @return A method access dispatcher for the given method.
         */
        protected static WithImplicitInvocationTargetType of(MethodDescription methodDescription, WithImplicitInvocationTargetType invocation) {
            return new OfGenericMethod(methodDescription.getReturnType().asErasure(), invocation);
        }

        @Override
        public StackManipulation virtual(TypeDescription invocationTarget) {
            return new StackManipulation.Compound(invocation.virtual(invocationTarget), TypeCasting.to(targetType));
        }

        @Override
        public StackManipulation special(TypeDescription invocationTarget) {
            return new StackManipulation.Compound(invocation.special(invocationTarget), TypeCasting.to(targetType));
        }

        @Override
        public StackManipulation dynamic(String methodName, TypeDescription returnType, List<? extends TypeDescription> methodType, List<?> arguments) {
            return invocation.dynamic(methodName, returnType, methodType, arguments);
        }

        @Override
        public boolean isValid() {
            return invocation.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return new Compound(invocation, TypeCasting.to(targetType)).apply(methodVisitor, implementationContext);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            OfGenericMethod that = (OfGenericMethod) other;
            return targetType.equals(that.targetType) && invocation.equals(that.invocation);
        }

        @Override
        public int hashCode() {
            int result = targetType.hashCode();
            result = 31 * result + invocation.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodInvocation.OfGenericMethod{" +
                    "targetType=" + targetType +
                    ", invocation=" + invocation +
                    '}';
        }
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
            this(methodDescription, methodDescription.getDeclaringType().asErasure());
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
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
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
            return methodDescription.isSpecializableFor(invocationTarget)
                    ? SPECIAL.new Invocation(methodDescription, invocationTarget)
                    : Illegal.INSTANCE;
        }

        @Override
        public StackManipulation dynamic(String methodName,
                                         TypeDescription returnType,
                                         List<? extends TypeDescription> methodType,
                                         List<?> arguments) {
            return methodDescription.isBootstrap()
                    ? new DynamicInvocation(methodName, returnType, new TypeList.Explicit(methodType), methodDescription, arguments)
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
                    && methodDescription.asToken().equals(that.methodDescription.asToken())
                    && typeDescription.equals(that.typeDescription);
        }

        @Override
        public int hashCode() {
            int result = typeDescription.hashCode();
            result = 31 * result + MethodInvocation.this.hashCode();
            result = 31 * result + methodDescription.asToken().hashCode();
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

    /**
     * Performs a dynamic method invocation of the given method.
     */
    protected class DynamicInvocation implements StackManipulation {

        /**
         * The internal name of the method that is to be bootstrapped.
         */
        private final String methodName;

        /**
         * The return type of the method to be bootstrapped.
         */
        private final TypeDescription returnType;

        /**
         * The parameter types of the method to be bootstrapped.
         */
        private final TypeList parameterTypes;

        /**
         * The bootstrap method.
         */
        private final MethodDescription bootstrapMethod;

        /**
         * The list of arguments to be handed over to the bootstrap method.
         */
        private final List<?> arguments;

        /**
         * Creates a new dynamic method invocation.
         *
         * @param methodName      The internal name of the method that is to be bootstrapped.
         * @param returnType      The return type of the method to be bootstrapped.
         * @param parameterTypes  The type of the parameters to be bootstrapped.
         * @param bootstrapMethod The bootstrap method.
         * @param arguments       The list of arguments to be handed over to the bootstrap method.
         */
        public DynamicInvocation(String methodName,
                                 TypeDescription returnType,
                                 TypeList parameterTypes,
                                 MethodDescription bootstrapMethod,
                                 List<?> arguments) {
            this.methodName = methodName;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.bootstrapMethod = bootstrapMethod;
            this.arguments = arguments;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : parameterTypes) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            String methodDescriptor = stringBuilder.append(')').append(returnType.getDescriptor()).toString();
            methodVisitor.visitInvokeDynamicInsn(methodName,
                    methodDescriptor,
                    new Handle(handle,
                            bootstrapMethod.getDeclaringType().asErasure().getInternalName(),
                            bootstrapMethod.getInternalName(),
                            bootstrapMethod.getDescriptor()),
                    arguments.toArray(new Object[arguments.size()]));
            int stackSize = returnType.getStackSize().getSize() - parameterTypes.getStackSize();
            return new Size(stackSize, Math.max(stackSize, 0));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            DynamicInvocation that = (DynamicInvocation) other;
            return MethodInvocation.this == that.getOuter()
                    && arguments.equals(that.arguments)
                    && bootstrapMethod.equals(that.bootstrapMethod)
                    && returnType.equals(that.returnType)
                    && parameterTypes.equals(that.parameterTypes)
                    && methodName.equals(that.methodName);
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private MethodInvocation getOuter() {
            return MethodInvocation.this;
        }

        @Override
        public int hashCode() {
            int result = methodName.hashCode();
            result = 31 * result + MethodInvocation.this.hashCode();
            result = 31 * result + returnType.hashCode();
            result = 31 * result + parameterTypes.hashCode();
            result = 31 * result + bootstrapMethod.hashCode();
            result = 31 * result + arguments.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodInvocation.DynamicInvocation{" +
                    "methodInvocation=" + MethodInvocation.this +
                    ", methodName='" + methodName + '\'' +
                    ", returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    ", bootstrapMethod=" + bootstrapMethod +
                    ", arguments=" + arguments +
                    '}';
        }
    }
}
