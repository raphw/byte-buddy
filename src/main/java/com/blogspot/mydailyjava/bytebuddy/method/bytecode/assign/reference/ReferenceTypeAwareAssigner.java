package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.reference;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public abstract class ReferenceTypeAwareAssigner implements Assigner {

    private static class DownCastAssignment implements Assignment {

        private static final Size NULL_SIZE = new Size(TypeSize.NONE.getSize(), TypeSize.NONE.getSize());

        private final String targetTypeInternalName;

        private DownCastAssignment(Class<?> targetTypeInternalName) {
            this.targetTypeInternalName = Type.getInternalName(targetTypeInternalName);
        }

        @Override
        public boolean isAssignable() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, targetTypeInternalName);
            return NULL_SIZE;
        }
    }

    @Override
    public Assignment assign(Class<?> superType, Class subType, boolean considerRuntimeType) {
        if (superType.isAssignableFrom(subType)) {
            return LegalTrivialAssignment.INSTANCE;
        } else if (considerRuntimeType) {
            return new DownCastAssignment(subType);
        } else {
            return IllegalAssignment.INSTANCE;
        }
    }
}
