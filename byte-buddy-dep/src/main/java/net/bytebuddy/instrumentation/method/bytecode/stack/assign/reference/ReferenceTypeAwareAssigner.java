package net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.IllegalStackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A simple assigner that is capable of handling the casting of reference types. Primitives can only be assigned to
 * each other if they represent the same type.
 */
public enum ReferenceTypeAwareAssigner implements Assigner {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public StackManipulation assign(TypeDescription sourceType, TypeDescription targetType, boolean considerRuntimeType) {
        if (sourceType.isPrimitive() || targetType.isPrimitive()) {
            if (sourceType.equals(targetType)) {
                return LegalTrivialStackManipulation.INSTANCE;
            } else {
                return IllegalStackManipulation.INSTANCE;
            }
        } else if (targetType.isAssignableFrom(sourceType)) {
            return LegalTrivialStackManipulation.INSTANCE;
        } else if (considerRuntimeType) {
            return new DownCastStackManipulation(targetType);
        } else {
            return IllegalStackManipulation.INSTANCE;
        }
    }

    private static class DownCastStackManipulation implements StackManipulation {

        private final String targetTypeInternalName;

        private DownCastStackManipulation(TypeDescription targetType) {
            this.targetTypeInternalName = targetType.getInternalName();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, targetTypeInternalName);
            return StackSize.ZERO.toIncreasingSize();
        }
    }
}
