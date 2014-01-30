package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;

public enum TypeSize {

    ZERO(0),
    SINGLE(1),
    DOUBLE(2);

    public static TypeSize of(Class<?> type) {
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

    private TypeSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public Assignment.Size toIncreasingSize() {
        return new Assignment.Size(getSize(), getSize());
    }

    public Assignment.Size toDecreasingSize() {
        return new Assignment.Size(-1 * getSize(), 0);
    }

    public TypeSize maximum(TypeSize typeSize) {
        switch (this) {
            case ZERO:
                return typeSize;
            case SINGLE:
                switch (typeSize) {
                    case DOUBLE:
                        return typeSize;
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
