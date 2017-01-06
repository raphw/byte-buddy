package net.bytebuddy.implementation.bytecode.member;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * A stack assignment that loads a method variable from a given index of the local variable array.
 */
public enum MethodVariableAccess {

    /**
     * The accessor handler for a JVM-integer.
     */
    INTEGER(Opcodes.ILOAD, Opcodes.ISTORE, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code long}.
     */
    LONG(Opcodes.LLOAD, Opcodes.LSTORE, StackSize.DOUBLE),

    /**
     * The accessor handler for a {@code float}.
     */
    FLOAT(Opcodes.FLOAD, Opcodes.FSTORE, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code double}.
     */
    DOUBLE(Opcodes.DLOAD, Opcodes.DSTORE, StackSize.DOUBLE),

    /**
     * The accessor handler for a reference type.
     */
    REFERENCE(Opcodes.ALOAD, Opcodes.ASTORE, StackSize.SINGLE);

    /**
     * The opcode for loading this variable type.
     */
    private final int loadOpcode;

    /**
     * The opcode for storing a local variable type.
     */
    private final int storeOpcode;

    /**
     * The size impact of this stack manipulation.
     */
    private final StackManipulation.Size size;

    /**
     * Creates a new method variable access for a given JVM type.
     *
     * @param loadOpcode  The opcode for loading this variable type.
     * @param storeOpcode The opcode for storing this variable type.
     * @param stackSize   The size of the JVM type.
     */
    MethodVariableAccess(int loadOpcode, int storeOpcode, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.size = stackSize.toIncreasingSize();
        this.storeOpcode = storeOpcode;
    }

    /**
     * Locates the correct accessor for a variable of a given type.
     *
     * @param typeDefinition The type of the variable to be loaded.
     * @return An accessor for the given type.
     */
    public static MethodVariableAccess of(TypeDefinition typeDefinition) {
        if (typeDefinition.isPrimitive()) {
            if (typeDefinition.represents(long.class)) {
                return LONG;
            } else if (typeDefinition.represents(double.class)) {
                return DOUBLE;
            } else if (typeDefinition.represents(float.class)) {
                return FLOAT;
            } else if (typeDefinition.represents(void.class)) {
                throw new IllegalArgumentException("Variable type cannot be void");
            } else {
                return INTEGER;
            }
        } else {
            return REFERENCE;
        }
    }

    /**
     * Loads all arguments of the provided method onto the operand stack.
     *
     * @param methodDescription The method for which all parameters are to be loaded onto the operand stack.
     * @return A stack manipulation that loads all parameters of the provided method onto the operand stack.
     */
    public static MethodLoading allArgumentsOf(MethodDescription methodDescription) {
        return new MethodLoading(methodDescription, MethodLoading.TypeCastingHandler.NoOp.INSTANCE);
    }

    /**
     * Loads a reference to the {@code this} reference what is only meaningful for a non-static method.
     *
     * @return A stack manipulation loading the {@code this} reference.
     */
    public static StackManipulation loadThis() {
        return MethodVariableAccess.REFERENCE.loadFrom(0);
    }

    /**
     * Creates a stack assignment for a reading given offset of the local variable array.
     *
     * @param offset The offset of the variable where {@code double} and {@code long} types count two slots.
     * @return A stack manipulation representing the variable read.
     */
    public StackManipulation loadFrom(int offset) {
        return new OffsetLoading(offset);
    }

    /**
     * Creates a stack assignment for writing to a given offset of the local variable array.
     *
     * @param offset The offset of the variable where {@code double} and {@code long} types count two slots.
     * @return A stack manipulation representing the variable write.
     */
    public StackManipulation storeAt(int offset) {
        return new OffsetWriting(offset);
    }

    /**
     * Creates a stack assignment for incrementing the given offset of the local variable array.
     *
     * @param offset The offset of the variable where {@code double} and {@code long} types count two slots.
     * @param value  The incremented value.
     * @return A stack manipulation representing the variable write.
     */
    public StackManipulation increment(int offset, int value) {
        if (this != INTEGER) {
            throw new IllegalStateException("Cannot increment type: " + this);
        }
        return new OffsetIncrementing(offset, value);
    }

    /**
     * Loads a parameter's value onto the operand stack.
     *
     * @param parameterDescription The parameter which to load onto the operand stack.
     * @return A stack manipulation loading a parameter onto the operand stack.
     */
    public static StackManipulation load(ParameterDescription parameterDescription) {
        return of(parameterDescription.getType()).loadFrom(parameterDescription.getOffset());
    }

    /**
     * Stores the top operand stack value at the supplied parameter.
     *
     * @param parameterDescription The parameter which to store a value for.
     * @return A stack manipulation storing the top operand stack value at this parameter.
     */
    public static StackManipulation store(ParameterDescription parameterDescription) {
        return of(parameterDescription.getType()).storeAt(parameterDescription.getOffset());
    }

    /**
     * Increments the value of the supplied parameter.
     *
     * @param parameterDescription The parameter which to increment.
     * @param value                The value to increment with.
     * @return A stack manipulation incrementing the supplied parameter.
     */
    public static StackManipulation increment(ParameterDescription parameterDescription, int value) {
        return of(parameterDescription.getType()).increment(parameterDescription.getOffset(), value);
    }

    /**
     * A stack manipulation that loads all parameters of a given method onto the operand stack.
     */
    @EqualsAndHashCode
    public static class MethodLoading implements StackManipulation {

        /**
         * The method for which all parameters are loaded onto the operand stack.
         */
        private final MethodDescription methodDescription;

        /**
         * A type casting handler which is capable of transforming all method parameters.
         */
        private final TypeCastingHandler typeCastingHandler;

        /**
         * Creates a new method loading stack manipulation.
         *
         * @param methodDescription  The method for which all parameters are loaded onto the operand stack.
         * @param typeCastingHandler A type casting handler which is capable of transforming all method parameters.
         */
        protected MethodLoading(MethodDescription methodDescription, TypeCastingHandler typeCastingHandler) {
            this.methodDescription = methodDescription;
            this.typeCastingHandler = typeCastingHandler;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>();
            for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
                TypeDescription parameterType = parameterDescription.getType().asErasure();
                stackManipulations.add(of(parameterType).loadFrom(parameterDescription.getOffset()));
                stackManipulations.add(typeCastingHandler.ofIndex(parameterType, parameterDescription.getIndex()));
            }
            return new Compound(stackManipulations).apply(methodVisitor, implementationContext);
        }

        /**
         * Prepends a reference to the {@code this} instance to the loaded parameters if the represented method is non-static.
         *
         * @return A stack manipulation that loads all method parameters onto the operand stack while additionally loading a reference
         * to {@code this} if the represented is non-static. Any potential parameter transformation is preserved.
         */
        public StackManipulation prependThisReference() {
            return methodDescription.isStatic()
                    ? this
                    : new Compound(MethodVariableAccess.loadThis(), this);
        }

        /**
         * Applies a transformation to all loaded arguments of the method being loaded to be casted to the corresponding parameter of
         * the provided method. This way, the parameters can be used for invoking a bridge target method.
         *
         * @param bridgeTarget The method that is the target of the bridge method for which the parameters are being loaded.
         * @return A stack manipulation that loads all parameters casted to the types of the supplied bridge target.
         */
        public MethodLoading asBridgeOf(MethodDescription bridgeTarget) {
            return new MethodLoading(methodDescription, new TypeCastingHandler.ForBridgeTarget(bridgeTarget));
        }

        /**
         * A type casting handler allows a type transformation of all arguments of a method after loading them onto the operand stack.
         */
        protected interface TypeCastingHandler {

            /**
             * Yields a stack transformation to transform the given argument of the method for which the arguments are loaded onto the operand stack.
             *
             * @param parameterType The parameter type that is to be transformed.
             * @param index         The index of the transformed parameter.
             * @return A transformation to apply after loading the parameter onto the operand stack.
             */
            StackManipulation ofIndex(TypeDescription parameterType, int index);

            /**
             * A non-operative type casting handler.
             */
            enum NoOp implements TypeCastingHandler {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public StackManipulation ofIndex(TypeDescription parameterType, int index) {
                    return Trivial.INSTANCE;
                }
            }

            /**
             * A type casting handler that casts all parameters of a method to the parameter types of a compatible method
             * with covariant parameter types. This allows a convenient implementation of bridge methods.
             */
            @EqualsAndHashCode
            class ForBridgeTarget implements TypeCastingHandler {

                /**
                 * The target of the method bridge.
                 */
                private final MethodDescription bridgeTarget;

                /**
                 * Creates a new type casting handler for a bridge target.
                 *
                 * @param bridgeTarget The target of the method bridge.
                 */
                public ForBridgeTarget(MethodDescription bridgeTarget) {
                    this.bridgeTarget = bridgeTarget;
                }

                @Override
                public StackManipulation ofIndex(TypeDescription parameterType, int index) {
                    TypeDescription targetType = bridgeTarget.getParameters().get(index).getType().asErasure();
                    return parameterType.equals(targetType)
                            ? Trivial.INSTANCE
                            : TypeCasting.to(targetType);
                }
            }
        }
    }

    /**
     * A stack manipulation for loading a variable of a method's local variable array onto the operand stack.
     */
    protected class OffsetLoading implements StackManipulation {

        /**
         * The offset of the local variable array from which the variable should be loaded.
         */
        private final int offset;

        /**
         * Creates a new argument loading stack manipulation.
         *
         * @param offset The offset of the local variable array from which the variable should be loaded.
         */
        protected OffsetLoading(int offset) {
            this.offset = offset;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitVarInsn(loadOpcode, offset);
            return size;
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private MethodVariableAccess getMethodVariableAccess() {
            return MethodVariableAccess.this;
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && MethodVariableAccess.this == ((OffsetLoading) other).getMethodVariableAccess()
                    && offset == ((OffsetLoading) other).offset;
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public int hashCode() {
            return MethodVariableAccess.this.hashCode() + 31 * offset;
        }
    }

    /**
     * A stack manipulation for storing a variable into a method's local variable array.
     */
    protected class OffsetWriting implements StackManipulation {

        /**
         * The offset of the local variable array to which the value should be written.
         */
        private final int offset;

        /**
         * Creates a new argument writing stack manipulation.
         *
         * @param offset The offset of the local variable array to which the value should be written.
         */
        protected OffsetWriting(int offset) {
            this.offset = offset;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitVarInsn(storeOpcode, offset);
            return size;
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private MethodVariableAccess getMethodVariableAccess() {
            return MethodVariableAccess.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && MethodVariableAccess.this == ((OffsetWriting) other).getMethodVariableAccess()
                    && offset == ((OffsetWriting) other).offset;
        }

        @Override
        public int hashCode() {
            return MethodVariableAccess.this.hashCode() + 31 * offset;
        }
    }

    /**
     * A stack manipulation that increments an integer variable.
     */
    @EqualsAndHashCode
    protected static class OffsetIncrementing implements StackManipulation {

        /**
         * The index of the local variable array from which the variable should be loaded.
         */
        private final int offset;

        /**
         * The value to increment.
         */
        private final int value;

        /**
         * Creates a new argument loading stack manipulation.
         *
         * @param offset The index of the local variable array from which the variable should be loaded.
         * @param value  The value to increment.
         */
        protected OffsetIncrementing(int offset, int value) {
            this.offset = offset;
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitIincInsn(offset, value);
            return new Size(0, 0);
        }
    }
}

