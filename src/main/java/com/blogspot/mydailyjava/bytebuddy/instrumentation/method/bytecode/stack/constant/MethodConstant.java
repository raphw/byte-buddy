package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class MethodConstant implements StackManipulation {

    public static final String GET_DECLARED_METHOD_DESCRIPTOR = "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;";
    public static final String GET_DECLARED_METHOD_NAME = "getDeclaredMethod";
    public static final String CLASS_TYPE_INTERNAL_NAME = "java/lang/Class";

    private final MethodDescription methodDescription;

    public MethodConstant(MethodDescription methodDescription) {
        this.methodDescription = methodDescription;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitLdcInsn(Type.getType(methodDescription.getDeclaringType().getDescriptor()));
        methodVisitor.visitLdcInsn(methodDescription.getInternalName());
        Size argumentSize = new Size(2, 2);
        argumentSize = argumentSize.aggregate(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Class.class))
                .withValues(typeConstantsFor(methodDescription.getParameterTypes()))
                .apply(methodVisitor, instrumentationContext));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                CLASS_TYPE_INTERNAL_NAME,
                GET_DECLARED_METHOD_NAME,
                GET_DECLARED_METHOD_DESCRIPTOR);
        return new Size(1, argumentSize.getMaximalSize());
    }

    private static List<StackManipulation> typeConstantsFor(TypeList parameterTypes) {
        List<StackManipulation> typeConstants = new ArrayList<StackManipulation>(parameterTypes.size());
        for (TypeDescription parameterType : parameterTypes) {
            typeConstants.add(new ClassConstant(parameterType));
        }
        return typeConstants;
    }
}
