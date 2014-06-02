package net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation for a type down casting. Such castings are not implicit but must be performed explicitly.
 */
public class DownCasting implements StackManipulation {

    /**
     * The internal name of the target type of the casting.
     */
    private final String targetTypeInternalName;

    /**
     * Creates a new type casting.
     *
     * @param targetType The type to which the uppermost stack value should be casted.
     */
    public DownCasting(TypeDescription targetType) {
        if (targetType.isPrimitive()) {
            throw new IllegalArgumentException("Cannot cast to primitive type " + targetType);
        }
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
