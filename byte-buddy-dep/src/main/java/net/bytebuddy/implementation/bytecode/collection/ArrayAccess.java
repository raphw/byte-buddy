package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows accessing array values.
 */
public enum ArrayAccess {

    /**
     * Access for a {@code byte}- or {@code boolean}-typed array.
     */
    BYTE(Opcodes.BALOAD, Opcodes.BASTORE, StackSize.SINGLE),

    /**
     * Access for a {@code short}-typed array.
     */
    SHORT(Opcodes.SALOAD, Opcodes.SASTORE, StackSize.SINGLE),

    /**
     * Access for a {@code char}-typed array.
     */
    CHARACTER(Opcodes.CALOAD, Opcodes.CASTORE, StackSize.SINGLE),

    /**
     * Access for a {@code int}-typed array.
     */
    INTEGER(Opcodes.IALOAD, Opcodes.IASTORE, StackSize.SINGLE),

    /**
     * Access for a {@code long}-typed array.
     */
    LONG(Opcodes.LALOAD, Opcodes.LASTORE, StackSize.DOUBLE),

    /**
     * Access for a {@code float}-typed array.
     */
    FLOAT(Opcodes.FALOAD, Opcodes.FASTORE, StackSize.SINGLE),

    /**
     * Access for a {@code double}-typed array.
     */
    DOUBLE(Opcodes.DALOAD, Opcodes.DASTORE, StackSize.DOUBLE),

    /**
     * Access for a reference-typed array.
     */
    REFERENCE(Opcodes.AALOAD, Opcodes.AASTORE, StackSize.SINGLE);

    /**
     * The opcode used for loading a value.
     */
    private final int loadOpcode;

    /**
     * The opcode used for storing a value.
     */
    private final int storeOpcode;

    /**
     * The size of the array's component value.
     */
    private final StackSize stackSize;

    /**
     * Creates a new array access.
     *
     * @param loadOpcode  The opcode used for loading a value.
     * @param storeOpcode The opcode used for storing a value.
     * @param stackSize   The size of the array's component value.
     */
    ArrayAccess(int loadOpcode, int storeOpcode, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.storeOpcode = storeOpcode;
        this.stackSize = stackSize;
    }

    /**
     * Locates an array accessor by the array's component type.
     *
     * @param componentType The array's component type.
     * @return An array accessor for the given type.
     */
    public static ArrayAccess of(TypeDefinition componentType) {
        if (componentType.represents(boolean.class) || componentType.represents(byte.class)) {
            return BYTE;
        } else if (componentType.represents(short.class)) {
            return SHORT;
        } else if (componentType.represents(char.class)) {
            return CHARACTER;
        } else if (componentType.represents(int.class)) {
            return INTEGER;
        } else if (componentType.represents(long.class)) {
            return LONG;
        } else if (componentType.represents(float.class)) {
            return FLOAT;
        } else if (componentType.represents(double.class)) {
            return DOUBLE;
        } else if (componentType.represents(void.class)) {
            throw new IllegalArgumentException("void is no legal array type");
        } else {
            return REFERENCE;
        }
    }

    /**
     * Creates a value-loading stack manipulation.
     *
     * @return A value-loading stack manipulation.
     */
    public StackManipulation load() {
        return new Loader();
    }

    /**
     * Creates a value-storing stack manipulation.
     *
     * @return A value-storing stack manipulation.
     */
    public StackManipulation store() {
        return new Putter();
    }

    /**
     * Applies a stack manipulation to the values of an array. The array must have at least as many values as the list has elements.
     *
     * @param processInstructions The elements to apply.
     * @return A stack manipulation that applies the supplied instructions.
     */
    public StackManipulation forEach(List<? extends StackManipulation> processInstructions) {
        List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(processInstructions.size());
        int index = 0;
        for (StackManipulation processInstruction : processInstructions) {
            stackManipulations.add(new StackManipulation.Compound(
                    Duplication.SINGLE,
                    IntegerConstant.forValue(index++),
                    new Loader(),
                    processInstruction
            ));
        }
        return new StackManipulation.Compound(stackManipulations);
    }

    /**
     * A stack manipulation for loading an array's value.
     */
    protected class Loader implements StackManipulation {

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitInsn(loadOpcode);
            return stackSize.toIncreasingSize().aggregate(new Size(-2, 0));
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private ArrayAccess getArrayAccess() {
            return ArrayAccess.this;
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public int hashCode() {
            return ArrayAccess.this.hashCode();
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public boolean equals(Object other) {
            return this == other || (other != null && other.getClass() == getClass()
                    && getArrayAccess() == ((Loader) other).getArrayAccess());
        }
    }

    /**
     * A stack manipulation for storing an array's value.
     */
    protected class Putter implements StackManipulation {

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitInsn(storeOpcode);
            return stackSize.toDecreasingSize().aggregate(new Size(-2, 0));
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private ArrayAccess getArrayAccess() {
            return ArrayAccess.this;
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public int hashCode() {
            return ArrayAccess.this.hashCode();
        }

        @Override // HE: Remove when Lombok support for getOuter is added.
        public boolean equals(Object other) {
            return this == other || (other != null && other.getClass() == getClass()
                    && getArrayAccess() == ((Putter) other).getArrayAccess());
        }
    }
}
