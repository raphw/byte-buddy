package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.method.utility.MethodDescriptor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum MethodReturn {

    INTEGER(Opcodes.IRETURN, -1),
    DOUBLE(Opcodes.DRETURN, -2),
    FLOAT(Opcodes.FRETURN, -1),
    LONG(Opcodes.LRETURN, -2),
    ANY_REFERENCE(Opcodes.ARETURN, -1),
    VOID(Opcodes.RETURN, 0);

    public static MethodReturn forType(String name) {
        switch (name.charAt(0)) {
            case MethodDescriptor.OBJECT_REFERENCE_SYMBOL:
            case MethodDescriptor.ARRAY_REFERENCE_SYMBOL:
                return ANY_REFERENCE;
            case MethodDescriptor.INT_SYMBOL:
            case MethodDescriptor.BOOLEAN_SYMBOL:
            case MethodDescriptor.BYTE_SYMBOL:
            case MethodDescriptor.CHAR_SYMBOL:
            case MethodDescriptor.SHORT_SYMBOL:
                return INTEGER;
            case MethodDescriptor.DOUBLE_SYMBOL:
                return DOUBLE;
            case MethodDescriptor.FLOAT_SYMBOL:
                return FLOAT;
            case MethodDescriptor.LONG_SYMBOL:
                return LONG;
            case MethodDescriptor.VOID_SYMBOL:
                return VOID;
            default:
                throw new IllegalArgumentException("Illegal method argument type: " + name);
        }
    }

    private final int returnOpcode;
    private final int sizeChange;

    private MethodReturn(int returnOpcode, int sizeChange) {
        this.returnOpcode = returnOpcode;
        this.sizeChange = sizeChange;
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
            return size.aggregateLeftFirst(sizeChange);
        }
    }

    public Assignment returnAfter(Assignment returnValuePreparationAssignment) {
        return new MethodReturnValueAssignment(returnValuePreparationAssignment);
    }
}
