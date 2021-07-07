package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * A {@link net.bytebuddy.implementation.bytecode.collection.CollectionFactory} that is capable of
 * creating an array of a given type with any number of given values. 一个{@link net.bytebuddy.implementation.bytecode.collection.CollectionFactory}，它能够创建具有任意数量的给定值的给定类型的数组
 */
@HashCodeAndEqualsPlugin.Enhance
public class ArrayFactory implements CollectionFactory {

    /**
     * The component type of the array this array factory is creating.
     */
    private final TypeDescription.Generic componentType;

    /**
     * The array creator delegate that supplies suitable opcodes for the creation of an array and the storage of
     * values inside it. 数组创建者委托为数组的创建及其内部值的存储提供合适的操作码
     */
    private final ArrayCreator arrayCreator;

    /**
     * The decrease of stack size after each value storage operation.
     */
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
    private final StackManipulation.Size sizeDecrease;

    /**
     * Creates a new array factory with a given
     * {@link net.bytebuddy.implementation.bytecode.collection.ArrayFactory.ArrayCreator}
     * without inferring the type from the component type. Normally,
     * {@link net.bytebuddy.implementation.bytecode.collection.ArrayFactory#forType(net.bytebuddy.description.type.TypeDescription.Generic)}
     * should be used.
     *
     * @param componentType The component type of the array factory.
     * @param arrayCreator  The array creator responsible for providing the correct byte code instructions.
     */
    protected ArrayFactory(TypeDescription.Generic componentType, ArrayCreator arrayCreator) {
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
    public static ArrayFactory forType(TypeDescription.Generic componentType) {
        return new ArrayFactory(componentType, makeArrayCreatorFor(componentType));
    }

    /**
     * Creates a suitable array creator for the given component type.
     *
     * @param componentType The component type of the array to be created.
     * @return A suitable array creator.
     */
    private static ArrayCreator makeArrayCreatorFor(TypeDefinition componentType) {
        if (!componentType.isPrimitive()) {
            return new ArrayCreator.ForReferenceType(componentType.asErasure());
        } else if (componentType.represents(boolean.class)) {
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
    }

    @Override
    public StackManipulation withValues(List<? extends StackManipulation> stackManipulations) {
        return new ArrayStackManipulation(stackManipulations);
    }

    @Override
    public TypeDescription.Generic getComponentType() {
        return componentType;
    }

    /**
     * An array creator is responsible for providing correct byte code instructions for creating an array
     * and for storing values into it. 数组创建者负责提供正确的字节码指令，以创建数组并将值存储到其中
     */
    protected interface ArrayCreator extends StackManipulation {

        /**
         * The creation of an array consumes one slot on the operand stack and adds a new value to it.
         * Therefore, the operand stack's size is not altered. 数组的创建会占用操作数堆栈上的一个插槽，并为其添加一个新值。 因此，操作数堆栈的大小不会改变
         */
        StackManipulation.Size ARRAY_CREATION_SIZE_CHANGE = StackSize.ZERO.toDecreasingSize();

        /**
         * The opcode instruction for storing a value of the component type inside an array. 用于在数组内部存储组件类型的值的操作码指令
         *
         * @return The correct storage opcode for the represented type.
         */
        int getStorageOpcode();

        /**
         * An array creator implementation for primitive types. 原始类型的数组创建器实现
         */
        enum ForPrimitiveType implements ArrayCreator {

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
             * The opcode for creating an array of this type. 用于创建此类型的数组的操作码
             */
            private final int creationOpcode;

            /**
             * The opcode for storing a value in an array of this type. 用于在此类型的数组中存储值的操作码
             */
            private final int storageOpcode;

            /**
             * Creates a new primitive array creator.
             *
             * @param creationOpcode The opcode for creating an array of this type.
             * @param storageOpcode  The opcode for storing a value in an array of this type.
             */
            ForPrimitiveType(int creationOpcode, int storageOpcode) {
                this.creationOpcode = creationOpcode;
                this.storageOpcode = storageOpcode;
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                methodVisitor.visitIntInsn(Opcodes.NEWARRAY, creationOpcode);
                return ARRAY_CREATION_SIZE_CHANGE;
            }

            @Override
            public int getStorageOpcode() {
                return storageOpcode;
            }
        }

        /**
         * An array creator implementation for reference types. 引用类型的数组创建器实现
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForReferenceType implements ArrayCreator {

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
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalTypeName);
                return ARRAY_CREATION_SIZE_CHANGE;
            }

            @Override
            public int getStorageOpcode() {
                return Opcodes.AASTORE;
            }
        }
    }

    /**
     * A stack manipulation for creating an array as defined by the enclosing array factory. 用于创建由封闭数组工厂定义的数组的堆栈操作
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
    protected class ArrayStackManipulation implements StackManipulation {

        /**
         * A list of value load instructions that are to be stored inside the created array. 要存储在所创建数组中的值加载指令的列表
         */
        private final List<? extends StackManipulation> stackManipulations;

        /**
         * Creates a new array loading instruction.
         *
         * @param stackManipulations A list of value load instructions that are to be stored inside the created array.
         */
        protected ArrayStackManipulation(List<? extends StackManipulation> stackManipulations) {
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
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            Size size = IntegerConstant.forValue(stackManipulations.size()).apply(methodVisitor, implementationContext);
            // The array's construction does not alter the stack's size.
            size = size.aggregate(arrayCreator.apply(methodVisitor, implementationContext));
            int index = 0;
            for (StackManipulation stackManipulation : stackManipulations) {
                methodVisitor.visitInsn(Opcodes.DUP);
                size = size.aggregate(StackSize.SINGLE.toIncreasingSize());
                size = size.aggregate(IntegerConstant.forValue(index++).apply(methodVisitor, implementationContext));
                size = size.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
                methodVisitor.visitInsn(arrayCreator.getStorageOpcode());
                size = size.aggregate(sizeDecrease);
            }
            return size;
        }
    }
}

