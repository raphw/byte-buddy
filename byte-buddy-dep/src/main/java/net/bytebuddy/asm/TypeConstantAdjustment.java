package net.bytebuddy.asm;

import net.bytebuddy.ClassFileVersion;
import org.objectweb.asm.*;

public enum TypeConstantAdjustment implements ClassVisitorWrapper {

    INSTANCE;

    @Override
    public int mergeWriter(int flags) {
        return flags | ClassWriter.COMPUTE_MAXS;
    }

    @Override
    public int mergeReader(int flags) {
        return flags;
    }

    @Override
    public ClassVisitor wrap(ClassVisitor classVisitor) {
        return new TypeConstantDissolvingClassVisitor(classVisitor);
    }

    @Override
    public String toString() {
        return "TypeConstantAdjustment." + name();
    }

    protected static class TypeConstantDissolvingClassVisitor extends ClassVisitor {

        private boolean armed;

        protected TypeConstantDissolvingClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor);
        }

        @Override
        public void visit(int version, int modifiers, String name, String signature, String superTypeName, String[] interfaceName) {
            armed = !ClassFileVersion.ofMinorMajor(modifiers).isAtLeast(ClassFileVersion.JAVA_V5);
            super.visit(version, modifiers, name, signature, superTypeName, interfaceName);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return armed
                    ? new TypeConstantDissolvingMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions))
                    : super.visitMethod(access, name, desc, signature, exceptions);
        }

        @Override
        public String toString() {
            return "TypeConstantAdjustment.TypeConstantDissolvingClassVisitor{" +
                    "classVisitor=" + cv +
                    ", armed=" + armed +
                    "}";
        }

        protected static class TypeConstantDissolvingMethodVisitor extends MethodVisitor {

            private static final String JAVA_LANG_CLASS = "java/lang/Class";

            private static final String FOR_NAME = "forName";

            private static final String DESCRIPTOR = "Ljava/lang/Class(Ljava/lang/Sring";

            protected TypeConstantDissolvingMethodVisitor(MethodVisitor methodVisitor) {
                super(Opcodes.ASM5, methodVisitor);
            }

            @Override
            public void visitLdcInsn(Object constant) {
                if (constant instanceof Type) {
                    Type type = (Type) constant;
                    switch (type.getSort()) {
                        case Type.OBJECT:
                        case Type.ARRAY:
                            super.visitLdcInsn(type.getClassName());
                            super.visitMethodInsn(Opcodes.ACC_STATIC, JAVA_LANG_CLASS, FOR_NAME, DESCRIPTOR, false);
                            break;
                    }
                }
                super.visitLdcInsn(constant);
            }

            @Override
            public String toString() {
                return "TypeConstantAdjustment.TypeConstantDissolvingClassVisitor.TypeConstantDissolvingMethodVisitor{methodVisitor=" + mv + "}";
            }
        }
    }
}
