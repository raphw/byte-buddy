package net.bytebuddy.implementation.bytecode.assign;


import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation for a type down casting. Such castings are not implicit but must be performed explicitly.
 */
public class TypeCasting implements StackManipulation {

    /**
     * The internal name of the target type of the casting.
     */
    private final String targetTypeInternalName;

    /**
     * Creates a new type casting.
     *
     * @param targetType The type to which the uppermost stack value should be casted.
     */
    public TypeCasting(TypeDescription targetType) {
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
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, targetTypeInternalName);
        return StackSize.ZERO.toIncreasingSize();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && targetTypeInternalName.equals(((TypeCasting) other).targetTypeInternalName);
    }

    @Override
    public int hashCode() {
        return targetTypeInternalName.hashCode();
    }

    @Override
    public String toString() {
        return "TypeCasting{targetTypeInternalName='" + targetTypeInternalName + '\'' + '}';
    }
}
