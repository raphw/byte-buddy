package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.reference;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.AssignmentExaminer;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public abstract class AbstractRuntimeTypeAwareAssignmentExaminer implements AssignmentExaminer {

    private static class DownCastAssignment implements Assignment {

        private static final Size SIZE = new Size(0, 0);

        private final String targetType;

        private DownCastAssignment(Class<?> targetType) {
            this.targetType = Type.getInternalName(targetType);
        }

        private DownCastAssignment(String targetType) {
            this.targetType = targetType.substring(1, targetType.length() - 1);
        }

        @Override
        public boolean isAssignable() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, targetType);
            return SIZE;
        }
    }

    @Override
    public Assignment assign(String superTypeName, Class<?> subType, boolean considerRuntimeType) {
        if (isAssignable(superTypeName, subType)) {
            return LegalTrivialAssignment.INSTANCE;
        } else if (considerRuntimeType && isAssignable(subType, superTypeName)) {
            return new DownCastAssignment(subType);
        } else {
            return IllegalAssignment.INSTANCE;
        }
    }

    @Override
    public Assignment assign(Class<?> superType, String subTypeName, boolean considerRuntimeType) {
        if (isAssignable(superType, subTypeName)) {
            return LegalTrivialAssignment.INSTANCE;
        } else if (considerRuntimeType && isAssignable(subTypeName, superType)) {
            return new DownCastAssignment(subTypeName);
        } else {
            return IllegalAssignment.INSTANCE;
        }
    }

    protected abstract boolean isAssignable(String superTypeName, Class<?> subType);

    protected abstract boolean isAssignable(Class<?> superType, String subTypeName);
}
