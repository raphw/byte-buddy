package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation returning a value of a given type. 返回给定类型值的堆栈操作
 */
public enum MethodReturn implements StackManipulation {

    /**
     * The method return handler for returning a JVM-integer. 返回JVM整数的方法返回处理程序
     */
    INTEGER(Opcodes.IRETURN, StackSize.SINGLE),

    /**
     * The method return handler for returning a {@code double}. 返回{@code double}的方法返回处理程序
     */
    DOUBLE(Opcodes.DRETURN, StackSize.DOUBLE),

    /**
     * The method return handler for returning a {@code float}. 返回{@code float}的方法返回处理程序
     */
    FLOAT(Opcodes.FRETURN, StackSize.SINGLE),

    /**
     * The method return handler for returning a {@code long}.
     */
    LONG(Opcodes.LRETURN, StackSize.DOUBLE),

    /**
     * The method return handler for returning {@code void}. 返回 {@code void} 的方法返回处理程序
     */
    VOID(Opcodes.RETURN, StackSize.ZERO),

    /**
     * The method return handler for returning a reference type. 用于返回引用类型的方法返回处理程序
     */
    REFERENCE(Opcodes.ARETURN, StackSize.SINGLE);

    /**
     * The opcode of this operation.
     */
    private final int returnOpcode;

    /**
     * The operand stack size change that is implied by this operation. 此操作所暗示的操作数堆栈大小更改
     */
    private final Size size;

    /**
     * Creates a new method return manipulation. 创建一个新的方法返回操作
     *
     * @param returnOpcode The opcode of this operation.
     * @param stackSize    The operand stack size change that is implied by this operation.
     */
    MethodReturn(int returnOpcode, StackSize stackSize) {
        this.returnOpcode = returnOpcode;
        size = stackSize.toDecreasingSize();
    }

    /**
     * Returns a method return corresponding to a given type. 返回 给定类型对应的方法返回 基础类型或者参考
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
