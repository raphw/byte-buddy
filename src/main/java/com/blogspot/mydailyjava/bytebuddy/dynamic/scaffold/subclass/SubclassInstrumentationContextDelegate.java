package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodArgument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;

public class SubclassInstrumentationContextDelegate
        implements AuxiliaryType.MethodProxyFactory,
        Instrumentation.Context.Default.AuxiliaryTypeNamingStrategy, MethodRegistry.Compiled {

    private static final String PREFIX = "delegate";

    private InstrumentedType instrumentedType;

    private final Random random;
    private final List<MethodDescription> proxyMethods;
    private final Map<MethodDescription, MethodDescription> targetMethodToProxyMethod;
    private final Map<MethodDescription, MethodDescription> proxyMethodToTargetMethod;

    public SubclassInstrumentationContextDelegate(InstrumentedType instrumentedType) {
        this.instrumentedType = instrumentedType;
        this.random = new Random();
        proxyMethods = new ArrayList<MethodDescription>();
        targetMethodToProxyMethod = new HashMap<MethodDescription, MethodDescription>();
        proxyMethodToTargetMethod = new HashMap<MethodDescription, MethodDescription>();
    }

    @Override
    public String name(AuxiliaryType auxiliaryType) {
        return String.format("%s$%s$%d", instrumentedType.getName(), PREFIX, Math.abs(random.nextInt()));
    }

    @Override
    public MethodDescription requireProxyMethodFor(MethodDescription targetMethod) {
        String name = String.format("%s$%s$%d", targetMethod.getInternalName(), PREFIX, Math.abs(random.nextInt()));
        instrumentedType = instrumentedType.withMethod(name,
                targetMethod.getReturnType(),
                targetMethod.getParameterTypes(),
                (targetMethod.isStatic() ? Opcodes.ACC_STATIC : 0) | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL);
        MethodDescription proxyMethod = instrumentedType.getDeclaredMethods().filter(named(name)).getOnly();
        if (targetMethodToProxyMethod.put(targetMethod, proxyMethod) != null) {
            proxyMethodToTargetMethod.put(proxyMethod, targetMethod);
            proxyMethods.add(proxyMethod);
        }
        return proxyMethod;
    }

    public Iterable<MethodDescription> getProxiedMethods() {
        return new TypeWriter.SameThreadCoModifiableIterable<MethodDescription>(proxyMethods);
    }

    private class MethodCall implements Entry, ByteCodeAppender {

        private final MethodDescription targetDescription;

        private MethodCall(MethodDescription targetDescription) {
            this.targetDescription = targetDescription;
        }

        @Override
        public ByteCodeAppender getByteCodeAppender() {
            return this;
        }

        @Override
        public boolean isDefineMethod() {
            return true;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            StackManipulation.Size size = MethodArgument.loadParameters(instrumentedMethod)
                    .apply(methodVisitor, instrumentationContext);
            size = size.aggregate(MethodInvocation.invoke(targetDescription)
                    .special(instrumentedType.getSupertype())
                    .apply(methodVisitor, instrumentationContext));
            MethodReturn.returning(instrumentedMethod.getReturnType()).apply(methodVisitor, instrumentationContext);
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public MethodAttributeAppender getAttributeAppender() {
            return MethodAttributeAppender.NoOp.INSTANCE;
        }
    }

    @Override
    public Entry target(MethodDescription methodDescription) {
        return new MethodCall(proxyMethodToTargetMethod.get(methodDescription));
    }
}
