package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

/**
 * This instrumentation creates a method stub which does nothing but returning the default value of the return
 * type of the method. These default values are:
 * <ol>
 * <li>The value {@code 0} for all numeric type.</li>
 * <li>The null character for the {@code char} type.</li>
 * <li>{@code false} for the {@code boolean} type.</li>
 * <li>Nothing for {@code void} types.</li>
 * <li>A {@code null} reference for any reference types. Note that this includes primitive wrapper types.</li>
 * </ol>
 */
public enum StubMethod implements Instrumentation, ByteCodeAppender {
    INSTANCE;

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(TypeDescription instrumentedType) {
        return this;
    }

    @Override
    public boolean appendsCode() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor,
                      Context instrumentationContext,
                      MethodDescription instrumentedMethod) {
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                DefaultValue.of(instrumentedMethod.getReturnType()),
                MethodReturn.returning(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, instrumentationContext);
        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }
}
