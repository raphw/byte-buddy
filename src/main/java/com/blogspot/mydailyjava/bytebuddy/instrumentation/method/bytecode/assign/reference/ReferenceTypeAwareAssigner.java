package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.reference;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum ReferenceTypeAwareAssigner implements Assigner {
    INSTANCE;

    private static class DownCastAssignment implements Assignment {

        private final String targetTypeInternalName;

        private DownCastAssignment(TypeDescription targetType) {
            this.targetTypeInternalName = targetType.getInternalName();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, targetTypeInternalName);
            return TypeSize.ZERO.toIncreasingSize();
        }
    }

    @Override
    public Assignment assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType) {
        if (sourceType.isPrimitive() || targetType.isPrimitive()) {
            if (sourceType.equals(targetType)) {
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
