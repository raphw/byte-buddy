package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack assignment that loads a method variable from a given index of the local variable array.
 */
public enum MethodVariableAccess {

    INTEGER(Opcodes.ILOAD, 5, StackSize.SINGLE),
    LONG(Opcodes.LLOAD, 8, StackSize.DOUBLE),
    FLOAT(Opcodes.FLOAD, 11, StackSize.SINGLE),
    DOUBLE(Opcodes.DLOAD, 14, StackSize.DOUBLE),
    REFERENCE(Opcodes.ALOAD, 17, StackSize.SINGLE);
    private final int loadOpcode;
    private final int loadOpcodeShortcutIndex;
    private final StackManipulation.Size size;

    private MethodVariableAccess(int loadOpcode, int loadOpcodeShortcutIndex, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.loadOpcodeShortcutIndex = loadOpcodeShortcutIndex;
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
     * Loads all method parameters for a given method, including a reference to the object instance if the method is
     * not static.
     *
     * @param methodDescription The method for which all parameters and an instance reference, if any, is to be loaded.
     * @return A stack manipulation representing the loading of all parameters including a reference to the instance if any.
     */
    public static StackManipulation loadThisAndArguments(MethodDescription methodDescription) {
        int stackValues = (methodDescription.isStatic() ? 0 : 1) + methodDescription.getParameterTypes().size();
        StackManipulation[] stackManipulation = new StackManipulation[stackValues];
        int parameterIndex = 0, stackIndex;
        if (!methodDescription.isStatic()) {
            stackManipulation[parameterIndex++] = forType(methodDescription.getDeclaringType()).loadFromIndex(0);
            stackIndex = methodDescription.getDeclaringType().getStackSize().getSize();
        } else {
            stackIndex = 0;
        }
        for (TypeDescription parameterType : methodDescription.getParameterTypes()) {
            stackManipulation[parameterIndex++] = forType(parameterType).loadFromIndex(stackIndex);
            stackIndex += parameterType.getStackSize().getSize();
        }
        return new StackManipulation.Compound(stackManipulation);
    }

    public static StackManipulation loadArguments(MethodDescription methodDescription) {
        int stackValues = methodDescription.getParameterTypes().size();
        StackManipulation[] stackManipulation = new StackManipulation[stackValues];
        int parameterIndex = 0, stackIndex;
        if (!methodDescription.isStatic()) {
            stackIndex = methodDescription.getDeclaringType().getStackSize().getSize();
        } else {
            stackIndex = 0;
        }
        for (TypeDescription parameterType : methodDescription.getParameterTypes()) {
            stackManipulation[parameterIndex++] = forType(parameterType).loadFromIndex(stackIndex);
            stackIndex += parameterType.getStackSize().getSize();
        }
        return new StackManipulation.Compound(stackManipulation);
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
    public StackManipulation loadFromIndex(int variableOffset) {
        return new ArgumentLoadingStackManipulation(variableOffset);
    }

    private class ArgumentLoadingStackManipulation implements StackManipulation {

        private final int variableIndex;

        private ArgumentLoadingStackManipulation(int variableIndex) {
            this.variableIndex = variableIndex;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            switch (variableIndex) {
                case 0:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex);
                    break;
                case 1:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex + 1);
                    break;
                case 2:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex + 2);
                    break;
                case 3:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutIndex + 3);
                    break;
                default:
                    methodVisitor.visitVarInsn(loadOpcode, variableIndex);
                    break;
            }
            return size;
        }
    }
}

