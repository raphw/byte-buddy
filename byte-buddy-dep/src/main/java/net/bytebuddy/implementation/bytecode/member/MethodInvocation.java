package net.bytebuddy.implementation.bytecode.member;

import com.google.auto.value.AutoValue;
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
        } else if (methodDescription.getDeclaringType().isInterface()) { // Check this property last, default methods must be called by INVOKESPECIAL
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
        public StackManipulation onHandle(HandleType type) {
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

        /**
         * Invokes the method via a {@code MethodHandle}.
         *
         * @param type The type of invocation.
         * @return A stack manipulation that represents a method call of the specified method via a method handle.
         */
        StackManipulation onHandle(HandleType type);
    }

    /**
     * A method invocation of a generically resolved method.
     */
    @AutoValue
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
        public StackManipulation onHandle(HandleType type) {
            return new Compound(invocation.onHandle(type), TypeCasting.to(targetType));
        }

        @Override
        public boolean isValid() {
            return invocation.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return new Compound(invocation, TypeCasting.to(targetType)).apply(methodVisitor, implementationContext);
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
        private final MethodDescription.InDefinedShape methodDescription;

        /**
         * Creates an invocation of a given method on its declaring type as an invocation target.
         *
         * @param methodDescription The method to be invoked.
         */
        protected Invocation(MethodDescription.InDefinedShape methodDescription) {
            this(methodDescription, methodDescription.getDeclaringType());
        }

        /**
         * Creates an invocation of a given method on a given invocation target type.
         *
         * @param methodDescription The method to be invoked.
         * @param typeDescription   The type on which this method is to be invoked.
         */
        protected Invocation(MethodDescription.InDefinedShape methodDescription, TypeDescription typeDescription) {
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
            int parameterSize = methodDescription.getStackSize(), returnValueSize = methodDescription.getReturnType().getStackSize().getSize();
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
                    ? new DynamicInvocation(methodName, returnType, new TypeList.Explicit(methodType), methodDescription.asDefined(), arguments)
                    : Illegal.INSTANCE;
        }

        @Override
        public StackManipulation onHandle(HandleType type) {
            return new HandleInvocation(methodDescription, type);
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private MethodInvocation getOuterInstance() {
            return MethodInvocation.this;
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Invocation that = (Invocation) other;
            return MethodInvocation.this.equals(((Invocation) other).getOuterInstance())
                    && methodDescription.asSignatureToken().equals(that.methodDescription.asSignatureToken())
                    && typeDescription.equals(that.typeDescription);
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public int hashCode() {
            int result = typeDescription.hashCode();
            result = 31 * result + MethodInvocation.this.hashCode();
            result = 31 * result + methodDescription.asSignatureToken().hashCode();
            return result;
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
        private final MethodDescription.InDefinedShape bootstrapMethod;

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
                                 MethodDescription.InDefinedShape bootstrapMethod,
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
                            bootstrapMethod.getDeclaringType().getInternalName(),
                            bootstrapMethod.getInternalName(),
                            bootstrapMethod.getDescriptor(),
                            bootstrapMethod.getDeclaringType().isInterface()),
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
    }

    /**
     * Performs a method invocation on a method handle with a polymorphic type signature.
     */
    @AutoValue
    protected static class HandleInvocation implements StackManipulation {

        /**
         * The internal name of the method handle type.
         */
        private static final String METHOD_HANDLE = "java/lang/invoke/MethodHandle";

        /**
         * The invoked method.
         */
        private final MethodDescription.InDefinedShape methodDescription;

        /**
         * The type of method handle invocation.
         */
        private final HandleType type;

        /**
         * Creates a new method handle invocation.
         *
         * @param methodDescription The invoked method.
         * @param type              The type of method handle invocation.
         */
        protected HandleInvocation(MethodDescription.InDefinedShape methodDescription, HandleType type) {
            this.methodDescription = methodDescription;
            this.type = type;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    METHOD_HANDLE,
                    type.getMethodName(),
                    methodDescription.isStatic() || methodDescription.isConstructor()
                            ? methodDescription.getDescriptor()
                            : "(" + methodDescription.getDeclaringType().getDescriptor() + methodDescription.getDescriptor().substring(1),
                    false);
            int parameterSize = 1 + methodDescription.getStackSize(), returnValueSize = methodDescription.getReturnType().getStackSize().getSize();
            return new Size(returnValueSize - parameterSize, Math.max(0, returnValueSize - parameterSize));
        }
    }

    /**
     * The type of method handle invocation.
     */
    public enum HandleType {

        /**
         * An exact invocation without type adjustments.
         */
        EXACT("invokeExact"),

        /**
         * A regular invocation with standard type adjustments.
         */
        REGULAR("invoke");

        /**
         * The name of the invoked method.
         */
        private final String methodName;

        /**
         * Creates a new handle type.
         *
         * @param methodName The name of the invoked method.
         */
        HandleType(String methodName) {
            this.methodName = methodName;
        }

        /**
         * Returns the name of the represented method.
         *
         * @return The name of the invoked method.
         */
        protected String getMethodName() {
            return methodName;
        }
    }
}
