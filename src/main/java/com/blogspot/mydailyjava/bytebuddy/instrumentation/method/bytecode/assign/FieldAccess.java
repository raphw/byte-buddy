package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public enum FieldAccess {

    STATIC(Opcodes.PUTSTATIC, Opcodes.GETSTATIC),
    INSTANCE(Opcodes.PUTFIELD, Opcodes.GETFIELD);

    public static FieldAccess forStaticField(boolean isFieldStatic) {
        return isFieldStatic ? STATIC : INSTANCE;
    }

    private final int put;
    private final int get;

    private FieldAccess(int put, int get) {
        this.put = put;
        this.get = get;
    }

    public class Accessor {

        private final String fieldName;
        private final String fieldTypeDescriptor;
        private final TypeSize typeSize;
        private final String ownerTypeInternalName;

        protected Accessor(String fieldName, String fieldTypeDescriptor, TypeSize typeSize, String ownerTypeInternalName) {
            this.fieldName = fieldName;
            this.fieldTypeDescriptor = fieldTypeDescriptor;
            this.typeSize = typeSize;
            this.ownerTypeInternalName = ownerTypeInternalName;
        }

        private class Getter implements Assignment {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor) {
                methodVisitor.visitFieldInsn(get, ownerTypeInternalName, fieldName, fieldTypeDescriptor);
                return typeSize.toIncreasingSize();
            }
        }

        public Assignment getterFor() {
            return new Getter();
        }

        private class Putter implements Assignment {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor) {
                methodVisitor.visitFieldInsn(put, ownerTypeInternalName, fieldName, fieldTypeDescriptor);
                return typeSize.toDecreasingSize();
            }
        }

        public Assignment putterFor() {
            return new Putter();
        }
    }

    public Accessor accessor(String fieldName, Class<?> fieldType, String ownerTypeInternalName) {
        return new Accessor(fieldName, Type.getDescriptor(fieldType), TypeSize.of(fieldType), ownerTypeInternalName);
    }
}
