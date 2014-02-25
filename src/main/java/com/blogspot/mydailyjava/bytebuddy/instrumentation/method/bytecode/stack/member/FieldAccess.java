package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum FieldAccess {

    STATIC(Opcodes.PUTSTATIC, Opcodes.GETSTATIC),
    INSTANCE(Opcodes.PUTFIELD, Opcodes.GETFIELD);

    public static interface Defined {

        StackManipulation getter();

        StackManipulation putter();
    }

    private class AccessDispatcher implements Defined {

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
                        fieldDescription.getFieldType().getDescriptor());
                return resolveSize(fieldDescription.getFieldType().getStackSize());
            }

            protected abstract int getOpcode();

            protected abstract Size resolveSize(StackSize stackSize);
        }

        private class FieldGetInstruction extends AbstractFieldInstruction {

            @Override
            protected int getOpcode() {
                return getterOpcode;
            }

            @Override
            protected Size resolveSize(StackSize stackSize) {
                return stackSize.toIncreasingSize();
            }
        }

        private class FieldPutInstruction extends AbstractFieldInstruction {

            @Override
            protected int getOpcode() {
                return putterOpcode;
            }

            @Override
            protected Size resolveSize(StackSize stackSize) {
                return stackSize.toDecreasingSize();
            }
        }

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
    }

    public static Defined forField(FieldDescription fieldDescription) {
        if (fieldDescription.isStatic()) {
            return STATIC.new AccessDispatcher(fieldDescription);
        } else {
            return INSTANCE.new AccessDispatcher(fieldDescription);
        }
    }

    private final int putterOpcode;
    private final int getterOpcode;

    private FieldAccess(int putterOpcode, int getterOpcode) {
        this.putterOpcode = putterOpcode;
        this.getterOpcode = getterOpcode;
    }
}
