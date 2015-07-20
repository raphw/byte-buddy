package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Represents a constant representing any loaded Java {@link java.lang.Class}.
 */
public enum ClassConstant implements StackManipulation {

    /**
     * The {@code void} type.
     */
    VOID(Void.class),

    /**
     * The {@code boolean} type.
     */
    BOOLEAN(Boolean.class),

    /**
     * The {@code byte} type.
     */
    BYTE(Byte.class),

    /**
     * The {@code short} type.
     */
    SHORT(Short.class),

    /**
     * The {@code char} type.
     */
    CHARACTER(Character.class),

    /**
     * The {@code int} type.
     */
    INTEGER(Integer.class),

    /**
     * The {@code long} type.
     */
    LONG(Long.class),

    /**
     * The {@code float} type.
     */
    FLOAT(Float.class),

    /**
     * The {@code double} type.
     */
    DOUBLE(Double.class);

    /**
     * The size of a {@link java.lang.Class} on the operand stack.
     */
    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    /**
     * The field name that stores a reference to the primitive type representation.
     */
    private static final String PRIMITIVE_TYPE_FIELD = "TYPE";

    /**
     * The descriptor of the {@link java.lang.Class} type.
     */
    private static final String CLASS_TYPE_INTERNAL_NAME = "Ljava/lang/Class;";

    /**
     * The internal name of the type owning the field.
     */
    private final String fieldOwnerInternalName;

    /**
     * Creates a new class constant for a primitive type.
     *
     * @param type The primitive type to represent.
     */
    ClassConstant(Class<?> type) {
        fieldOwnerInternalName = Type.getInternalName(type);
    }

    /**
     * Returns a stack manipulation that loads a {@link java.lang.Class} type onto the operand stack which
     * represents the given type.
     *
     * @param typeDescription The type to load onto the operand stack.
     * @return The corresponding stack manipulation.
     */
    public static StackManipulation of(TypeDescription typeDescription) {
        if (typeDescription.represents(void.class)) {
            return VOID;
        } else if (typeDescription.represents(boolean.class)) {
            return BOOLEAN;
        } else if (typeDescription.represents(byte.class)) {
            return BYTE;
        } else if (typeDescription.represents(short.class)) {
            return SHORT;
        } else if (typeDescription.represents(char.class)) {
            return CHARACTER;
        } else if (typeDescription.represents(int.class)) {
            return INTEGER;
        } else if (typeDescription.represents(long.class)) {
            return LONG;
        } else if (typeDescription.represents(float.class)) {
            return FLOAT;
        } else if (typeDescription.represents(double.class)) {
            return DOUBLE;
        } else {
            return new ForReferenceType(typeDescription);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, fieldOwnerInternalName, PRIMITIVE_TYPE_FIELD, CLASS_TYPE_INTERNAL_NAME);
        return SIZE;
    }

    @Override
    public String toString() {
        return "ClassConstant." + name();
    }

    /**
     * A class constant for a non-primitive {@link java.lang.Class}.
     */
    protected static class ForReferenceType implements StackManipulation {

        /**
         * The type which should be loaded onto the operand stack as a class value.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a stack manipulation that represents loading a class constant onto the stack.
         *
         * @param typeDescription A description of the class to load onto the stack.
         */
        protected ForReferenceType(TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitLdcInsn(Type.getType(typeDescription.getDescriptor()));
            return SIZE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && typeDescription.equals(((ForReferenceType) other).typeDescription);
        }

        @Override
        public int hashCode() {
            return typeDescription.hashCode();
        }

        @Override
        public String toString() {
            return "ClassConstant.ForReferenceType{typeDescription=" + typeDescription + '}';
        }
    }
}
