package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation returning a value of a given type.
 */
public enum MethodReturn implements StackManipulation {

    /**
     * The method return handler for returning a JVM-integer.
     */
    INTEGER(Opcodes.IRETURN, StackSize.SINGLE),

    /**
     * The method return handler for returning a {@code double}.
     */
    DOUBLE(Opcodes.DRETURN, StackSize.DOUBLE),

    /**
     * The method return handler for returning a {@code float}.
     */
    FLOAT(Opcodes.FRETURN, StackSize.SINGLE),

    /**
     * The method return handler for returning a {@code long}.
     */
    LONG(Opcodes.LRETURN, StackSize.DOUBLE),

    /**
     * The method return handler for returning {@code void}.
     */
    VOID(Opcodes.RETURN, StackSize.ZERO),

    /**
     * The method return handler for returning a reference type.
     */
    REFERENCE(Opcodes.ARETURN, StackSize.SINGLE);

    /**
     * The opcode of this operation.
     */
    private final int returnOpcode;

    /**
     * The operand stack size change that is implied by this operation.
     */
    private final Size size;

    /**
     * Creates a new method return manipulation.
     *
     * @param returnOpcode The opcode of this operation.
     * @param stackSize    The operand stack size change that is implied by this operation.
     */
    MethodReturn(int returnOpcode, StackSize stackSize) {
        this.returnOpcode = returnOpcode;
        size = stackSize.toDecreasingSize();
    }

    /**
     * Returns a method return corresponding to a given type.
     *
     * @param typeDefinition The type to be returned.
     * @return The stack manipulation representing the method return.
     */
    public static StackManipulation of(TypeDefinition typeDefinition) {
        if (typeDefinition.isPrimitive()) {
            if (typeDefinition.represents(long.class)) {
                return LONG;
            } else if (typeDefinition.represents(double.class)) {
                return DOUBLE;
            } else if (typeDefinition.represents(float.class)) {
                return FLOAT;
            } else if (typeDefinition.represents(void.class)) {
                return VOID;
            } else {
                return INTEGER;
            }
        } else {
            return REFERENCE;
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(returnOpcode);
        return size;
    }
}
