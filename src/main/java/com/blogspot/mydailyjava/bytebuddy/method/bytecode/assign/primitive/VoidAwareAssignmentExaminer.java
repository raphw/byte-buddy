package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.AssignmentExaminer;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.utility.MethodDescriptor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class VoidAwareAssignmentExaminer implements AssignmentExaminer {

    private static enum ValueRemovingAssignment implements Assignment {

        POP_ONE_VALUE(Opcodes.POP, new Size(-1, 0)),
        POP_TWO_VALUES(Opcodes.POP2, new Size(-2, 0));

        public static ValueRemovingAssignment of(String typeName) {
            switch (typeName.charAt(0)) {
                case MethodDescriptor.LONG_SYMBOL:
                case MethodDescriptor.DOUBLE_SYMBOL:
                    return POP_TWO_VALUES;
                case MethodDescriptor.OBJECT_REFERENCE_SYMBOL:
                case MethodDescriptor.ARRAY_REFERENCE_SYMBOL:
                case MethodDescriptor.INT_SYMBOL:
                case MethodDescriptor.BOOLEAN_SYMBOL:
                case MethodDescriptor.BYTE_SYMBOL:
                case MethodDescriptor.CHAR_SYMBOL:
                case MethodDescriptor.FLOAT_SYMBOL:
                case MethodDescriptor.SHORT_SYMBOL:
                    return POP_ONE_VALUE;
                default:
                    throw new IllegalArgumentException("Cannot pop type from stack: " + typeName);
            }
        }

        public static ValueRemovingAssignment of(Class<?> type) {
            if (type == long.class || type == double.class) {
                return POP_TWO_VALUES;
            } else if (type == void.class) {
                throw new IllegalArgumentException("Cannot pop void type from stack");
            } else {
                return POP_ONE_VALUE;
            }
        }

        private final int removalOpCode;
        private final Size sizeChange;

        private ValueRemovingAssignment(int removalOpCode, Size sizeChange) {
            this.removalOpCode = removalOpCode;
            this.sizeChange = sizeChange;
        }

        @Override
        public boolean isAssignable() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitInsn(removalOpCode);
            return sizeChange;
        }
    }

    private final AssignmentExaminer assignmentExaminer;

    public VoidAwareAssignmentExaminer(AssignmentExaminer assignmentExaminer) {
        this.assignmentExaminer = assignmentExaminer;
    }

    @Override
    public Assignment assign(String superTypeName, Class<?> subType, boolean considerRuntimeType) {
        boolean superTypeIsVoid = isVoid(superTypeName), subTypeIsVoid = subType == void.class;
        if (superTypeIsVoid && subTypeIsVoid) {
            return LegalTrivialAssignment.INSTANCE;
        } else if (superTypeIsVoid /* && !subTypeIsVoid */) {
            return IllegalAssignment.INSTANCE;
        } else if (/* !superTypeIsVoid && */ subTypeIsVoid) {
            return ValueRemovingAssignment.of(superTypeName);
        } else {
            return assignmentExaminer.assign(superTypeName, subType, considerRuntimeType);
        }
    }

    @Override
    public Assignment assign(Class<?> superType, String subTypeName, boolean considerRuntimeType) {
        boolean superTypeIsVoid = superType == void.class, subTypeIsVoid = isVoid(subTypeName);
        if (superTypeIsVoid && subTypeIsVoid) {
            return LegalTrivialAssignment.INSTANCE;
        } else if (superTypeIsVoid /* && !subTypeIsVoid */) {
            return IllegalAssignment.INSTANCE;
        } else if (/* !superTypeIsVoid && */ subTypeIsVoid) {
            return ValueRemovingAssignment.of(superType);
        } else {
            return assignmentExaminer.assign(superType, subTypeName, considerRuntimeType);
        }
    }

    private static boolean isVoid(String typeName) {
        return typeName.length() == 1 && typeName.charAt(0) == MethodDescriptor.VOID_SYMBOL;
    }
}
