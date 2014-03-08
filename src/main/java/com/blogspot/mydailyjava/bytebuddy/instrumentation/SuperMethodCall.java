package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.hasSameSignatureAs;

public enum SuperMethodCall implements Instrumentation {
    INSTANCE;

    private static class SuperMethodCallAppender implements ByteCodeAppender {

        private final TypeDescription targetType;

        private SuperMethodCallAppender(TypeDescription targetType) {
            this.targetType = targetType;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            MethodDescription targetMethod;
            if (instrumentedMethod.isConstructor()) {
                MethodList methodList = targetType.getDeclaredMethods().filter(hasSameSignatureAs(instrumentedMethod));
                if (methodList.size() == 0) {
                    throw new IllegalArgumentException("There is no super constructor resembling " + instrumentedMethod);
                }
                targetMethod = methodList.getOnly();
            } else {
                targetMethod = instrumentedMethod;
            }
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    MethodVariableAccess.loadAll(instrumentedMethod),
                    MethodInvocation.invoke(targetMethod).special(targetType),
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(TypeDescription instrumentedType) {
        if (instrumentedType.getSupertype() == null) {
            throw new IllegalArgumentException("The object type does not have a super type");
        }
        return new SuperMethodCallAppender(instrumentedType.getSupertype());
    }
}
