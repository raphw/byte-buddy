package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;

public class InvocationHandlerAdapter implements Instrumentation, TypeInitializer {

    private class Appender implements ByteCodeAppender {

        private final TypeDescription instrumentedType;

        private Appender(TypeDescription instrumentedType) {
            this.instrumentedType = instrumentedType;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
            TypeDescription objectType = new TypeDescription.ForLoadedType(Object.class);
            TypeDescription invocationHandlerType = new TypeDescription.ForLoadedType(InvocationHandler.class);
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    FieldAccess.forField(instrumentedType.getDeclaredFields().named(fieldName)).getter(),
                    MethodVariableAccess.forType(objectType).loadFromIndex(0),
                    MethodConstant.forMethod(instrumentedMethod),
                    ArrayFactory.targeting(objectType).withValues(argumentValuesOf(instrumentedMethod)),
                    MethodInvocation.invoke(invocationHandlerType.getDeclaredMethods().getOnly()),
                    assigner.assign(objectType, instrumentedMethod.getReturnType(), true),
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        private List<StackManipulation> argumentValuesOf(MethodDescription instrumentedMethod) {
            TypeList parameterTypes = instrumentedMethod.getParameterTypes();
            List<StackManipulation> instruction = new ArrayList<StackManipulation>(parameterTypes.size());
            int currentIndex = 1;
            for (TypeDescription parameterType : parameterTypes) {
                instruction.add(MethodVariableAccess.forType(parameterType).loadFromIndex(currentIndex));
                currentIndex += parameterType.getStackSize().getSize();
            }
            return instruction;
        }

        private InvocationHandlerAdapter getJavaProxyAdapter() {
            return InvocationHandlerAdapter.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && instrumentedType.equals(((Appender) other).instrumentedType)
                    && InvocationHandlerAdapter.this.equals(((Appender) other).getJavaProxyAdapter());
        }

        @Override
        public int hashCode() {
            return 31 * InvocationHandlerAdapter.this.hashCode() + instrumentedType.hashCode();
        }

        @Override
        public String toString() {
            return "InvocationHandlerAdapter.Appender{" +
                    "javaProxyAdapter=" + InvocationHandlerAdapter.this +
                    "instrumentedType=" + instrumentedType +
                    '}';
        }
    }

    private static final Object STATIC_FIELD = null;
    private static final String PREFIX = "invocationHandler";

    private final String fieldName;
    private final InvocationHandler invocationHandler;

    private final Assigner assigner;

    public InvocationHandlerAdapter(InvocationHandler invocationHandler) {
        this(invocationHandler, String.format("%s$%d", PREFIX, Math.abs(invocationHandler.hashCode())));
    }

    public InvocationHandlerAdapter(InvocationHandler invocationHandler, String fieldName) {
        this.invocationHandler = invocationHandler;
        this.fieldName = fieldName;
        this.assigner = new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), true);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType
                .withField(fieldName, new TypeDescription.ForLoadedType(InvocationHandler.class), Opcodes.ACC_STATIC)
                .withInitializer(this);
    }

    @Override
    public ByteCodeAppender appender(TypeDescription instrumentedType) {
        return new Appender(instrumentedType);
    }

    @Override
    public void onLoad(Class<?> type) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(STATIC_FIELD, invocationHandler);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot set static field " + fieldName + " on " + type, e);
        }
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && fieldName.equals(((InvocationHandlerAdapter) other).fieldName)
                && invocationHandler.equals(((InvocationHandlerAdapter) other).invocationHandler);
    }

    @Override
    public int hashCode() {
        return 31 * fieldName.hashCode() + invocationHandler.hashCode();
    }
}
