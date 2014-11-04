package net.bytebuddy.instrumentation.method.bytecode.stack.collection;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum ArrayAccess {

    BYTE(Opcodes.BALOAD, Opcodes.BASTORE, StackSize.SINGLE),

    SHORT(Opcodes.SALOAD, Opcodes.SASTORE, StackSize.SINGLE),

    CHARACTER(Opcodes.CALOAD, Opcodes.CASTORE, StackSize.SINGLE),

    INTEGER(Opcodes.IALOAD, Opcodes.IASTORE, StackSize.SINGLE),

    LONG(Opcodes.LALOAD, Opcodes.LASTORE, StackSize.DOUBLE),

    FLOAT(Opcodes.FALOAD, Opcodes.FASTORE, StackSize.SINGLE),

    DOUBLE(Opcodes.DALOAD, Opcodes.DASTORE, StackSize.DOUBLE),

    REFERENCE(Opcodes.AALOAD, Opcodes.AASTORE, StackSize.SINGLE);

    public static ArrayAccess of(TypeDescription typeDescription) {
        if (typeDescription.represents(boolean.class) || typeDescription.represents(short.class)) {
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
        } else if (typeDescription.represents(void.class)) {
            throw new IllegalArgumentException("The void type is no legal array type");
        } else {
            return REFERENCE;
        }
    }

    private final int loadOpcode;

    private final int storeOpcode;

    private final StackSize stackSize;

    private ArrayAccess(int loadOpcode, int storeOpcode, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.storeOpcode = storeOpcode;
        this.stackSize = stackSize;
    }

    public StackManipulation load() {
        return new Loader();
    }

    public StackManipulation store() {
        return new Putter();
    }

    private class Loader implements StackManipulation {

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitInsn(loadOpcode);
            return stackSize.toIncreasingSize().aggregate(new Size(-2, 0));
        }

        @Override
        public int hashCode() {
            return ArrayAccess.this.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other != null && other.getClass() == getClass()
                    && getArrayAccess() == ((Loader) other).getArrayAccess());
        }

        private ArrayAccess getArrayAccess() {
            return ArrayAccess.this;
        }

        @Override
        public String toString() {
            return "ArrayAccess.Loader{arrayAccess=" + ArrayAccess.this + '}';
        }
    }

    private class Putter implements StackManipulation {

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitInsn(storeOpcode);
            return stackSize.toDecreasingSize().aggregate(new Size(-2, 0));
        }

        @Override
        public int hashCode() {
            return ArrayAccess.this.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || (other != null && other.getClass() == getClass()
                    && getArrayAccess() == ((Putter) other).getArrayAccess());
        }

        private ArrayAccess getArrayAccess() {
            return ArrayAccess.this;
        }

        @Override
        public String toString() {
            return "ArrayAccess.Putter{arrayAccess=" + ArrayAccess.this + '}';
        }
    }
}
