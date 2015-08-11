package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * An access representation to a given field.
 */
public enum FieldAccess {

    /**
     * The representation of field access to a static field.
     */
    STATIC(Opcodes.PUTSTATIC, Opcodes.GETSTATIC, StackSize.ZERO),

    /**
     * The representation of field access to an instance field.
     */
    INSTANCE(Opcodes.PUTFIELD, Opcodes.GETFIELD, StackSize.SINGLE);

    /**
     * The opcode for setting a field value.
     */
    private final int putterOpcode;

    /**
     * The opcode for getting a field value.
     */
    private final int getterOpcode;

    /**
     * The amount of operand slots this field access operation consumes when it is applied before eventually
     * adding new values onto the operand stack.
     */
    private final int targetSizeChange;

    /**
     * Creates a new field access.
     *
     * @param putterOpcode     The opcode for setting a field value.
     * @param getterOpcode     The opcode for getting a field value.
     * @param targetSizeChange The amount of operand slots this field access operation consumes when it is applied
     *                         before eventually adding new values onto the operand stack.
     */
    FieldAccess(int putterOpcode, int getterOpcode, StackSize targetSizeChange) {
        this.putterOpcode = putterOpcode;
        this.getterOpcode = getterOpcode;
        this.targetSizeChange = targetSizeChange.getSize();
    }

    /**
     * Creates an accessor to read an enumeration value.
     *
     * @param enumerationDescription The description of the enumeration.
     * @return A stack manipulation for reading the enumeration.
     */
    public static StackManipulation forEnumeration(EnumerationDescription enumerationDescription) {
        FieldList<?> fieldList = enumerationDescription.getEnumerationType().getDeclaredFields().filter(named(enumerationDescription.getValue()));
        return fieldList.size() != 1 || !fieldList.getOnly().isStatic() || !fieldList.getOnly().isPublic() || !fieldList.getOnly().isEnum()
                ? StackManipulation.Illegal.INSTANCE
                : STATIC.new AccessDispatcher(fieldList.getOnly()).getter();
    }

    /**
     * Creates a field access representation for a given field.
     *
     * @param fieldDescription The field to be accessed.
     * @return A field access definition for the given field.
     */
    public static Defined forField(FieldDescription.InDefinedShape fieldDescription) {
        return fieldDescription.isStatic()
                ? STATIC.new AccessDispatcher(fieldDescription)
                : INSTANCE.new AccessDispatcher(fieldDescription);
    }

    /**
     * Creates a field access representation for a given field. If the field's return type derives from its declared shape, the value
     * is additionally casted to the generically resolved field.
     *
     * @param fieldDescription The field to be accessed.
     * @return A field access definition for the given field.
     */
    public static Defined forField(FieldDescription fieldDescription) {
        FieldDescription.InDefinedShape declaredField = fieldDescription.asDefined();
        return fieldDescription.getType().asErasure().equals(declaredField.getType().asErasure())
                ? forField(declaredField)
                : OfGenericField.of(fieldDescription, forField(declaredField));
    }

    @Override
    public String toString() {
        return "FieldAccess." + name();
    }

    /**
     * Representation of a field access for which a getter and a putter can be created.
     */
    public interface Defined {

        /**
         * Creates a getter representation for a given field.
         *
         * @return A stack manipulation representing the retrieval of a field value.
         */
        StackManipulation getter();

        /**
         * Creates a putter representation for a given field.
         *
         * @return A stack manipulation representing the setting of a field value.
         */
        StackManipulation putter();
    }

    /**
     * A dispatcher for implementing a generic read or write access on a field.
     */
    protected static class OfGenericField implements Defined {

        /**
         * The resolved generic field type.
         */
        private final TypeDescription targetType;

        /**
         * An accessor for the field in its defined shape.
         */
        private final Defined defined;

        /**
         * Creates a new dispatcher for a generic field.
         *
         * @param targetType The resolved generic field type.
         * @param defined    An accessor for the field in its defined shape.
         */
        protected OfGenericField(TypeDescription targetType, Defined defined) {
            this.targetType = targetType;
            this.defined = defined;
        }

        /**
         * Creates a generic access dispatcher for a given field.
         *
         * @param fieldDescription The field that is being accessed.
         * @param fieldAccess      A field accessor for the field in its defined shape.
         * @return A field access dispatcher for the given field.
         */
        protected static Defined of(FieldDescription fieldDescription, Defined fieldAccess) {
            return new OfGenericField(fieldDescription.getType().asErasure(), fieldAccess);
        }

        @Override
        public StackManipulation getter() {
            return new StackManipulation.Compound(defined.getter(), TypeCasting.to(targetType));
        }

        @Override
        public StackManipulation putter() {
            return defined.putter();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            OfGenericField that = (OfGenericField) other;
            return targetType.equals(that.targetType) && defined.equals(that.defined);
        }

        @Override
        public int hashCode() {
            int result = targetType.hashCode();
            result = 31 * result + defined.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FieldAccess.OfGenericField{" +
                    "targetType=" + targetType +
                    ", defined=" + defined +
                    '}';
        }
    }

    /**
     * A dispatcher for implementing a non-generic read or write access on a field.
     */
    protected class AccessDispatcher implements Defined {

        /**
         * A description of the accessed field.
         */
        private final FieldDescription fieldDescription;

        /**
         * Creates a new access dispatcher.
         *
         * @param fieldDescription A description of the accessed field.
         */
        protected AccessDispatcher(FieldDescription fieldDescription) {
            this.fieldDescription = fieldDescription;
        }

        @Override
        public StackManipulation getter() {
            return new FieldGetInstruction();
        }

        @Override
        public StackManipulation putter() {
            return new FieldPutInstruction();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && FieldAccess.this.equals(((AccessDispatcher) other).getFieldAccess())
                    && fieldDescription.equals(((AccessDispatcher) other).fieldDescription);
        }

        @Override
        public int hashCode() {
            return fieldDescription.hashCode() + 31 * FieldAccess.this.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAccess.AccessDispatcher{" +
                    "fieldAccess=" + FieldAccess.this +
                    ", fieldDescription=" + fieldDescription +
                    '}';
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private FieldAccess getFieldAccess() {
            return FieldAccess.this;
        }

        /**
         * An abstract base implementation for accessing a field value.
         */
        private abstract class AbstractFieldInstruction implements StackManipulation {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                methodVisitor.visitFieldInsn(getOpcode(),
                        fieldDescription.getDeclaringType().asErasure().getInternalName(),
                        fieldDescription.getInternalName(),
                        fieldDescription.getDescriptor());
                return resolveSize(fieldDescription.getType().getStackSize());
            }

            /**
             * Returns the opcode for implementing the field access.
             *
             * @return The opcode for implementing the field access.
             */
            protected abstract int getOpcode();

            /**
             * Resolves the actual size of this field access operation.
             *
             * @param fieldSize The size of the accessed field.
             * @return The size of the field access operation based on the field's size.
             */
            protected abstract Size resolveSize(StackSize fieldSize);
        }

        /**
         * A reading field access operation.
         */
        protected class FieldGetInstruction extends AbstractFieldInstruction {

            @Override
            protected int getOpcode() {
                return getterOpcode;
            }

            @Override
            protected Size resolveSize(StackSize fieldSize) {
                int sizeChange = fieldSize.getSize() - targetSizeChange;
                return new Size(sizeChange, sizeChange);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && getAccessDispatcher().equals(((FieldGetInstruction) other).getAccessDispatcher());
            }

            @Override
            public int hashCode() {
                return getAccessDispatcher().hashCode() + 7;
            }

            @Override
            public String toString() {
                return "FieldAccess.AccessDispatcher.FieldGetInstruction{fieldDescription=" + fieldDescription + '}';
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private AccessDispatcher getAccessDispatcher() {
                return AccessDispatcher.this;
            }
        }

        /**
         * A writing field access operation.
         */
        protected class FieldPutInstruction extends AbstractFieldInstruction {

            @Override
            protected int getOpcode() {
                return putterOpcode;
            }

            @Override
            protected Size resolveSize(StackSize fieldSize) {
                return new Size(-1 * (fieldSize.getSize() + targetSizeChange), 0);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && getAccessDispatcher().equals(((FieldPutInstruction) other).getAccessDispatcher());
            }

            @Override
            public int hashCode() {
                return getAccessDispatcher().hashCode() + 14;
            }

            @Override
            public String toString() {
                return "FieldAccess.AccessDispatcher.FieldPutInstruction{fieldDescription=" + fieldDescription + '}';
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private AccessDispatcher getAccessDispatcher() {
                return AccessDispatcher.this;
            }
        }
    }
}
