package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation for creating an <i>undefined</i> type on which a constructor is to be called.
 */
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
    public static StackManipulation forType(TypeDescription typeDescription) {
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
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeDescription.getInternalName());
        return new Size(1, 1);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeDescription.equals(((TypeCreation) other).typeDescription);
    }

    @Override
    public int hashCode() {
        return typeDescription.hashCode();
    }

    @Override
    public String toString() {
        return "TypeCreation{typeDescription=" + typeDescription + '}';
    }
}
