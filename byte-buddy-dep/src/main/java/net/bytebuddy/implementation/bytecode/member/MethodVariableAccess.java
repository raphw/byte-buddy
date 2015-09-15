package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
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
    INTEGER(Opcodes.ILOAD, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code long}.
     */
    LONG(Opcodes.LLOAD, StackSize.DOUBLE),

    /**
     * The accessor handler for a {@code float}.
     */
    FLOAT(Opcodes.FLOAD, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code double}.
     */
    DOUBLE(Opcodes.DLOAD, StackSize.DOUBLE),

    /**
     * The accessor handler for a reference type.
     */
    REFERENCE(Opcodes.ALOAD, StackSize.SINGLE);

    /**
     * The opcode for loading this variable.
     */
    private final int loadOpcode;

    /**
     * The size impact of this stack manipulation.
     */
    private final StackManipulation.Size size;

    /**
     * Creates a new method variable access for a given JVM type.
     *
     * @param loadOpcode The opcode for loading this variable.
     * @param stackSize  The size of the JVM type.
     */
    MethodVariableAccess(int loadOpcode, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.size = stackSize.toIncreasingSize();
    }

    /**
     * Locates the correct accessor for a variable of a given type.
     *
     * @param typeDescription The type of the variable to be loaded.
     * @return An accessor for the given type.
     */
    public static MethodVariableAccess forType(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            if (typeDescription.represents(long.class)) {
                return LONG;
            } else if (typeDescription.represents(double.class)) {
                return DOUBLE;
            } else if (typeDescription.represents(float.class)) {
                return FLOAT;
            } else if (typeDescription.represents(void.class)) {
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
     * Creates a stack assignment for a given index of the local variable array.
     * <p>&nbsp;</p>
     * The index has to be relative to the method's local variable array size.
     *
     * @param variableOffset The offset of the variable where {@code double} and {@code long} types
     *                       count two slots.
     * @return A stack manipulation representing the method retrieval.
     */
    public StackManipulation loadOffset(int variableOffset) {
        return new OffsetLoading(variableOffset);
    }

    @Override
    public String toString() {
        return "MethodVariableAccess." + name();
    }

    /**
     * A stack manipulation that loads all parameters of a given method onto the operand stack.
     */
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
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(methodDescription.getParameters().size() * 2);
            for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
                TypeDescription parameterType = parameterDescription.getType().asErasure();
                stackManipulations.add(forType(parameterType).loadOffset(parameterDescription.getOffset()));
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
                    : new Compound(MethodVariableAccess.REFERENCE.loadOffset(0), this);
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

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            MethodLoading that = (MethodLoading) other;
            return methodDescription.equals(that.methodDescription) && typeCastingHandler.equals(that.typeCastingHandler);
        }

        @Override
        public int hashCode() {
            int result = methodDescription.hashCode();
            result = 31 * result + typeCastingHandler.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodVariableAccess.MethodLoading{" +
                    "methodDescription=" + methodDescription +
                    ", typeCastingHandler=" + typeCastingHandler +
                    '}';
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

                @Override
                public String toString() {
                    return "MethodVariableAccess.MethodLoading.TypeCastingHandler.NoOp." + name();
                }
            }

            /**
             * A type casting handler that casts all parameters of a method to the parameter types of a compatible method
             * with covariant parameter types. This allows a convenient implementation of bridge methods.
             */
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

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForBridgeTarget that = (ForBridgeTarget) other;
                    return bridgeTarget.equals(that.bridgeTarget);
                }

                @Override
                public int hashCode() {
                    return bridgeTarget.hashCode();
                }

                @Override
                public String toString() {
                    return "MethodVariableAccess.MethodLoading.TypeCastingHandler.ForBridgeTarget{" +
                            "bridgeTarget=" + bridgeTarget +
                            '}';
                }
            }
        }
    }

    /**
     * A stack manipulation for loading a variable of a method's local variable array onto the operand stack.
     */
    protected class OffsetLoading implements StackManipulation {

        /**
         * The index of the local variable array from which the variable should be loaded.
         */
        private final int offset;

        /**
         * Creates a new argument loading stack manipulation.
         *
         * @param offset The index of the local variable array from which the variable should be loaded.
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

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && MethodVariableAccess.this == ((OffsetLoading) other).getMethodVariableAccess()
                    && offset == ((OffsetLoading) other).offset;
        }

        @Override
        public int hashCode() {
            return MethodVariableAccess.this.hashCode() + 31 * offset;
        }

        @Override
        public String toString() {
            return "MethodVariableAccess.OffsetLoading{" +
                    "methodVariableAccess=" + MethodVariableAccess.this +
                    " ,offset=" + offset + '}';
        }
    }
}

