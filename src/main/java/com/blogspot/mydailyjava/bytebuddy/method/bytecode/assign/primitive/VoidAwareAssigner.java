package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.*;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class VoidAwareAssigner implements Assigner {

    private static enum ValueRemovingAssignment implements Assignment {

        POP_ONE_FRAME(Opcodes.POP, TypeSize.SINGLE.toDecreasingSize()),
        POP_TWO_FRAMES(Opcodes.POP2, TypeSize.DOUBLE.toDecreasingSize());

        public static ValueRemovingAssignment of(Class<?> type) {
            if (type == long.class || type == double.class) {
                return POP_TWO_FRAMES;
            } else if (type == void.class) {
                throw new IllegalArgumentException("Cannot pop void type from stack");
            } else {
                return POP_ONE_FRAME;
            }
        }

        private final int removalOpCode;
        private final Size sizeChange;

        private ValueRemovingAssignment(int removalOpCode, Size sizeChange) {
            this.removalOpCode = removalOpCode;
            this.sizeChange = sizeChange;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitInsn(removalOpCode);
            return sizeChange;
        }
    }

    private final Assigner nonVoidAwareAssigner;
    private final boolean returnDefaultValue;

    public VoidAwareAssigner(Assigner nonVoidAwareAssigner, boolean returnDefaultValue) {
        this.nonVoidAwareAssigner = nonVoidAwareAssigner;
        this.returnDefaultValue = returnDefaultValue;
    }

    @Override
    public Assignment assign(Class<?> superType, Class<?> subType, boolean considerRuntimeType) {
        if (superType == void.class && subType == void.class) {
            return LegalTrivialAssignment.INSTANCE;
        } else if (superType == void.class /* && !(subType == void.class) */) {
            return returnDefaultValue ? DefaultValue.load(subType) : IllegalAssignment.INSTANCE;
        } else if (/* !(superType == void.class) && */ subType == void.class) {
            return ValueRemovingAssignment.of(superType);
        } else /* !(superType == void.class) && !(subType == void.class) */ {
            return nonVoidAwareAssigner.assign(superType, subType, considerRuntimeType);
        }
    }
}
