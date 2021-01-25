package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDefinition;

import java.util.Arrays;
import java.util.Collection;

/**
 * Represents the size of a Java type on the operand stack. 表示操作数堆栈上 Java 类型的大小
 */
public enum StackSize {

    /**
     * An empty stack size. 空堆栈大小
     */
    ZERO(0),

    /**
     * A single slot stack size. 单个插槽堆栈大小
     */
    SINGLE(1),

    /**
     * A double slot stack size which is required by {@code long} and {@code double} values. {@code long}和{@code double}值所需的双时隙堆栈大小
     */
    DOUBLE(2);

    /**
     * The size of the stack this instance represents. 此实例表示的堆栈的大小
     */
    private final int size;

    /**
     * Creates a new stack size.
     *
     * @param size The size of the stack this instance represents.
     */
    StackSize(int size) {
        this.size = size;
    }

    /**
     * Finds the operand stack size of a given Java type. 查找给定Java类型的操作数堆栈大小
     *
     * @param type The type of interest.
     * @return The given type's operand stack size.
     */
    public static StackSize of(Class<?> type) {
        if (type == void.class) {
            return ZERO;
        } else if (type == double.class || type == long.class) {
            return DOUBLE;
        } else {
            return SINGLE;
        }
    }

    /**
     * Represents a numeric size as a {@link StackSize}.
     *
     * @param size The size to represent. Must be {@code 0}, {@code 1} or {@code 2}.
     * @return A stack size representation for the given value.
     */
    public static StackSize of(int size) {
        switch (size) {
            case 0:
                return ZERO;
            case 1:
                return SINGLE;
            case 2:
                return DOUBLE;
            default:
                throw new IllegalArgumentException("Unexpected stack size value: " + size);
        }
    }

    /**
     * Computes the stack size of all supplied types. 计算所有提供类型的堆栈大小
     *
     * @param typeDefinition The types for which to compute the size.
     * @return The total size of all types.
     */
    public static int of(TypeDefinition... typeDefinition) {
        return of(Arrays.asList(typeDefinition));
    }

    /**
     * Computes the stack size of all supplied types.
     *
     * @param typeDefinitions The types for which to compute the size.
     * @return The total size of all types.
     */
    public static int of(Collection<? extends TypeDefinition> typeDefinitions) {
        int size = 0;
        for (TypeDefinition typeDefinition : typeDefinitions) {
            size += typeDefinition.getStackSize().getSize();
        }
        return size;
    }

    /**
     * The numeric value of this stack size representation. 此堆栈大小表示的数值
     *
     * @return An integer representing the operand stack size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Creates an instance of a
     * {@link StackManipulation.Size}
     * that describes a stack growth of this size. 创建 {@link StackManipulation.Size} 它描述了这种大小的堆栈增长
     *
     * @return A stack size growth by the size represented by this stack size. 堆栈大小由该堆栈大小表示的大小增长
     */
    public StackManipulation.Size toIncreasingSize() {
        return new StackManipulation.Size(getSize(), getSize());
    }

    /**
     * Creates an instance of a
     * {@link StackManipulation.Size}
     * that describes a stack decrease of this size.
     *
     * @return A stack size decrease by the size represented by this stack size.
     */
    public StackManipulation.Size toDecreasingSize() {
        return new StackManipulation.Size(-1 * getSize(), 0);
    }

    /**
     * Determines the maximum of two stack size representations. 确定两个堆栈大小表示的最大值
     *
     * @param stackSize The other stack size representation.
     * @return The maximum of this and the other stack size.
     */
    public StackSize maximum(StackSize stackSize) {
        switch (this) {
            case ZERO:
                return stackSize;
            case SINGLE:
                switch (stackSize) {
                    case DOUBLE:
                        return stackSize;
                    case SINGLE:
                    case ZERO:
                        return this;
                    default:
                        throw new AssertionError();
                }
            case DOUBLE:
                return this;
            default:
                throw new AssertionError();
        }
    }
}
