package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

public interface Instrumentation {

    static interface Context {

        static class Default implements Context, AuxiliaryType.MethodProxyFactory {

            public static interface AuxiliaryTypeNamingStrategy {

                String name(AuxiliaryType auxiliaryType);
            }

            private final AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy;
            private final AuxiliaryType.MethodProxyFactory methodProxyFactory;
            private final Map<AuxiliaryType, DynamicType<?>> auxiliaryTypes;
            private final Map<MethodDescription, MethodDescription> registeredProxyMethods;

            public Default(AuxiliaryTypeNamingStrategy auxiliaryTypeNamingStrategy,
                           AuxiliaryType.MethodProxyFactory methodProxyFactory) {
                this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
                this.methodProxyFactory = methodProxyFactory;
                auxiliaryTypes = new HashMap<AuxiliaryType, DynamicType<?>>();
                registeredProxyMethods = new HashMap<MethodDescription, MethodDescription>();
            }

            @Override
            public String register(AuxiliaryType auxiliaryType) {
                DynamicType<?> dynamicType = auxiliaryTypes.get(auxiliaryType);
                if (dynamicType == null) {
                    dynamicType = auxiliaryType.make(auxiliaryTypeNamingStrategy.name(auxiliaryType), this);
                    auxiliaryTypes.put(auxiliaryType, dynamicType);
                }
                return dynamicType.getMainTypeName();
            }

            @Override
            public MethodDescription requireProxyMethodFor(MethodDescription targetMethod) {
                MethodDescription proxyMethod = registeredProxyMethods.get(targetMethod);
                if (proxyMethod == null) {
                    proxyMethod = methodProxyFactory.requireProxyMethodFor(targetMethod);
                    registeredProxyMethods.put(targetMethod, proxyMethod);
                }
                return proxyMethod;
            }

            @Override
            public List<DynamicType<?>> getRegisteredAuxiliaryTypes() {
                return Collections.unmodifiableList(new ArrayList<DynamicType<?>>(auxiliaryTypes.values()));
            }
        }

        String register(AuxiliaryType auxiliaryType);

        List<DynamicType<?>> getRegisteredAuxiliaryTypes();
    }

    static enum ForAbstractMethod implements Instrumentation, ByteCodeAppender {
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
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
            throw new IllegalStateException();
        }
    }

    static class Composite implements Instrumentation {

        private final Instrumentation[] instrumentation;

        public Composite(Instrumentation... instrumentation) {
            this.instrumentation = instrumentation;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            for (Instrumentation instrumentation : this.instrumentation) {
                instrumentedType = instrumentation.prepare(instrumentedType);
            }
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            ByteCodeAppender[] byteCodeAppender = new ByteCodeAppender[instrumentation.length];
            int index = 0;
            for (Instrumentation instrumentation : this.instrumentation) {
                byteCodeAppender[index++] = instrumentation.appender(instrumentedType);
            }
            return new ByteCodeAppender.Composite(byteCodeAppender);
        }
    }

    InstrumentedType prepare(InstrumentedType instrumentedType);

    ByteCodeAppender appender(TypeDescription instrumentedType);
}
