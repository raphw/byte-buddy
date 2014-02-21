package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum SuperClassDelegation implements ByteCodeAppender.Factory {
    INSTANCE;

    private static class Appender implements ByteCodeAppender {

        private final TypeDescription proxyType;

        private Appender(TypeDescription proxyType) {
            this.proxyType = proxyType;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            StackManipulation.Size parameterSize = new StackManipulation.Size(1, 1);
            for (TypeDescription parameterType : instrumentedMethod.getParameterTypes()) {
                parameterSize = parameterSize.aggregate(MethodArgument.forType(parameterType)
                        .loadFromIndex(parameterSize.getSizeImpact()).apply(methodVisitor, instrumentationContext));
            }
            StackManipulation.Size size = parameterSize.aggregate(MethodInvocation.invoke(instrumentedMethod)
                    .special(proxyType.getSupertype()).apply(methodVisitor, instrumentationContext));
            size = size.aggregate(MethodReturn.returning(instrumentedMethod.getReturnType()).apply(methodVisitor, instrumentationContext));
            return new Size(size.getMaximalSize(), parameterSize.getMaximalSize());
        }
    }

    @Override
    public ByteCodeAppender make(TypeDescription typeDescription) {
        return new Appender(typeDescription);
    }
}
