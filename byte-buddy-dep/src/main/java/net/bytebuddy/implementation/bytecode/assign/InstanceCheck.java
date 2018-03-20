package net.bytebuddy.implementation.bytecode.assign;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Implements an {@code instanceof} check.
 */
@EqualsAndHashCode
public class InstanceCheck implements StackManipulation {

    /**
     * The type to apply the instance check against.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new instance check.
     *
     * @param typeDescription The type to apply the instance check against.
     */
    protected InstanceCheck(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Creates a new instance check.
     *
     * @param typeDescription The type to apply the instance check against.
     * @return An appropriate stack manipulation.
     */
    public static StackManipulation of(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            throw new IllegalArgumentException("Cannot check an instance against a primitive type: " + typeDescription);
        }
        return new InstanceCheck(typeDescription);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, typeDescription.getInternalName());
        return new Size(0, 0);
    }
}
