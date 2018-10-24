/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
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
    VIRTUAL(Opcodes.INVOKEVIRTUAL, Opcodes.H_INVOKEVIRTUAL, Opcodes.INVOKEVIRTUAL, Opcodes.H_INVOKEVIRTUAL),

    /**
     * An interface-typed virtual method invocation.
     */
    INTERFACE(Opcodes.INVOKEINTERFACE, Opcodes.H_INVOKEINTERFACE, Opcodes.INVOKEINTERFACE, Opcodes.H_INVOKEINTERFACE),

    /**
     * A static method invocation.
     */
    STATIC(Opcodes.INVOKESTATIC, Opcodes.H_INVOKESTATIC, Opcodes.INVOKESTATIC, Opcodes.H_INVOKESTATIC),

    /**
     * A specialized pseudo-virtual method invocation for a non-constructor.
     */
    SPECIAL(Opcodes.INVOKESPECIAL, Opcodes.H_INVOKESPECIAL, Opcodes.INVOKESPECIAL, Opcodes.H_INVOKESPECIAL),

    /**
     * A specialized pseudo-virtual method invocation for a constructor.
     */
    SPECIAL_CONSTRUCTOR(Opcodes.INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL, Opcodes.INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL),

    /**
     * A private method call that is potentially virtual.
     */
    VIRTUAL_PRIVATE(Opcodes.INVOKEVIRTUAL, Opcodes.H_INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.H_INVOKESPECIAL),

    /**
     * A private method call that is potentially virtual on an interface type.
     */
    INTERFACE_PRIVATE(Opcodes.INVOKEINTERFACE, Opcodes.H_INVOKEINTERFACE, Opcodes.INVOKESPECIAL, Opcodes.H_INVOKESPECIAL);

    /**
     * The opcode for invoking a method.
     */
    private final int opcode;

    /**
     * The handle being used for a dynamic method invocation.
     */
    private final int handle;

    /**
     * The opcode for invoking a method before Java 11.
     */
    private final int legacyOpcode;

    /**
     * The handle being used for a dynamic method invocation before Java 11.
     */
    private final int legacyHandle;

    /**
     * Creates a new type of method invocation.
     *
     * @param opcode       The opcode for invoking a method.
     * @param handle       The handle being used for a dynamic method invocation.
     * @param legacyOpcode The opcode for invoking a method before Java 11.
     * @param legacyHandle The handle being used for a dynamic method invocation before Java 11.
     */
    MethodInvocation(int opcode, int handle, int legacyOpcode, int legacyHandle) {
        this.opcode = opcode;
        this.handle = handle;
        this.legacyOpcode = legacyOpcode;
        this.legacyHandle = legacyHandle;
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
            return (methodDescription.getDeclaringType().isInterface()
                    ? INTERFACE_PRIVATE
                    : VIRTUAL_PRIVATE).new Invocation(methodDescription);
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

        /**
         * {@inheritDoc}
         */
        public StackManipulation virtual(TypeDescription invocationTarget) {
            return Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation special(TypeDescription invocationTarget) {
            return Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation dynamic(String methodName,
                                         TypeDescription returnType,
                                         List<? extends TypeDescription> methodType,
                                         List<?> arguments) {
            return Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation onHandle(HandleType type) {
            return Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
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
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * {@inheritDoc}
         */
        public StackManipulation virtual(TypeDescription invocationTarget) {
            return new StackManipulation.Compound(invocation.virtual(invocationTarget), TypeCasting.to(targetType));
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation special(TypeDescription invocationTarget) {
            return new StackManipulation.Compound(invocation.special(invocationTarget), TypeCasting.to(targetType));
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation dynamic(String methodName, TypeDescription returnType, List<? extends TypeDescription> methodType, List<?> arguments) {
            return invocation.dynamic(methodName, returnType, methodType, arguments);
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation onHandle(HandleType type) {
            return new Compound(invocation.onHandle(type), TypeCasting.to(targetType));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return invocation.isValid();
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return new Compound(invocation, TypeCasting.to(targetType)).apply(methodVisitor, implementationContext);
        }
    }

    /**
     * An implementation of a method invoking stack manipulation.
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
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

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitMethodInsn(opcode == legacyOpcode || implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V11)
                            ? opcode
                            : legacyOpcode,
                    typeDescription.getInternalName(),
                    methodDescription.getInternalName(),
                    methodDescription.getDescriptor(),
                    typeDescription.isInterface());
            int parameterSize = methodDescription.getStackSize(), returnValueSize = methodDescription.getReturnType().getStackSize().getSize();
            return new Size(returnValueSize - parameterSize, Math.max(0, returnValueSize - parameterSize));
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation virtual(TypeDescription invocationTarget) {
            if (methodDescription.isConstructor() || methodDescription.isStatic()) {
                return Illegal.INSTANCE;
            } else if (methodDescription.isPrivate()) {
                return methodDescription.getDeclaringType().equals(invocationTarget)
                        ? this
                        : Illegal.INSTANCE;
            } else if (invocationTarget.isInterface()) {
                return methodDescription.getDeclaringType().represents(Object.class)
                        ? this
                        : INTERFACE.new Invocation(methodDescription, invocationTarget);
            } else {
                return VIRTUAL.new Invocation(methodDescription, invocationTarget);
            }
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation special(TypeDescription invocationTarget) {
            return methodDescription.isSpecializableFor(invocationTarget)
                    ? SPECIAL.new Invocation(methodDescription, invocationTarget)
                    : Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation dynamic(String methodName,
                                         TypeDescription returnType,
                                         List<? extends TypeDescription> methodType,
                                         List<?> arguments) {
            return methodDescription.isInvokeBootstrap()
                    ? new DynamicInvocation(methodName, returnType, new TypeList.Explicit(methodType), methodDescription.asDefined(), arguments)
                    : Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation onHandle(HandleType type) {
            return new HandleInvocation(methodDescription, type);
        }
    }

    /**
     * Performs a dynamic method invocation of the given method.
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
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
        private final List<? extends TypeDescription> parameterTypes;

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
                                 List<? extends TypeDescription> parameterTypes,
                                 MethodDescription.InDefinedShape bootstrapMethod,
                                 List<?> arguments) {
            this.methodName = methodName;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.bootstrapMethod = bootstrapMethod;
            this.arguments = arguments;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            StringBuilder stringBuilder = new StringBuilder("(");
            for (TypeDescription parameterType : parameterTypes) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            String methodDescriptor = stringBuilder.append(')').append(returnType.getDescriptor()).toString();
            methodVisitor.visitInvokeDynamicInsn(methodName,
                    methodDescriptor,
                    new Handle(handle == legacyHandle || implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V11)
                            ? handle
                            : legacyHandle,
                            bootstrapMethod.getDeclaringType().getInternalName(),
                            bootstrapMethod.getInternalName(),
                            bootstrapMethod.getDescriptor(),
                            bootstrapMethod.getDeclaringType().isInterface()),
                    arguments.toArray(new Object[arguments.size()]));
            int stackSize = returnType.getStackSize().getSize() - StackSize.of(parameterTypes);
            return new Size(stackSize, Math.max(stackSize, 0));
        }
    }

    /**
     * Performs a method invocation on a method handle with a polymorphic type signature.
     */
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
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
