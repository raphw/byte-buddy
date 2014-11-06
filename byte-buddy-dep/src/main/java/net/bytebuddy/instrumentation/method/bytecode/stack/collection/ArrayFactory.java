package net.bytebuddy.instrumentation.method.bytecode.stack.collection;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.IntegerConstant;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * A {@link net.bytebuddy.instrumentation.method.bytecode.stack.collection.CollectionFactory} that is capable of
 * creating an array of a given type with any number of given values.
 */
public class ArrayFactory implements CollectionFactory {

    /**
     * The component type of the array this array factory is creating.
     */
    private final TypeDescription componentType;

    /**
     * The array creator delegate that supplies suitable opcodes for the creation of an array and the storage of
     * values inside it.
     */
    private final ArrayCreator arrayCreator;

    /**
     * The decrease of stack size after each value storage operation.
     */
    private final StackManipulation.Size sizeDecrease;

    /**
     * Creates a new array factory with a given
     * {@link net.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory.ArrayCreator}
     * without inferring the type from the component type. Normally,
     * {@link net.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory#targeting(net.bytebuddy.instrumentation.type.TypeDescription)}
     * should be used.
     *
     * @param componentType The component type of the array factory.
     * @param arrayCreator  The array creator responsible for providing the correct byte code instructions.
     */
    protected ArrayFactory(TypeDescription componentType, ArrayCreator arrayCreator) {
        this.componentType = componentType;
        this.arrayCreator = arrayCreator;
        // Size decreases by index and array reference (2) and array element (1, 2) after each element storage.
        sizeDecrease = StackSize.DOUBLE.toDecreasingSize().aggregate(componentType.getStackSize().toDecreasingSize());
    }

    /**
     * Creates a new array factory for a given component type.
     *
     * @param componentType The component type of the array that is to be build.
     * @return A new array factory for the given type.
     */
    public static ArrayFactory targeting(TypeDescription componentType) {
        return new ArrayFactory(componentType, makeArrayCreatorFor(componentType));
    }

    /**
     * Creates a suitable array creator for the given component type.
     *
     * @param componentType The component type of the array to be created.
     * @return A suitable array creator.
     */
    private static ArrayCreator makeArrayCreatorFor(TypeDescription componentType) {
        if (componentType.isPrimitive()) {
            if (componentType.represents(boolean.class)) {
                return ArrayCreator.ForPrimitiveType.BOOLEAN;
            } else if (componentType.represents(byte.class)) {
                return ArrayCreator.ForPrimitiveType.BYTE;
            } else if (componentType.represents(short.class)) {
                return ArrayCreator.ForPrimitiveType.SHORT;
            } else if (componentType.represents(char.class)) {
                return ArrayCreator.ForPrimitiveType.CHARACTER;
            } else if (componentType.represents(int.class)) {
                return ArrayCreator.ForPrimitiveType.INTEGER;
            } else if (componentType.represents(long.class)) {
                return ArrayCreator.ForPrimitiveType.LONG;
            } else if (componentType.represents(float.class)) {
                return ArrayCreator.ForPrimitiveType.FLOAT;
            } else if (componentType.represents(double.class)) {
                return ArrayCreator.ForPrimitiveType.DOUBLE;
            } else {
                throw new IllegalArgumentException("Cannot create array of type " + componentType);
            }
        } else {
            return new ArrayCreator.ForReferenceType(componentType);
        }
    }

    @Override
    public StackManipulation withValues(List<StackManipulation> stackManipulations) {
        return new ArrayStackManipulation(stackManipulations);
    }

    @Override
    public TypeDescription getComponentType() {
        return componentType;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && componentType.equals(((ArrayFactory) other).componentType);
    }

    @Override
    public int hashCode() {
        return componentType.hashCode();
    }

    @Override
    public String toString() {
        return "ArrayFactory{" +
                "componentType=" + componentType +
                ", arrayCreator=" + arrayCreator +
                ", sizeDecrease=" + sizeDecrease +
                '}';
    }

    /**
     * An array creator is responsible for providing correct byte code instructions for creating an array
     * and for storing values into it.
     */
    protected static interface ArrayCreator extends StackManipulation {

        /**
         * The creation of an array consumes one slot on the operand stack and adds a new value to it.
         * Therefore, the operand stack's size is not altered.
         */
        static final StackManipulation.Size ARRAY_CREATION_SIZE_CHANGE = StackSize.ZERO.toDecreasingSize();

        /**
         * The opcode instruction for storing a value of the component type inside an array.
         *
         * @return The correct storage opcode for the represented type.
         */
        int getStorageOpcode();

        /**
         * An array creator implementation for primitive types.
         */
        static enum ForPrimitiveType implements ArrayCreator {

            /**
             * An array creator for creating {@code boolean[]} arrays.
             */
            BOOLEAN(Opcodes.T_BOOLEAN, Opcodes.BASTORE),

            /**
             * An array creator for creating {@code byte[]} arrays.
             */
            BYTE(Opcodes.T_BYTE, Opcodes.BASTORE),

            /**
             * An array creator for creating {@code short[]} arrays.
             */
            SHORT(Opcodes.T_SHORT, Opcodes.SASTORE),

            /**
             * An array creator for creating {@code char[]} arrays.
             */
            CHARACTER(Opcodes.T_CHAR, Opcodes.CASTORE),

            /**
             * An array creator for creating {@code int[]} arrays.
             */
            INTEGER(Opcodes.T_INT, Opcodes.IASTORE),

            /**
             * An array creator for creating {@code long[]} arrays.
             */
            LONG(Opcodes.T_LONG, Opcodes.LASTORE),

            /**
             * An array creator for creating {@code float[]} arrays.
             */
            FLOAT(Opcodes.T_FLOAT, Opcodes.FASTORE),

            /**
             * An array creator for creating {@code double[]} arrays.
             */
            DOUBLE(Opcodes.T_DOUBLE, Opcodes.DASTORE);

            /**
             * The opcode for creating an array of this type.
             */
            private final int creationOpcode;

            /**
             * The opcode for storing a value in an array of this type.
             */
            private final int storageOpcode;

            /**
             * Creates a new primitive array creator.
             *
             * @param creationOpcode The opcode for creating an array of this type.
             * @param storageOpcode  The opcode for storing a value in an array of this type.
             */
            private ForPrimitiveType(int creationOpcode, int storageOpcode) {
                this.creationOpcode = creationOpcode;
                this.storageOpcode = storageOpcode;
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                methodVisitor.visitIntInsn(Opcodes.NEWARRAY, creationOpcode);
                return ARRAY_CREATION_SIZE_CHANGE;
            }

            @Override
            public int getStorageOpcode() {
                return storageOpcode;
            }
        }

        /**
         * An array creator implementation for reference types.
         */
        static class ForReferenceType implements ArrayCreator {

            /**
             * The internal name of this array's non-primitive component type.
             */
            private final String internalTypeName;

            /**
             * Creates a new array creator for a reference type.
             *
             * @param referenceType The internal name of this array's non-primitive component type.
             */
            protected ForReferenceType(TypeDescription referenceType) {
                this.internalTypeName = referenceType.getInternalName();
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalTypeName);
                return ARRAY_CREATION_SIZE_CHANGE;
            }

            @Override
            public int getStorageOpcode() {
                return Opcodes.AASTORE;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && internalTypeName.equals(((ForReferenceType) other).internalTypeName);
            }

            @Override
            public int hashCode() {
                return internalTypeName.hashCode();
            }

            @Override
            public String toString() {
                return "ArrayFactory.ArrayCreator.Reference{" +
                        "internalTypeName='" + internalTypeName + '\'' +
                        '}';
            }
        }
    }

    /**
     * A stack manipulation for creating an array as defined by the enclosing array factory.
     */
    private class ArrayStackManipulation implements StackManipulation {

        /**
         * A list of value load instructions that are to be stored inside the created array.
         */
        private final List<StackManipulation> stackManipulations;

        /**
         * Creates a new array loading instruction.
         *
         * @param stackManipulations A list of value load instructions that are to be stored inside the created array.
         */
        public ArrayStackManipulation(List<StackManipulation> stackManipulations) {
            this.stackManipulations = stackManipulations;
        }

        @Override
        public boolean isValid() {
            for (StackManipulation stackManipulation : stackManipulations) {
                if (!stackManipulation.isValid()) {
                    return false;
                }
            }
            return arrayCreator.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            Size size = IntegerConstant.forValue(stackManipulations.size()).apply(methodVisitor, instrumentationContext);
            // The array's construction does not alter the stack's size.
            size = size.aggregate(arrayCreator.apply(methodVisitor, instrumentationContext));
            int index = 0;
            for (StackManipulation stackManipulation : stackManipulations) {
                methodVisitor.visitInsn(Opcodes.DUP);
                size = size.aggregate(StackSize.SINGLE.toIncreasingSize());
                size = size.aggregate(IntegerConstant.forValue(index++).apply(methodVisitor, instrumentationContext));
                size = size.aggregate(stackManipulation.apply(methodVisitor, instrumentationContext));
                methodVisitor.visitInsn(arrayCreator.getStorageOpcode());
                size = size.aggregate(sizeDecrease);
            }
            return size;
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private ArrayFactory getArrayFactory() {
            return ArrayFactory.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && ArrayFactory.this.equals(((ArrayStackManipulation) other).getArrayFactory())
                    && stackManipulations.equals(((ArrayStackManipulation) other).stackManipulations);
        }

        @Override
        public int hashCode() {
            return stackManipulations.hashCode();
        }

        @Override
        public String toString() {
            return "ArrayFactory.ArrayStackManipulation{" +
                    "arrayFactory=" + ArrayFactory.this +
                    "stackManipulations=" + stackManipulations +
                    '}';
        }
    }
}

