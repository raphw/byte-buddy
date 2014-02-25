package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.collection;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.IntegerConstant;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class ArrayFactory {

    private static final StackManipulation.Size SIZE = StackSize.ZERO.toDecreasingSize();

    private static interface ArrayCreator extends StackManipulation {

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

        int getStorageOpcode();
    }



    public static ArrayFactory of(TypeDescription typeDescription) {
        if (!typeDescription.isArray()) {
            throw new IllegalArgumentException("Expected array type instead of " + typeDescription);
        }
        return new ArrayFactory(typeDescription.getComponentType(), makeCreatorFor(typeDescription.getComponentType()));
    }

    private static ArrayCreator makeCreatorFor(TypeDescription componentType) {
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

    private final TypeDescription componentType;
    private final ArrayCreator arrayCreator;
    private final StackManipulation.Size sizeDecrease;

    protected ArrayFactory(TypeDescription componentType, ArrayCreator arrayCreator) {
        this.componentType = componentType;
        this.arrayCreator = arrayCreator;
        // Size decreases by index and array reference (2) and array element (1, 2) after each element storage.
        sizeDecrease = StackSize.DOUBLE.toDecreasingSize().aggregate(componentType.getStackSize().toDecreasingSize());
    }

    public StackManipulation withValues(List<StackManipulation> stackManipulations) {
        return new ArrayStackManipulation(stackManipulations);
    }

    public TypeDescription getComponentType() {
        return componentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayFactory that = (ArrayFactory) o;
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
}

