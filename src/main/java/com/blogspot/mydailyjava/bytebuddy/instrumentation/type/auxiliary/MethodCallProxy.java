package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassVersion;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class MethodCallProxy implements AuxiliaryClass {

    private static final String OBJECT_INTERNAL_NAME = Type.getInternalName(Object.class);

    private final MethodDescription proxiedMethod;

    public MethodCallProxy(MethodDescription proxiedMethod) {
        this.proxiedMethod = proxiedMethod;
    }

    private enum Interface {

        RUNNABLE(Runnable.class, "run", "()V", void.class, Opcodes.RETURN),
        CALLABLE(Callable.class, "call", "()Ljava/lang/Object;", Object.class, Opcodes.ARETURN);

        private static final int METHOD_ACCESS = Opcodes.ACC_PUBLIC;

        public static String[] getInternalNames() {
            String[] internalNames = new String[Interface.values().length];
            int i = 0;
            for (Interface anInterface : Interface.values()) {
                internalNames[i++] = anInterface.interfaceInternalName;
            }
            return internalNames;
        }

        private final String interfaceInternalName;
        private final String methodName;
        private final String methodDescriptor;
        private final TypeDescription returnTypeDescription;
        private final int returnOpcode;

        private Interface(Class<?> type,
                          String methodName,
                          String methodDescriptor,
                          Class<?> returnType,
                          int returnOpcode) {
            interfaceInternalName = Type.getInternalName(type);
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
            this.returnTypeDescription = new TypeDescription.ForLoadedType(returnType);
            this.returnOpcode = returnOpcode;
        }

        public void implement(ClassVisitor classVisitor,
                              String proxyTypeInternalName,
                              MethodDescription proxiedMethod,
                              Map<String, TypeDescription> fields,
                              Assigner assigner) {
            MethodVisitor methodVisitor = classVisitor.visitMethod(METHOD_ACCESS,
                    methodName,
                    methodDescriptor,
                    null,
                    null);
            methodVisitor.visitCode();
            StackManipulation.Size size = StackSize.ZERO.toIncreasingSize();
            for (Map.Entry<String, TypeDescription> field : fields.entrySet()) {
                methodVisitor.visitIntInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD,
                        proxyTypeInternalName,
                        field.getKey(),
                        field.getValue().getDescriptor());
                // Field value will be at least as big as self reference and can be ignored.
                size = size.aggregate(field.getValue().getStackSize().toIncreasingSize());
            }
            size.aggregate(MethodInvocation.invoke(proxiedMethod).apply(methodVisitor, null));
            size = size.aggregate(assigner.assign(proxiedMethod.getReturnType(), returnTypeDescription, false).apply(methodVisitor, null));
            methodVisitor.visitInsn(returnOpcode); // Size change will always be decreasing and can therefore be ignored.
            methodVisitor.visitMaxs(size.getMaximalSize(), StackSize.SINGLE.getSize());
            methodVisitor.visitEnd();
        }
    }

    private class Named implements AuxiliaryClass.Named {

        private static final int ASM_MANUAL = 0;
        private static final int CONSTRUCTOR_ACCESS = Opcodes.ACC_PUBLIC;
        private static final int FIELD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL;
        private static final String FIELD_NAME_PREFIX = "arg";
        private static final String CONSTRUCTOR_INTERNAL_NAME = "<init>";
        private static final String DEFAULT_CONSTRUCTOR_DESCRIPTOR = "()V";

        private final String proxyTypeInternalName;
        private final ClassVersion classVersion;

        private final String constructorDescriptor;
        private final Map<String, TypeDescription> fields;

        private final Assigner assigner;

        private Named(String proxyTypeInternalName, ClassVersion classVersion) {
            this.proxyTypeInternalName = proxyTypeInternalName;
            this.classVersion = classVersion;
            int i = 0;
            Map<String, TypeDescription> fields = new LinkedHashMap<String, TypeDescription>(1 + proxiedMethod.getParameterTypes().size());
            StringBuilder constructorDescriptor = new StringBuilder("(");
            if (!proxiedMethod.isStatic()) {
                fields.put(FIELD_NAME_PREFIX + i, proxiedMethod.getDeclaringType());
                constructorDescriptor.append(proxiedMethod.getDeclaringType().getDescriptor());
            }
            for (TypeDescription parameterType : proxiedMethod.getParameterTypes()) {
                fields.put(FIELD_NAME_PREFIX + i, parameterType);
                constructorDescriptor.append(parameterType.getDescriptor());
            }
            this.constructorDescriptor = constructorDescriptor.append(")V").toString();
            this.fields = Collections.unmodifiableMap(fields);
            assigner = new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), true);
        }

        @Override
        public String getProxyTypeInternalName() {
            return proxyTypeInternalName;
        }

        @Override
        public byte[] make() {
            ClassWriter classWriter = new ClassWriter(ASM_MANUAL);
            classWriter.visit(classVersion.getVersionNumber(),
                    DEFAULT_TYPE_ACCESS,
                    proxyTypeInternalName,
                    null,
                    OBJECT_INTERNAL_NAME,
                    Interface.getInternalNames());
            for (Map.Entry<String, TypeDescription> field : fields.entrySet()) {
                classWriter.visitField(FIELD_ACCESS,
                        field.getKey(),
                        field.getValue().getDescriptor(),
                        null,
                        null).visitEnd();
            }
            MethodVisitor constructor = classWriter.visitMethod(CONSTRUCTOR_ACCESS,
                    CONSTRUCTOR_INTERNAL_NAME,
                    constructorDescriptor,
                    null,
                    null);
            constructor.visitCode();
            int argumentIndex = 1;
            constructor.visitIntInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    OBJECT_INTERNAL_NAME,
                    CONSTRUCTOR_INTERNAL_NAME,
                    DEFAULT_CONSTRUCTOR_DESCRIPTOR);
            int currentMaximum = 1;
            for (Map.Entry<String, TypeDescription> field : fields.entrySet()) {
                constructor.visitIntInsn(Opcodes.ALOAD, 0);
                MethodArgument.forType(field.getValue()).loadFromIndex(argumentIndex).apply(constructor, null);
                constructor.visitFieldInsn(Opcodes.PUTFIELD,
                        proxyTypeInternalName,
                        field.getKey(),
                        field.getValue().getDescriptor());
                currentMaximum = Math.max(currentMaximum, field.getValue().getStackSize().getSize() + 1);
                argumentIndex += field.getValue().getStackSize().getSize();
            }
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(currentMaximum, proxiedMethod.getStackSize() + 1);
            constructor.visitEnd();
            for (Interface anInterface : Interface.values()) {
                anInterface.implement(classWriter, proxyTypeInternalName, proxiedMethod, fields, assigner);
            }
            classWriter.visitEnd();
            return classWriter.toByteArray();
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            StackManipulation.Size size = StackSize.ZERO.toIncreasingSize();
            for (TypeDescription typeDescription : fields.values()) {
                size = size.aggregate(MethodArgument.forType(typeDescription).loadFromIndex(size.getSizeImpact()).apply(methodVisitor, instrumentationContext));
            }
            methodVisitor.visitTypeInsn(Opcodes.NEW, proxyTypeInternalName);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    proxyTypeInternalName,
                    CONSTRUCTOR_INTERNAL_NAME,
                    constructorDescriptor);
            return new Size(1, Math.max(1, size.getMaximalSize()));
        }
    }

    @Override
    public Named name(String proxyTypeName, ClassVersion classVersion) {
        return new Named(proxyTypeName.replace('.', '/'), classVersion);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && proxiedMethod.equals(((MethodCallProxy) other).proxiedMethod);
    }

    @Override
    public int hashCode() {
        return proxiedMethod.hashCode();
    }
}
