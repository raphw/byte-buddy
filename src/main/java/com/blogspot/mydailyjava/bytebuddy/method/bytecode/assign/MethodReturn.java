package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ValueSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodReturn {

    INTEGER(Opcodes.IRETURN, ValueSize.SINGLE),
    DOUBLE(Opcodes.DRETURN, ValueSize.DOUBLE),
    FLOAT(Opcodes.FRETURN, ValueSize.SINGLE),
    LONG(Opcodes.LRETURN, ValueSize.DOUBLE),
    VOID(Opcodes.RETURN, ValueSize.NONE),
    ANY_REFERENCE(Opcodes.ARETURN, ValueSize.SINGLE);

    public static MethodReturn returning(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == long.class) {
                return LONG;
            } else if (type == double.class) {
                return DOUBLE;
            } else if (type == float.class) {
                return FLOAT;
            } else if (type == void.class) {
                return VOID;
            } else {
                return INTEGER;
            }
        } else {
            return ANY_REFERENCE;
        }
    }

    private final int returnOpcode;
    private final ValueSize valueSize;

    private MethodReturn(int returnOpcode, ValueSize valueSize) {
        this.returnOpcode = returnOpcode;
        this.valueSize = valueSize;
    }

    private class MethodReturnValueAssignment implements Assignment {

        private final Assignment returnValuePreparationAssignment;

        private MethodReturnValueAssignment(Assignment returnValuePreparationAssignment) {
            this.returnValuePreparationAssignment = returnValuePreparationAssignment;
        }

        @Override
        public boolean isAssignable() {
            return returnValuePreparationAssignment.isAssignable();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            Size size = returnValuePreparationAssignment.apply(methodVisitor);
            methodVisitor.visitInsn(returnOpcode);
            return size.aggregateLeftFirst(-1 * valueSize.getSize());
        }
    }

    public Assignment returnAfter(Assignment returnValuePreparationAssignment) {
        return new MethodReturnValueAssignment(returnValuePreparationAssignment);
    }

    public Assignment.Size apply(MethodVisitor methodVisitor) {
        return new MethodReturnValueAssignment(LegalTrivialAssignment.INSTANCE).apply(methodVisitor);
    }
}
