package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.JavaInstance;
import org.objectweb.asm.MethodVisitor;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class LambdaCreator {

    private static final String LAMBDA_FACTORY = "get$Lambda";

    private static final String FIELD_PREFIX = "arg$";

    private static final String LAMBDA_TYPE_INFIX = "$$Lambda$";

    private static final Class<?> NOT_PREVIOUSLY_DEFINED = null;

    private static final AtomicInteger LAMBDA_NAME_COUNTER = new AtomicInteger();

    private final ByteBuddy byteBuddy;

    public LambdaCreator(ByteBuddy byteBuddy) {
        this.byteBuddy = byteBuddy;
    }

    public byte[] make(Object targetTypeLookup,
                       String lambdaMethodName,
                       Object factoryMethodType,
                       Object lambdaMethodType,
                       Object targetMethodHandle,
                       Object specializedLambdaMethodType,
                       boolean serializable,
                       List<Class<?>> markerInterfaces,
                       List<?> additionalBridges,
                       Collection<? extends ClassFileTransformer> classFileTransformers) throws Exception {
        JavaInstance.MethodType factoryMethod = JavaInstance.MethodType.of(factoryMethodType);
        JavaInstance.MethodType lambdaMethod = JavaInstance.MethodType.of(lambdaMethodType);
        JavaInstance.MethodHandle targetMethod = JavaInstance.MethodHandle.of(targetMethodHandle, targetTypeLookup);
        JavaInstance.MethodType specializedLambdaMethod = JavaInstance.MethodType.of(specializedLambdaMethodType);
        Class<?> targetType = JavaInstance.MethodHandle.lookupType(targetTypeLookup);
        String lambdaClassName = targetType.getName() + LAMBDA_TYPE_INFIX + LAMBDA_NAME_COUNTER.incrementAndGet();
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(factoryMethod.getReturnType(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .modifiers(SyntheticState.SYNTHETIC, TypeManifestation.FINAL, Visibility.PUBLIC)
                .implement(markerInterfaces)
                .name(lambdaClassName)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(factoryMethod.getParameterTypes())
                .intercept(new ConstructorImplementation())
                .method(named(lambdaMethodName)
                        .and(takesArguments(lambdaMethod.getParameterTypes()))
                        .and(returns(lambdaMethod.getReturnType())))
                .intercept(new LambdaMethodImplementation(targetMethod.asMethodDescription(), specializedLambdaMethod));
        int index = 0;
        for (TypeDescription capturedType : factoryMethod.getParameterTypes()) {
            builder = builder.defineField(FIELD_PREFIX + ++index, capturedType, Visibility.PRIVATE, FieldManifestation.FINAL);
        }
        if (!factoryMethod.getParameterTypes().isEmpty()) {
            builder = builder.defineMethod(LAMBDA_FACTORY, factoryMethod.getReturnType(), Visibility.PRIVATE, Ownership.STATIC)
                    .withParameters(factoryMethod.getParameterTypes())
                    .intercept(new FactoryImplementation());
        }
        if (serializable) {
            if (!markerInterfaces.contains(Serializable.class)) {
                builder = builder.implement(Serializable.class);
            }
            builder = builder.defineMethod("writeReplace", Object.class)
                    .intercept(new SerializationImplementation(new TypeDescription.ForLoadedType(targetType),
                            factoryMethod.getReturnType(),
                            lambdaMethodName,
                            lambdaMethod,
                            targetMethod,
                            JavaInstance.MethodType.of(specializedLambdaMethodType)));
        } else if (factoryMethod.getReturnType().isAssignableTo(Serializable.class)) {
            builder = builder.defineMethod("readObject", void.class, Visibility.PRIVATE, MethodManifestation.FINAL)
                    .withParameters(ObjectInputStream.class)
                    .throwing(NotSerializableException.class)
                    .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"))
                    .defineMethod("writeObject", void.class, Visibility.PRIVATE, MethodManifestation.FINAL)
                    .withParameters(ObjectOutputStream.class)
                    .throwing(NotSerializableException.class)
                    .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"));
        }
        for (Object additionalBridgeType : additionalBridges) {
            JavaInstance.MethodType additionalBridge = JavaInstance.MethodType.of(additionalBridgeType);
            builder = builder.defineMethod(lambdaMethodName, additionalBridge.getReturnType(), MethodManifestation.BRIDGE, Visibility.PUBLIC)
                    .withParameters(additionalBridge.getParameterTypes())
                    .intercept(new BridgeMethodImplementation(lambdaMethodName, lambdaMethod));
        }
        byte[] classFile = builder.visit(DebuggingWrapper.makeDefault()).make().getBytes();
        for (ClassFileTransformer classFileTransformer : classFileTransformers) {
            byte[] transformedClassFile = classFileTransformer.transform(targetType.getClassLoader(),
                    lambdaClassName.replace('.', '/'),
                    NOT_PREVIOUSLY_DEFINED,
                    targetType.getProtectionDomain(),
                    classFile);
            classFile = transformedClassFile == null
                    ? classFile
                    : transformedClassFile;
        }
        return classFile;
    }

    private static class ConstructorImplementation implements Implementation {

        protected static final MethodDescription.InDefinedShape OBJECT_CONSTRUCTOR;

        static {
            OBJECT_CONSTRUCTOR = TypeDescription.OBJECT.getDeclaredMethods().filter(isConstructor()).getOnly();
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        private static class Appender implements ByteCodeAppender {

            private final TypeDescription instrumentedType;

            public Appender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                List<StackManipulation> fieldAssignments = new ArrayList<StackManipulation>(instrumentedType.getDeclaredFields().size() * 3);
                List<FieldDescription.InDefinedShape> fieldDescriptions = instrumentedType.getDeclaredFields();
                for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                    fieldAssignments.add(MethodVariableAccess.REFERENCE.loadOffset(0));
                    fieldAssignments.add(MethodVariableAccess.of(parameterDescription.getType()).loadOffset(parameterDescription.getOffset()));
                    fieldAssignments.add(FieldAccess.forField(fieldDescriptions.get(parameterDescription.getIndex())).putter());
                }
                return new Size(new StackManipulation.Compound(
                        MethodVariableAccess.REFERENCE.loadOffset(0),
                        MethodInvocation.invoke(OBJECT_CONSTRUCTOR),
                        new StackManipulation.Compound(fieldAssignments),
                        MethodReturn.VOID
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    private static class FactoryImplementation implements Implementation {

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        private static class Appender implements ByteCodeAppender {

            private final TypeDescription instrumentedType;

            public Appender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return new Size(new StackManipulation.Compound(
                        TypeCreation.of(instrumentedType),
                        Duplication.SINGLE,
                        MethodVariableAccess.allArgumentsOf(instrumentedMethod),
                        MethodInvocation.invoke(instrumentedType.getDeclaredMethods().filter(isConstructor()).getOnly()),
                        MethodReturn.REFERENCE
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    private static class LambdaMethodImplementation implements Implementation {

        private final MethodDescription.InDefinedShape targetMethod;

        private final JavaInstance.MethodType specializedLambdaMethod;

        public LambdaMethodImplementation(MethodDescription.InDefinedShape targetMethod, JavaInstance.MethodType specializedLambdaMethod) {
            this.targetMethod = targetMethod;
            this.specializedLambdaMethod = specializedLambdaMethod;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(targetMethod, specializedLambdaMethod, implementationTarget.getInstrumentedType().getDeclaredFields());
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        private static class Appender implements ByteCodeAppender {

            private final MethodDescription lambdaDispatcherMethod;

            private final List<FieldDescription.InDefinedShape> declaredFields;

            private final JavaInstance.MethodType specializedLambdaMethod;

            public Appender(MethodDescription lambdaDispatcherMethod,
                            JavaInstance.MethodType specializedLambdaMethod,
                            List<FieldDescription.InDefinedShape> declaredFields) {
                this.lambdaDispatcherMethod = lambdaDispatcherMethod;
                this.specializedLambdaMethod = specializedLambdaMethod;
                this.declaredFields = declaredFields;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                List<StackManipulation> fieldAccess = new ArrayList<StackManipulation>(declaredFields.size() * 2);
                for (FieldDescription.InDefinedShape fieldDescription : declaredFields) {
                    fieldAccess.add(MethodVariableAccess.REFERENCE.loadOffset(0));
                    fieldAccess.add(FieldAccess.forField(fieldDescription).getter());
                }
                List<StackManipulation> parameterAccess = new ArrayList<StackManipulation>(instrumentedMethod.getParameters().size() * 2);
                for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                    parameterAccess.add(MethodVariableAccess.of(parameterDescription.getType()).loadOffset(parameterDescription.getOffset()));
                    parameterAccess.add(Assigner.DEFAULT.assign(parameterDescription.getType(),
                            specializedLambdaMethod.getParameterTypes().get(parameterDescription.getIndex()).asGenericType(),
                            Assigner.Typing.DYNAMIC));
                }
                return new Size(new StackManipulation.Compound(
                        new StackManipulation.Compound(fieldAccess),
                        new StackManipulation.Compound(parameterAccess),
                        MethodInvocation.invoke(lambdaDispatcherMethod),
                        MethodReturn.returning(lambdaDispatcherMethod.getReturnType().asErasure())
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    private static class SerializationImplementation implements Implementation {

        private final TypeDescription targetType;

        private final TypeDescription lambdaType;

        private final String lambdaMethodName;

        private final JavaInstance.MethodType lambdaMethod;

        private final JavaInstance.MethodHandle targetMethod;

        private final JavaInstance.MethodType specializedMethod;

        public SerializationImplementation(TypeDescription targetType,
                                           TypeDescription lambdaType,
                                           String lambdaMethodName,
                                           JavaInstance.MethodType lambdaMethod,
                                           JavaInstance.MethodHandle targetMethod,
                                           JavaInstance.MethodType specializedMethod) {
            this.targetType = targetType;
            this.lambdaType = lambdaType;
            this.lambdaMethodName = lambdaMethodName;
            this.lambdaMethod = lambdaMethod;
            this.targetMethod = targetMethod;
            this.specializedMethod = specializedMethod;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            TypeDescription serializedLambda;
            try {
                serializedLambda = new TypeDescription.ForLoadedType(Class.forName("java.lang.invoke.SerializedLambda"));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot find class for lambda serialization", exception);
            }
            List<StackManipulation> lambdaArguments = new ArrayList<StackManipulation>(implementationTarget.getInstrumentedType().getDeclaredFields().size());
            for (FieldDescription.InDefinedShape fieldDescription : implementationTarget.getInstrumentedType().getDeclaredFields()) {
                lambdaArguments.add(new StackManipulation.Compound(MethodVariableAccess.REFERENCE.loadOffset(0),
                        FieldAccess.forField(fieldDescription).getter(),
                        Assigner.DEFAULT.assign(fieldDescription.getType(), TypeDescription.Generic.OBJECT, Assigner.Typing.STATIC)));
            }
            return new ByteCodeAppender.Simple(new StackManipulation.Compound(
                    TypeCreation.of(serializedLambda),
                    Duplication.SINGLE,
                    ClassConstant.of(targetType),
                    new TextConstant(lambdaType.getInternalName()),
                    new TextConstant(lambdaMethodName),
                    new TextConstant(lambdaMethod.getDescriptor()),
                    IntegerConstant.forValue(targetMethod.getHandleType().getIdentifier()),
                    new TextConstant(targetMethod.getOwnerType().getInternalName()),
                    new TextConstant(targetMethod.getName()),
                    new TextConstant(targetMethod.getDescriptor()),
                    new TextConstant(specializedMethod.getDescriptor()),
                    ArrayFactory.forType(TypeDescription.Generic.OBJECT).withValues(lambdaArguments),
                    MethodInvocation.invoke(serializedLambda.getDeclaredMethods().filter(isConstructor()).getOnly()),
                    MethodReturn.REFERENCE
            ));
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }
    }

    private static class BridgeMethodImplementation implements Implementation {

        private final String lambdaMethodName;

        private final JavaInstance.MethodType lambdaMethod;

        public BridgeMethodImplementation(String lambdaMethodName, JavaInstance.MethodType lambdaMethod) {
            this.lambdaMethodName = lambdaMethodName;
            this.lambdaMethod = lambdaMethod;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.invokeSuper(new MethodDescription.SignatureToken(lambdaMethodName,
                    lambdaMethod.getReturnType(),
                    lambdaMethod.getParameterTypes())));
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        protected static class Appender implements ByteCodeAppender {

            private final SpecialMethodInvocation bridgeMethodInvocation;

            protected Appender(SpecialMethodInvocation bridgeMethodInvocation) {
                this.bridgeMethodInvocation = bridgeMethodInvocation;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return new Compound(new Simple(
                        MethodVariableAccess.allArgumentsOf(instrumentedMethod)
                                .asBridgeOf(bridgeMethodInvocation.getMethodDescription())
                                .prependThisReference(),
                        bridgeMethodInvocation,
                        bridgeMethodInvocation.getMethodDescription().getReturnType().asErasure().isAssignableTo(instrumentedMethod.getReturnType().asErasure())
                                ? StackManipulation.Trivial.INSTANCE
                                : TypeCasting.to(instrumentedMethod.getReceiverType().asErasure()),
                        MethodReturn.returning(instrumentedMethod.getReturnType().asErasure())

                )).apply(methodVisitor, implementationContext, instrumentedMethod);
            }
        }
    }
}
