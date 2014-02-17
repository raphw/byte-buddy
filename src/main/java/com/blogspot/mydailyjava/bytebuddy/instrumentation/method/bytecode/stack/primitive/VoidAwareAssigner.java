package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.*;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class VoidAwareAssigner implements Assigner {

    private static enum ValueRemovingStackManipulation implements StackManipulation {

        POP_ONE_FRAME(Opcodes.POP, StackSize.SINGLE.toDecreasingSize()),
        POP_TWO_FRAMES(Opcodes.POP2, StackSize.DOUBLE.toDecreasingSize());

        public static ValueRemovingStackManipulation of(TypeDescription typeDescription) {
            if (typeDescription.represents(long.class) || typeDescription.represents(double.class)) {
                return POP_TWO_FRAMES;
            } else if (typeDescription.represents(void.class)) {
                throw new IllegalArgumentException("Cannot pop void type from stack");
            } else {
                return POP_ONE_FRAME;
            }
        }

        private final int removalOpCode;
        private final Size sizeChange;

        private ValueRemovingStackManipulation(int removalOpCode, Size sizeChange) {
            this.removalOpCode = removalOpCode;
            this.sizeChange = sizeChange;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
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
    public StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType) {
        if (sourceType.represents(void.class) && targetType.represents(void.class)) {
            return LegalTrivialStackManipulation.INSTANCE;
        } else if (sourceType.represents(void.class) /* && subType != void.class */) {
            return returnDefaultValue ? DefaultValue.load(targetType) : IllegalStackManipulation.INSTANCE;
        } else if (/* superType != void.class && */ targetType.represents(void.class)) {
            return ValueRemovingStackManipulation.of(sourceType);
        } else /* superType != void.class && subType != void.class */ {
            return nonVoidAwareAssigner.assign(sourceType, targetType, considerRuntimeType);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && returnDefaultValue == ((VoidAwareAssigner) other).returnDefaultValue
                && nonVoidAwareAssigner.equals(((VoidAwareAssigner) other).nonVoidAwareAssigner);
    }

    @Override
    public int hashCode() {
        return 31 * nonVoidAwareAssigner.hashCode() + (returnDefaultValue ? 1 : 0);
    }

    @Override
    public String toString() {
        return "VoidAwareAssigner{chained=" + nonVoidAwareAssigner + ", returnDefaultValue=" + returnDefaultValue + '}';
    }
}
