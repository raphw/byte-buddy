package com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary;

import com.blogspot.mydailyjava.bytebuddy.ByteBuddy;
import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.TargetType;
import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.DefaultValue;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.modifier.Ownership;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

public class TypeProxy implements AuxiliaryType {

    public static final String REFLECTION_METHOD = "make";
    private static final String INSTANCE_FIELD = "target";

    public static class ByConstructor implements StackManipulation {

        private final TypeDescription proxiedType;
        private final TypeDescription instrumentedType;
        private final List<TypeDescription> constructorParameters;
        private final boolean ignoreFinalizer;

        public ByConstructor(TypeDescription proxiedType,
                             TypeDescription instrumentedType,
                             List<TypeDescription> constructorParameters,
                             boolean ignoreFinalizer) {
            this.proxiedType = proxiedType;
            this.instrumentedType = instrumentedType;
            this.constructorParameters = constructorParameters;
            this.ignoreFinalizer = ignoreFinalizer;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription proxyType = instrumentationContext.register(new TypeProxy(proxiedType, instrumentedType, ignoreFinalizer));
            StackManipulation[] constructorValue = new StackManipulation[constructorParameters.size()];
            int index = 0;
            for (TypeDescription parameterType : constructorParameters) {
                constructorValue[index++] = DefaultValue.of(parameterType);
            }
            return new Compound(
                    TypeCreation.forType(proxyType),
                    Duplication.SINGLE,
                    new Compound(constructorValue),
                    MethodInvocation.invoke(proxyType.getDeclaredMethods()
                            .filter(isConstructor().and(takesArguments(constructorParameters))).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                    FieldAccess.forField(proxyType.getDeclaredFields().named(INSTANCE_FIELD)).putter()
            ).apply(methodVisitor, instrumentationContext);
        }
    }

    public static class ByReflectionFactory implements StackManipulation {

        private final TypeDescription proxiedType;
        private final TypeDescription instrumentedType;
        private final boolean ignoreFinalizer;

        public ByReflectionFactory(TypeDescription proxiedType, TypeDescription instrumentedType, boolean ignoreFinalizer) {
            this.proxiedType = proxiedType;
            this.instrumentedType = instrumentedType;
            this.ignoreFinalizer = ignoreFinalizer;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            TypeDescription proxyType = instrumentationContext.register(new TypeProxy(proxiedType, instrumentedType, ignoreFinalizer));
            return new Compound(
                    MethodInvocation.invoke(proxyType.getDeclaredMethods()
                            .filter(named(REFLECTION_METHOD).and(takesArguments(0))).getOnly()),
                    Duplication.SINGLE,
                    MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                    FieldAccess.forField(proxyType.getDeclaredFields().named(INSTANCE_FIELD)).putter()
            ).apply(methodVisitor, instrumentationContext);
        }
    }

    private class MethodCall implements Instrumentation {

        private class Appender implements ByteCodeAppender {

            private final StackManipulation fieldLoadingInstruction;

            public Appender(TypeDescription instrumentedType) {
                fieldLoadingInstruction = FieldAccess.forField(instrumentedType.getDeclaredFields().named(INSTANCE_FIELD)).getter();
            }

            @Override
            public boolean appendsCode() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor,
                              Context instrumentationContext,
                              MethodDescription instrumentedMethod) {
                MethodDescription proxyMethod = methodAccessorFactory.requireAccessorMethodFor(instrumentedMethod);
                StackManipulation.Size stackSize = new StackManipulation.Compound(
                        MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                        fieldLoadingInstruction,
                        MethodVariableAccess.loadArguments(instrumentedMethod),
                        MethodInvocation.invoke(proxyMethod),
                        MethodReturn.returning(instrumentedMethod.getReturnType())
                ).apply(methodVisitor, instrumentationContext);
                return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }

        private final MethodAccessorFactory methodAccessorFactory;

        private MethodCall(MethodAccessorFactory methodAccessorFactory) {
            this.methodAccessorFactory = methodAccessorFactory;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(INSTANCE_FIELD, TypeProxy.this.instrumentedType, Opcodes.ACC_SYNTHETIC);
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(instrumentedType);
        }
    }

    private class SilentConstruction implements Instrumentation {

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
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "sun/reflect/ReflectionFactory",
                        "getReflectionFactory",
                        "()Lsun/reflect/ReflectionFactory;");
                methodVisitor.visitLdcInsn(Type.getType(instrumentedType.getDescriptor()));
                methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class",
                        "getDeclaredConstructor",
                        "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "sun/reflect/ReflectionFactory",
                        "newConstructorForSerialization",
                        "(Ljava/lang/Class;Ljava/lang/reflect/Constructor;)Ljava/lang/reflect/Constructor;");
                methodVisitor.visitInsn(Opcodes.ICONST_0);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Constructor",
                        "newInstance",
                        "([Ljava/lang/Object;)Ljava/lang/Object;");
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, instrumentedType.getInternalName());
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return new Size(4, 0);
            }
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(TypeDescription instrumentedType) {
            return new Appender(instrumentedType);
        }
    }

    private final TypeDescription proxiedType;
    private final TypeDescription instrumentedType;
    private final boolean ignoreFinalizer;

    public TypeProxy(TypeDescription proxiedType, TypeDescription instrumentedType, boolean ignoreFinalizer) {
        this.proxiedType = proxiedType;
        this.instrumentedType = instrumentedType;
        this.ignoreFinalizer = ignoreFinalizer;
    }

    @Override
    public DynamicType make(String auxiliaryTypeName,
                            ClassFormatVersion classFormatVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        MethodMatcher methodMatcher = ignoreFinalizer ? not(isFinalizer()) : any();
        return new ByteBuddy(classFormatVersion)
                .subclass(proxiedType, ConstructorStrategy.Default.IMITATE_SUPER_TYPE)
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER.toArray(new ModifierContributor.ForType[DEFAULT_TYPE_MODIFIER.size()]))
                .method(methodMatcher).intercept(new MethodCall(methodAccessorFactory))
                .defineMethod(REFLECTION_METHOD, TargetType.DESCRIPTION, Collections.<TypeDescription>emptyList(), Ownership.STATIC)
                .intercept(new SilentConstruction())
                .make();
    }
}
