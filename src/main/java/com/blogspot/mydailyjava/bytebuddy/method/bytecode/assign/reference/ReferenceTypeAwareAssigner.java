package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.reference;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public enum ReferenceTypeAwareAssigner implements Assigner {
    INSTANCE;

    private static class DownCastAssignment implements Assignment {

        private final String targetTypeInternalName;

        private DownCastAssignment(Class<?> targetType) {
            this.targetTypeInternalName = Type.getInternalName(targetType);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, targetTypeInternalName);
            return TypeSize.NONE.toIncreasingSize();
        }
    }

    @Override
    public Assignment assign(Class<?> sourceType, Class<?> targetType, boolean considerRuntimeType) {
        if (sourceType.isPrimitive() || targetType.isPrimitive()) {
            if (sourceType == targetType) {
                return LegalTrivialAssignment.INSTANCE;
            } else {
                return IllegalAssignment.INSTANCE;
            }
        } else if (targetType.isAssignableFrom(sourceType)) {
            return LegalTrivialAssignment.INSTANCE;
        } else if (considerRuntimeType) {
            return new DownCastAssignment(targetType);
        } else {
            return IllegalAssignment.INSTANCE;
        }
    }
}
