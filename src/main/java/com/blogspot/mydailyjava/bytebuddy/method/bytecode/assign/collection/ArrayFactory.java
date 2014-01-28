package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.collection;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IntegerConstant;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

public class ArrayFactory {

    private static final Assignment.Size SIZE = TypeSize.ZERO.toDecreasingSize();

    private static interface ArrayCreator extends Assignment {

        int getStorageOpcode();
    }

    private static enum PrimitiveArrayCreator implements ArrayCreator {

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

        private PrimitiveArrayCreator(int creationOpcode, int storageOpcode) {
            this.creationOpcode = creationOpcode;
            this.storageOpcode = storageOpcode;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitIntInsn(Opcodes.NEWARRAY, creationOpcode);
            return SIZE;
        }


        @Override
        public int getStorageOpcode() {
            return storageOpcode;
        }
    }

    private static class ReferenceTypeArrayCreator implements ArrayCreator {

        private final String internalTypeName;

        private ReferenceTypeArrayCreator(Class<?> referenceType) {
            this.internalTypeName = Type.getInternalName(referenceType);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalTypeName);
            return SIZE;
        }

        @Override
        public int getStorageOpcode() {
            return Opcodes.AASTORE;
        }
    }

    public static ArrayFactory of(Class<?> type) {
        if (!type.isArray()) {
            throw new IllegalArgumentException("Expected array type instead of " + type);
        }
        return new ArrayFactory(type.getComponentType(), makeCreatorFor(type.getComponentType()));
    }

    private static ArrayCreator makeCreatorFor(Class<?> componentType) {
        if (componentType.isPrimitive()) {
            if (componentType == boolean.class) {
                return PrimitiveArrayCreator.BOOLEAN;
            } else if (componentType == byte.class) {
                return PrimitiveArrayCreator.BYTE;
            } else if (componentType == short.class) {
                return PrimitiveArrayCreator.SHORT;
            } else if (componentType == char.class) {
                return PrimitiveArrayCreator.CHARACTER;
            } else if (componentType == int.class) {
                return PrimitiveArrayCreator.INTEGER;
            } else if (componentType == long.class) {
                return PrimitiveArrayCreator.LONG;
            } else if (componentType == float.class) {
                return PrimitiveArrayCreator.FLOAT;
            } else if (componentType == double.class) {
                return PrimitiveArrayCreator.DOUBLE;
            } else {
                throw new IllegalArgumentException("Cannot create array of type " + componentType);
            }
        } else {
            return new ReferenceTypeArrayCreator(componentType);
        }
    }

    private class ArrayAssignment implements Assignment {

        private final List<Assignment> assignments;

        public ArrayAssignment(List<Assignment> assignments) {
            this.assignments = assignments;
        }

        @Override
        public boolean isValid() {
            for (Assignment assignment : assignments) {
                if (!assignment.isValid()) {
                    return false;
                }
            }
            return arrayCreator.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            Size size = IntegerConstant.forValue(assignments.size()).apply(methodVisitor);
            // The array's construction does not alter the stack's size.
            size = size.aggregate(arrayCreator.apply(methodVisitor));
            int index = 0;
            for (Assignment assignment : assignments) {
                methodVisitor.visitInsn(Opcodes.DUP);
                size = size.aggregate(TypeSize.SINGLE.toIncreasingSize());
                size = size.aggregate(IntegerConstant.forValue(index++).apply(methodVisitor));
                size = size.aggregate(assignment.apply(methodVisitor));
                methodVisitor.visitInsn(arrayCreator.getStorageOpcode());
                size = size.aggregate(sizeDecrease);
            }
            return size;
        }
    }

    private final Class<?> componentType;
    private final ArrayCreator arrayCreator;
    private final Assignment.Size sizeDecrease;

    protected ArrayFactory(Class<?> componentType, ArrayCreator arrayCreator) {
        this.componentType = componentType;
        this.arrayCreator = arrayCreator;
        // Size decreases by index and array reference (2) and array element (1, 2) after each element storage.
        sizeDecrease = TypeSize.DOUBLE.toDecreasingSize().aggregate(TypeSize.of(componentType).toDecreasingSize());
    }

    public Assignment withValues(List<Assignment> assignments) {
        return new ArrayAssignment(assignments);
    }

    public Class<?> getComponentType() {
        return componentType;
    }
}

