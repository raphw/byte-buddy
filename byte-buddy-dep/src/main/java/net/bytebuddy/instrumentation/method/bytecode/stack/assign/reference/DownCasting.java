package net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DownCasting implements StackManipulation {

    private final String targetTypeInternalName;

    public DownCasting(TypeDescription targetType) {
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

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && targetTypeInternalName.equals(((DownCasting) other).targetTypeInternalName);
    }

    @Override
    public int hashCode() {
        return targetTypeInternalName.hashCode();
    }

    @Override
    public String toString() {
        return "DownCasting{targetTypeInternalName='" + targetTypeInternalName + '\'' + '}';
    }
}
