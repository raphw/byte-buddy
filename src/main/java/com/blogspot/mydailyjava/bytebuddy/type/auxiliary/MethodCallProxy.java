package com.blogspot.mydailyjava.bytebuddy.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodCallProxy implements AuxiliaryClass {

    private static enum MethodImplementation {

        RUN("run", "()V", null, Opcodes.RETURN),
        CALL("call", "()Ljava/lang/Object;", new String[]{"java/lang/Exception"}, Opcodes.ARETURN);

        private static final int METHOD_MODIFIER = 0;

        private final String methodName;
        private final String methodDescriptor;
        private final String[] methodExceptionInternalName;
        private final int returnOpcode;

        MethodImplementation(String methodName,
                             String methodDescriptor,
                             String[] methodExceptionInternalName,
                             int returnOpcode) {
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.methodExceptionInternalName = methodExceptionInternalName;
            this.returnOpcode = returnOpcode;
        }

        public void implement(ClassVisitor classVisitor) {
            MethodVisitor methodVisitor = classVisitor.visitMethod(METHOD_MODIFIER,
                    methodName,
                    methodDescriptor,
                    null,
                    methodExceptionInternalName);
            methodVisitor.visitCode();
            // Field to stack loading goes here
            // Method call goes here
            methodVisitor.visitInsn(returnOpcode);
            methodVisitor.visitEnd();
        }
    }

    private static final int ASM_MANUAL = 0;

    private static final int CLASS_MODIFIER = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
    private static final String OBJECT_INTERNAL_NAME = "java/lang/Object";
    private static final String RUNNABLE_INTERNAL_NAME = "java/lang/Runnable";
    private static final String CALLABLE_INTERNAL_NAME = "java/util/concurrent/Callable";

    private static final String CONSTRUCTOR_INTERNAL_NAME = "<init>";
    private static final char VOID_TYPE_TOKEN = 'V';

    private final String internalMethodName;
    private final String descriptor;
    private final Class<?>[] parameterType;

    public MethodCallProxy(MethodDescription methodDescription) {
        internalMethodName = methodDescription.getInternalName();
        descriptor = methodDescription.getDescriptor();
        parameterType = methodDescription.getParameterTypes();
    }

    private class Named implements AuxiliaryClass.Named {

        private final String proxyInternalName;
        private final String constructorDescriptor;

        private final ClassVersion classVersion;

        private Named(String proxyInternalName, ClassVersion classVersion) {
            this.proxyInternalName = proxyInternalName;
            constructorDescriptor = descriptor.substring(0, descriptor.lastIndexOf(')')) + VOID_TYPE_TOKEN;
            this.classVersion = classVersion;
        }

        @Override
        public String getName() {
            return proxyInternalName;
        }

        @Override
        public byte[] make() {
            ClassWriter classWriter = new ClassWriter(ASM_MANUAL);
            classWriter.visit(classVersion.getVersionNumber(),
                    CLASS_MODIFIER,
                    proxyInternalName,
                    null,
                    OBJECT_INTERNAL_NAME,
                    new String[]{RUNNABLE_INTERNAL_NAME, CALLABLE_INTERNAL_NAME});
            // Add field for target object
            for(Class<?> type : parameterType) {
            // Adding of fields goes here
            }
            // Implementing of constructor goes and field setting goes here
            for(MethodImplementation methodImplementation : MethodImplementation.values()) {
                methodImplementation.implement(classWriter);
            }
            classWriter.visitEnd();
            return classWriter.toByteArray();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitTypeInsn(Opcodes.NEW, proxyInternalName);
            methodVisitor.visitInsn(Opcodes.DUP);
            // Argument of caller method loading goes here
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, proxyInternalName, CONSTRUCTOR_INTERNAL_NAME, constructorDescriptor);
            return new Size(1, -1 /* size of arguments */);
        }
    }

    @Override
    public Named name(String name, ClassVersion classVersion) {
        return new Named(name, classVersion);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && descriptor.equals(((MethodCallProxy) other).descriptor)
                && internalMethodName.equals(((MethodCallProxy) other).internalMethodName);
    }

    @Override
    public int hashCode() {
        return 31 * internalMethodName.hashCode() + descriptor.hashCode();
    }
}
