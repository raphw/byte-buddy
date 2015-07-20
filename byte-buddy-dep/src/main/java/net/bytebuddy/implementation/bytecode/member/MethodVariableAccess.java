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
import java.util.Iterator;
import java.util.List;

/**
 * A stack assignment that loads a method variable from a given index of the local variable array.
 */
public enum MethodVariableAccess {

    /**
     * The accessor handler for a JVM-integer.
     */
    INTEGER(Opcodes.ILOAD, 5, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code long}.
     */
    LONG(Opcodes.LLOAD, 8, StackSize.DOUBLE),

    /**
     * The accessor handler for a {@code float}.
     */
    FLOAT(Opcodes.FLOAD, 11, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code double}.
     */
    DOUBLE(Opcodes.DLOAD, 14, StackSize.DOUBLE),

    /**
     * The accessor handler for a reference type.
     */
    REFERENCE(Opcodes.ALOAD, 17, StackSize.SINGLE);

    /**
     * The opcode for loading this variable.
     */
    private final int loadOpcode;

    /**
     * The offset for any shortcut opcode that allows to load a variable from a low range index, such as
     * {@code ALOAD_0}, {@code ILOAD_0} etc.
     */
    private final int loadOpcodeShortcutOffset;

    /**
     * The size impact of this stack manipulation.
     */
    private final StackManipulation.Size size;

    /**
     * Creates a new method variable access for a given JVM type.
     *
     * @param loadOpcode               The opcode for loading this variable.
     * @param loadOpcodeShortcutOffset The offset for any shortcut opcode that allows to load a variable from a low
     *                                 range index, such as {@code ALOAD_0}, {@code ILOAD_0} etc.
     * @param stackSize                The size of the JVM type.
     */
    MethodVariableAccess(int loadOpcode, int loadOpcodeShortcutOffset, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.loadOpcodeShortcutOffset = loadOpcodeShortcutOffset;
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
        return new ArgumentLoadingStackManipulation(variableOffset);
    }

    @Override
    public String toString() {
        return "MethodVariableAccess." + name();
    }

    /**
     * A stack manipulation for loading a variable of a method's local variable array onto the operand stack.
     */
    protected class ArgumentLoadingStackManipulation implements StackManipulation {

        /**
         * The index of the local variable array from which the variable should be loaded.
         */
        private final int variableIndex;

        /**
         * Creates a new argument loading stack manipulation.
         *
         * @param variableIndex The index of the local variable array from which the variable should be loaded.
         */
        protected ArgumentLoadingStackManipulation(int variableIndex) {
            this.variableIndex = variableIndex;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            switch (variableIndex) {
                case 0:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset);
                    break;
                case 1:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset + 1);
                    break;
                case 2:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset + 2);
                    break;
                case 3:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset + 3);
                    break;
                default:
                    methodVisitor.visitVarInsn(loadOpcode, variableIndex);
                    break;
            }
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
                    && MethodVariableAccess.this == ((ArgumentLoadingStackManipulation) other).getMethodVariableAccess()
                    && variableIndex == ((ArgumentLoadingStackManipulation) other).variableIndex;
        }

        @Override
        public int hashCode() {
            return MethodVariableAccess.this.hashCode() + 31 * variableIndex;
        }

        @Override
        public String toString() {
            return "MethodVariableAccess.ArgumentLoadingStackManipulation{" +
                    "methodVariableAccess=" + MethodVariableAccess.this +
                    " ,variableIndex=" + variableIndex + '}';
        }
    }

    public static class MethodLoading implements StackManipulation {

        private final MethodDescription methodDescription;

        private final TypeCastingHandler typeCastingHandler;

        public MethodLoading(MethodDescription methodDescription, TypeCastingHandler typeCastingHandler) {
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
                TypeDescription parameterType = parameterDescription.getType().asRawType();
                stackManipulations.add(forType(parameterType).loadOffset(parameterDescription.getOffset()));
                stackManipulations.add(typeCastingHandler.ofIndex(parameterType, parameterDescription.getIndex()));
            }
            return new Compound(stackManipulations).apply(methodVisitor, implementationContext);
        }

        public StackManipulation prependThisReference() {
            return methodDescription.isStatic()
                    ? this
                    : new Compound(MethodVariableAccess.REFERENCE.loadOffset(0), this);
        }

        public StackManipulation asBridgeOf(MethodDescription methodDescription) {
            return new MethodLoading(methodDescription, new TypeCastingHandler.ForBridgeTarget(methodDescription));
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

        protected interface TypeCastingHandler {

            StackManipulation ofIndex(TypeDescription parameterType, int index);

            enum NoOp implements TypeCastingHandler {

                INSTANCE;

                @Override
                public StackManipulation ofIndex(TypeDescription parameterType, int index) {
                    return LegalTrivial.INSTANCE;
                }

                @Override
                public String toString() {
                    return "MethodVariableAccess.MethodLoading.TypeCastingHandler.NoOp." + name();
                }
            }

            class ForBridgeTarget implements TypeCastingHandler {

                private final MethodDescription bridgeTarget;

                public ForBridgeTarget(MethodDescription bridgeTarget) {
                    this.bridgeTarget = bridgeTarget;
                }

                @Override
                public StackManipulation ofIndex(TypeDescription parameterType, int index) {
                    TypeDescription targetType = bridgeTarget.getParameters().get(index).getType().asRawType();
                    return parameterType.equals(targetType)
                            ? LegalTrivial.INSTANCE
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
}

