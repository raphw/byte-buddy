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

    private static final StackManipulation.Size SIZE = StackSize.ZERO.toDecreasingSize();
    private final TypeDescription componentType;
    private final ArrayCreator arrayCreator;
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

    private static ArrayCreator makeArrayCreatorFor(TypeDescription componentType) {
        if (componentType.isPrimitive()) {
            if (componentType.represents(boolean.class)) {
                return ArrayCreator.Primitive.BOOLEAN;
            } else if (componentType.represents(byte.class)) {
                return ArrayCreator.Primitive.BYTE;
            } else if (componentType.represents(short.class)) {
                return ArrayCreator.Primitive.SHORT;
            } else if (componentType.represents(char.class)) {
                return ArrayCreator.Primitive.CHARACTER;
            } else if (componentType.represents(int.class)) {
                return ArrayCreator.Primitive.INTEGER;
            } else if (componentType.represents(long.class)) {
                return ArrayCreator.Primitive.LONG;
            } else if (componentType.represents(float.class)) {
                return ArrayCreator.Primitive.FLOAT;
            } else if (componentType.represents(double.class)) {
                return ArrayCreator.Primitive.DOUBLE;
            } else {
                throw new IllegalArgumentException("Cannot create array of type " + componentType);
            }
        } else {
            return new ArrayCreator.Reference(componentType);
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
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ArrayFactory that = (ArrayFactory) other;
        return arrayCreator.equals(that.arrayCreator)
                && componentType.equals(that.componentType)
                && sizeDecrease.equals(that.sizeDecrease);
    }

    @Override
    public int hashCode() {
        int result = componentType.hashCode();
        result = 31 * result + arrayCreator.hashCode();
        result = 31 * result + sizeDecrease.hashCode();
        return result;
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
         * The opcode instruction for storing a value of the component type inside an array.
         *
         * @return The correct storage opcode for the represented type.
         */
        int getStorageOpcode();

        /**
         * An array creator implementation for primitive types.
         */
        static enum Primitive implements ArrayCreator {

            BOOLEAN(Opcodes.T_BOOLEAN, Opcodes.BASTORE),
            BYTE(Opcodes.T_BYTE, Opcodes.BASTORE),
            SHORT(Opcodes.T_SHORT, Opcodes.SASTORE),
            CHARACTER(Opcodes.T_CHAR, Opcodes.CASTORE),
            INTEGER(Opcodes.T_INT, Opcodes.IASTORE),
            LONG(Opcodes.T_LONG, Opcodes.LASTORE),
            FLOAT(Opcodes.T_FLOAT, Opcodes.FASTORE),
            DOUBLE(Opcodes.T_DOUBLE, Opcodes.DASTORE);

            private final int creationOpcode;
            private final int storageOpcode;

            private Primitive(int creationOpcode, int storageOpcode) {
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
                return SIZE;
            }

            @Override
            public int getStorageOpcode() {
                return storageOpcode;
            }
        }

        /**
         * An array creator implementation for reference types.
         */
        static class Reference implements ArrayCreator {

            private final String internalTypeName;

            private Reference(TypeDescription referenceType) {
                this.internalTypeName = referenceType.getInternalName();
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalTypeName);
                return SIZE;
            }

            @Override
            public int getStorageOpcode() {
                return Opcodes.AASTORE;
            }
        }
    }

    private class ArrayStackManipulation implements StackManipulation {

        private final List<StackManipulation> stackManipulations;

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
    }
}

