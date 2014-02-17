package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;

public enum StackSize {

    ZERO(0),
    SINGLE(1),
    DOUBLE(2);

    public static StackSize of(Class<?> type) {
        if (type == void.class) {
            return ZERO;
        } else if (type == double.class || type == long.class) {
            return DOUBLE;
        } else {
            return SINGLE;
        }
    }

    public static int sizeOf(Iterable<? extends Class<?>> types) {
        int size = 0;
        for (Class<?> type : types) {
            size += of(type).getSize();
        }
        return size;
    }

    private final int size;

    private StackSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public StackManipulation.Size toIncreasingSize() {
        return new StackManipulation.Size(getSize(), getSize());
    }

    public StackManipulation.Size toDecreasingSize() {
        return new StackManipulation.Size(-1 * getSize(), 0);
    }

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
