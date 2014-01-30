package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MethodCallProxy implements AuxiliaryClass {

    private static final String RUNNABLE_TYPE_NAME = "java/lang/Runnable";
    private static final String CALLABLE_TYPE_NAME = "java/util/concurrent/Callable";

    private static final String FIELD_NAME_PREFIX = "arg";

    private final Map<String, String> methodArgumentFields;
    private final String proxyConstructorDescriptor;
    private final Assignment.Size stackedSize;
    private final TypeSize maximalSize;

    private final String proxiedMethodInternalName;
    private final String proxiedMethodDescriptor;
    private final String proxiedTypeInternalName;

    public MethodCallProxy(MethodDescription methodDescription) {
        Map<String, String> fieldTypes = new HashMap<String, String>(1 + methodDescription.getParameterTypes().size());
        int i = 0;
        TypeSize maximalSize = TypeSize.ZERO;
        Assignment.Size stackedSize = TypeSize.ZERO.toIncreasingSize();
        StringBuilder proxyConstructorDescriptor = new StringBuilder("(");
        if (!methodDescription.isStatic()) {
            fieldTypes.put(FIELD_NAME_PREFIX + i, Type.getDescriptor(Object.class));
            proxyConstructorDescriptor.append(Type.getDescriptor(Object.class));
            TypeSize argumentSize = TypeSize.SINGLE;
            stackedSize = stackedSize.aggregate(argumentSize.toIncreasingSize());
            maximalSize = maximalSize.maximum(argumentSize);
        }
        for (TypeDescription parameterType : methodDescription.getParameterTypes()) {
            fieldTypes.put(FIELD_NAME_PREFIX + i, parameterType.getDescriptor());
            proxyConstructorDescriptor.append(parameterType.getDescriptor());
            TypeSize argumentSize = parameterType.getStackSize();
            stackedSize = stackedSize.aggregate(argumentSize.toIncreasingSize());
            maximalSize = maximalSize.maximum(argumentSize);
        }
        this.methodArgumentFields = Collections.unmodifiableMap(fieldTypes);
        this.proxyConstructorDescriptor = proxyConstructorDescriptor.append(")V").toString();
        this.stackedSize = stackedSize;
        this.maximalSize = maximalSize;
        proxiedMethodInternalName = methodDescription.getInternalName();
        proxiedMethodDescriptor = methodDescription.getDescriptor();
        proxiedTypeInternalName = methodDescription.getDeclaringType().getInternalName();
    }

    private class Named implements AuxiliaryClass.Named {

        private static final int PROXY_TYPE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;
        private static final int PROXY_FIELD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
        private static final int PROXY_METHOD_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;

        private static final String CONSTRUCTOR_NAME = "<init>";
        private static final String DEFAULT_CONSTRUCTOR_DESCRIPTOR = "()V";

        private final String proxyTypeInternalName;
        private final ClassVersion classVersion;

        private Named(String proxyTypeInternalName, ClassVersion classVersion) {
            this.proxyTypeInternalName = proxyTypeInternalName;
            this.classVersion = classVersion;
        }

        @Override
        public String getProxyTypeInternalName() {
            return proxyTypeInternalName;
        }

        @Override
        public byte[] make() {
            ClassWriter classWriter = new ClassWriter(0);
            classWriter.visit(classVersion.getVersionNumber(),
                    PROXY_TYPE_ACCESS,
                    proxyTypeInternalName,
                    null,
                    Type.getInternalName(Object.class),
                    new String[]{RUNNABLE_TYPE_NAME, CALLABLE_TYPE_NAME});
            for (Map.Entry<String, String> entry : methodArgumentFields.entrySet()) {
                classWriter.visitField(PROXY_FIELD_ACCESS, entry.getKey(), entry.getValue(), null, null).visitEnd();
            }
            MethodVisitor constructor = classWriter.visitMethod(PROXY_METHOD_ACCESS,
                    CONSTRUCTOR_NAME,
                    proxyConstructorDescriptor,
                    null,
                    null);
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    Type.getInternalName(Object.class),
                    CONSTRUCTOR_NAME,
                    DEFAULT_CONSTRUCTOR_DESCRIPTOR);
            for (Map.Entry<String, String> entry : methodArgumentFields.entrySet()) {
                constructor.visitVarInsn(Opcodes.ALOAD, 0);
                constructor.visitFieldInsn(Opcodes.PUTFIELD, proxyTypeInternalName, entry.getKey(), entry.getValue());
            }
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(1 + maximalSize.getSize(), 1 + stackedSize.getMaximalSize());
            constructor.visitEnd();
            // Implement both methods
            MethodVisitor method = classWriter.visitMethod(PROXY_METHOD_ACCESS,
                    "run",
                    "()V",
                    null,
                    null);
            method.visitCode();
            for (Map.Entry<String, String> entry : methodArgumentFields.entrySet()) {
                method.visitVarInsn(Opcodes.ALOAD, 0);
                method.visitFieldInsn(Opcodes.GETFIELD, proxyTypeInternalName, entry.getKey(), entry.getValue());
            }
            method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    proxiedTypeInternalName,
                    proxiedMethodInternalName,
                    proxiedMethodDescriptor);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(stackedSize.getMaximalSize(), 1);
            method.visitEnd();
            classWriter.visitEnd();
            return classWriter.toByteArray();
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            return null;
        }
    }

    @Override
    public Named name(String proxyTypeName, ClassVersion classVersion) {
        return new Named(toInternalName(proxyTypeName), classVersion);
    }

    private static String toInternalName(String name) {
        return name.replace('.', '/');
    }
}
