package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class MethodCallProxy0 implements AuxiliaryType {

    private static final String OBJECT_INTERNAL_NAME = Type.getInternalName(Object.class);
    private static final int ASM_MANUAL = 0;
    private static final Object ASM_IGNORE = null;
    private static final int CONSTRUCTOR_ACCESS = Opcodes.ACC_PUBLIC;
    private static final int FIELD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL;
    private static final String FIELD_NAME_PREFIX = "arg";
    private static final String DEFAULT_CONSTRUCTOR_DESCRIPTOR = "()V";

    public static class AssignableSignatureCall implements StackManipulation {

        private final MethodDescription proxiedMethod;

        public AssignableSignatureCall(MethodDescription proxiedMethod) {
            this.proxiedMethod = proxiedMethod;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            String typeName = instrumentationContext.register(new MethodCallProxy0(proxiedMethod));
            methodVisitor.visitTypeInsn(Opcodes.NEW, typeName);
            methodVisitor.visitInsn(Opcodes.DUP);
            Size size = new Size(2, 2);
            size = size.aggregate(MethodVariableAccess.loadAll(proxiedMethod).apply(methodVisitor, instrumentationContext));
            StringBuilder stringBuilder = new StringBuilder("(");
            if (!proxiedMethod.isStatic()) {
                stringBuilder.append(proxiedMethod.getDeclaringType().getDescriptor());
            }
            for (TypeDescription parameterType : proxiedMethod.getParameterTypes()) {
                stringBuilder.append(parameterType.getDescriptor());
            }
            stringBuilder.append(")V");
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, typeName, MethodDescription.CONSTRUCTOR_INTERNAL_NAME, stringBuilder.toString());
            return new Size(1, size.getMaximalSize());
        }
    }

    private static enum Interface {

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

    private final MethodDescription proxiedMethod;

    public MethodCallProxy0(MethodDescription proxiedMethod) {
        this.proxiedMethod = proxiedMethod;
    }

    @Override
    public DynamicType<?> make(String auxiliaryTypeName, ClassFormatVersion classFormatVersion, MethodAccessorFactory methodAccessorFactory) {
        String proxyTypeInternalName = auxiliaryTypeName.replace('.', '/');
        MethodDescription proxiedMethod = methodAccessorFactory.requireAccessorMethodFor(this.proxiedMethod);
        Map<String, TypeDescription> fields = new LinkedHashMap<String, TypeDescription>(1 + proxiedMethod.getParameterTypes().size());
        StringBuilder constructorDescriptor = new StringBuilder("(");
        int i = 0;
        if (!proxiedMethod.isStatic()) {
            fields.put(FIELD_NAME_PREFIX + i, proxiedMethod.getDeclaringType());
            constructorDescriptor.append(proxiedMethod.getDeclaringType().getDescriptor());
        }
        for (TypeDescription parameterType : proxiedMethod.getParameterTypes()) {
            fields.put(FIELD_NAME_PREFIX + i, parameterType);
            constructorDescriptor.append(parameterType.getDescriptor());
        }
        constructorDescriptor.append(")V");
        Assigner assigner = new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), true);
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL);
        ClassVisitor classVisitor = new TraceClassVisitor(classWriter, new PrintWriter(System.out));
        classVisitor.visit(Opcodes.V1_5,
                DEFAULT_TYPE_ACCESS,
                proxyTypeInternalName,
                null,
                OBJECT_INTERNAL_NAME,
                Interface.getInternalNames());
        for (Map.Entry<String, TypeDescription> field : fields.entrySet()) {
            classVisitor.visitField(FIELD_ACCESS,
                    field.getKey(),
                    field.getValue().getDescriptor(),
                    null,
                    ASM_IGNORE).visitEnd();
        }
        MethodVisitor constructor = classVisitor.visitMethod(CONSTRUCTOR_ACCESS,
                MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                constructorDescriptor.toString(),
                null,
                null);
        constructor.visitCode();
        int argumentIndex = 1;
        constructor.visitIntInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                OBJECT_INTERNAL_NAME,
                MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                DEFAULT_CONSTRUCTOR_DESCRIPTOR);
        int currentMaximum = 1;
        for (Map.Entry<String, TypeDescription> field : fields.entrySet()) {
            constructor.visitIntInsn(Opcodes.ALOAD, 0);
            MethodVariableAccess.forType(field.getValue()).loadFromIndex(argumentIndex).apply(constructor, null);
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
            anInterface.implement(classVisitor, proxyTypeInternalName, proxiedMethod, fields, assigner);
        }
        classVisitor.visitEnd();
        return new DynamicType.Default.Unloaded<Object>(auxiliaryTypeName,
                classWriter.toByteArray(),
                TypeInitializer.NoOp.INSTANCE,
                Collections.<DynamicType<?>>emptyList());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && proxiedMethod.equals(((MethodCallProxy0) other).proxiedMethod);
    }

    @Override
    public int hashCode() {
        return proxiedMethod.hashCode();
    }
}
