package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * An access representation to a given field.
 */
public enum FieldAccess {

    STATIC(Opcodes.PUTSTATIC, Opcodes.GETSTATIC, StackSize.ZERO),
    INSTANCE(Opcodes.PUTFIELD, Opcodes.GETFIELD, StackSize.SINGLE);
    private final int putterOpcode;
    private final int getterOpcode;
    private final int targetSizeChange;

    private FieldAccess(int putterOpcode, int getterOpcode, StackSize targetSizeChange) {
        this.putterOpcode = putterOpcode;
        this.getterOpcode = getterOpcode;
        this.targetSizeChange = targetSizeChange.getSize();
    }

    /**
     * Creates a field access representation for a given field.
     *
     * @param fieldDescription The field to be accessed.
     * @return A field access definition for the given field.
     */
    public static Defined forField(FieldDescription fieldDescription) {
        if (fieldDescription.isStatic()) {
            return STATIC.new AccessDispatcher(fieldDescription);
        } else {
            return INSTANCE.new AccessDispatcher(fieldDescription);
        }
    }
    /**
     * Representation of a field access for which a getter and a putter can be created.
     */
    public static interface Defined {

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

        /**
         * Returns the field for which this field access is defined for.
         *
         * @return The field for which this field access was defined for.
         */
        FieldDescription getDefinedField();
    }

    private class AccessDispatcher implements Defined {

        private final FieldDescription fieldDescription;

        private AccessDispatcher(FieldDescription fieldDescription) {
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
        public FieldDescription getDefinedField() {
            return fieldDescription;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && fieldDescription.equals(((AccessDispatcher) other).fieldDescription);
        }

        @Override
        public int hashCode() {
            return fieldDescription.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAccess.AccessDispatcher{fieldDescription=" + fieldDescription + '}';
        }

        private abstract class AbstractFieldInstruction implements StackManipulation {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                methodVisitor.visitFieldInsn(getOpcode(),
                        fieldDescription.getDeclaringType().getInternalName(),
                        fieldDescription.getInternalName(),
                        fieldDescription.getDescriptor());
                return resolveSize(fieldDescription.getFieldType().getStackSize());
            }

            protected abstract int getOpcode();

            protected abstract Size resolveSize(StackSize fieldSize);


        }

        private class FieldGetInstruction extends AbstractFieldInstruction {

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

            private AccessDispatcher getAccessDispatcher() {
                return AccessDispatcher.this;
            }
        }

        private class FieldPutInstruction extends AbstractFieldInstruction {

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

            private AccessDispatcher getAccessDispatcher() {
                return AccessDispatcher.this;
            }
        }
    }
}
