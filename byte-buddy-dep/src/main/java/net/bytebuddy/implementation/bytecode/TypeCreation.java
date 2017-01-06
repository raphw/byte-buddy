package net.bytebuddy.implementation.bytecode;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation for creating an <i>undefined</i> type on which a constructor is to be called.
 */
@EqualsAndHashCode
public class TypeCreation implements StackManipulation {

    /**
     * The type that is being created.
     */
    private final TypeDescription typeDescription;

    /**
     * Constructs a new type creation.
     *
     * @param typeDescription The type to be create.
     */
    protected TypeCreation(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Creates a type creation for the given type.
     *
     * @param typeDescription The type to be create.
     * @return A stack manipulation that represents the creation of the given type.
     */
    public static StackManipulation of(TypeDescription typeDescription) {
        if (typeDescription.isArray() || typeDescription.isPrimitive() || typeDescription.isAbstract()) {
            throw new IllegalArgumentException(typeDescription + " is not instantiable");
        }
        return new TypeCreation(typeDescription);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeDescription.getInternalName());
        return new Size(1, 1);
    }
}
