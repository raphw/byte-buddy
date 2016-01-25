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
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
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

    private static final AtomicInteger lambdaNameCounter = new AtomicInteger();

    private final ByteBuddy byteBuddy;

    public LambdaCreator(ByteBuddy byteBuddy) {
        this.byteBuddy = byteBuddy;
    }

    public byte[] make(Object callerTypeLookup,
                              String functionalMethodName,
                              Object factoryMethod,
                              Object implementedMethod,
                              Object targetMethodHandle,
                              Object specializedMethodType,
                              boolean enforceSerialization,
                              List<Class<?>> markerInterfaces,
                              Collection<? extends ClassFileTransformer> classFileTransformers) throws Exception {
        JavaInstance.MethodType factoryMethodType = JavaInstance.MethodType.of(factoryMethod);
        JavaInstance.MethodType implementedMethodType = JavaInstance.MethodType.of(implementedMethod);
        JavaInstance.MethodHandle targetMethod = JavaInstance.MethodHandle.of(targetMethodHandle, callerTypeLookup);
        Class<?> lookupType = JavaInstance.MethodHandle.lookupType(callerTypeLookup);
        String lambdaClassName = lookupType.getName() + LAMBDA_TYPE_INFIX + lambdaNameCounter.incrementAndGet();
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(factoryMethodType.getReturnType(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .modifiers(SyntheticState.SYNTHETIC, TypeManifestation.FINAL, implementedMethodType.getParameterTypes().isEmpty()
                        ? Visibility.PUBLIC
                        : Visibility.PACKAGE_PRIVATE)
                .implement(markerInterfaces)
                .name(lambdaClassName);
        int index = 0;
        for (TypeDescription parameterTypes : implementedMethodType.getParameterTypes()) {
            builder = builder.defineField(FIELD_PREFIX + index++, parameterTypes, Visibility.PUBLIC, FieldManifestation.FINAL);
        }
        if (!implementedMethodType.getParameterTypes().isEmpty()) {
            builder = builder.defineMethod(LAMBDA_FACTORY, factoryMethodType.getReturnType(), Visibility.PRIVATE, Ownership.STATIC)
                    .withParameters(factoryMethodType.getParameterTypes())
                    .intercept(new FactoryImplementation());
        }
        if (enforceSerialization || factoryMethodType.getReturnType().isAssignableTo(Serializable.class)) {
            builder = builder.defineMethod("writeReplace", Object.class)
                    .intercept(new SerializationImplementation(new TypeDescription.ForLoadedType(lookupType),
                            targetMethod,
                            JavaInstance.MethodType.of(specializedMethodType)));
        } else {
            builder = builder.defineMethod("readObject", ObjectInputStream.class)
                    .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"))
                    .defineMethod("writeObject", ObjectOutputStream.class)
                    .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"));
        }
        byte[] classFile = builder.defineConstructor(factoryMethodType.getParameterTypes().isEmpty() ? Visibility.PUBLIC : Visibility.PRIVATE)
                .intercept(SuperMethodCall.INSTANCE.andThen(new ConstructorImplementation()))
                .method(named(functionalMethodName)
                        .and(takesArguments(implementedMethodType.getParameterTypes()))
                        .and(returns(implementedMethodType.getReturnType())))
                .intercept(new LambdaMethodImplementation(targetMethod.asMethodDescription()))
                .make()
                .getBytes();
        for (ClassFileTransformer classFileTransformer : classFileTransformers) {
            byte[] transformedClassFile = classFileTransformer.transform(lookupType.getClassLoader(),
                    lambdaClassName.replace('.', '/'),
                    NOT_PREVIOUSLY_DEFINED,
                    lookupType.getProtectionDomain(),
                    classFile);
            classFile = transformedClassFile == null
                    ? classFile
                    : transformedClassFile;
        }
        return classFile;
    }

    private static class ConstructorImplementation implements Implementation {

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
                        MethodVariableAccess.allArgumentsOf(instrumentedMethod),
                        TypeCreation.of(instrumentedType),
                        Duplication.SINGLE,
                        MethodInvocation.invoke(instrumentedType.getDeclaredMethods().filter(isConstructor()).getOnly()),
                        MethodReturn.REFERENCE
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    private static class LambdaMethodImplementation implements Implementation {

        private final MethodDescription.InDefinedShape targetMethod;

        public LambdaMethodImplementation(MethodDescription.InDefinedShape targetMethod) {
            this.targetMethod = targetMethod;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(targetMethod, implementationTarget.getInstrumentedType().getDeclaredFields());
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        private static class Appender implements ByteCodeAppender {

            private final MethodDescription lambdaDispatcherMethod;

            private final List<FieldDescription.InDefinedShape> declaredFields;

            public Appender(MethodDescription lambdaDispatcherMethod, List<FieldDescription.InDefinedShape> declaredFields) {
                this.lambdaDispatcherMethod = lambdaDispatcherMethod;
                this.declaredFields = declaredFields;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                List<StackManipulation> fieldAccess = new ArrayList<StackManipulation>(declaredFields.size() * 2);
                for (FieldDescription.InDefinedShape fieldDescription : declaredFields) {
                    fieldAccess.add(MethodVariableAccess.REFERENCE.loadOffset(0));
                    fieldAccess.add(FieldAccess.forField(fieldDescription).getter());
                }
                return new Size(new StackManipulation.Compound(
                        new StackManipulation.Compound(fieldAccess),
                        MethodVariableAccess.allArgumentsOf(lambdaDispatcherMethod),
                        MethodInvocation.invoke(lambdaDispatcherMethod),
                        MethodReturn.returning(lambdaDispatcherMethod.getReturnType().asErasure())
                ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
            }
        }
    }

    private static class SerializationImplementation implements Implementation {

        private final TypeDescription targetType;

        private final JavaInstance.MethodHandle targetMethodHandle;

        private final JavaInstance.MethodType specializedMethodType;

        public SerializationImplementation(TypeDescription targetType, JavaInstance.MethodHandle targetMethodHandle, JavaInstance.MethodType specializedMethodType) {
            this.targetType = targetType;
            this.targetMethodHandle = targetMethodHandle;
            this.specializedMethodType = specializedMethodType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            TypeDescription serializedLambda;
            try {
                serializedLambda = new TypeDescription.ForLoadedType(Class.forName("java.lang.invoke.SerializableLambda"));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot find serializable lambda class", exception);
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
                    new TextConstant(implementationTarget.getInstrumentedType().getInternalName()),
                    new TextConstant(targetMethodHandle.getName()),
                    new TextConstant(targetMethodHandle.getDescriptor()),
                    IntegerConstant.forValue(targetMethodHandle.getHandleType().getIdentifier()),
                    new TextConstant(targetMethodHandle.getInstanceType().getInternalName()),
                    new TextConstant(targetMethodHandle.getName()),
                    new TextConstant(specializedMethodType.getDescriptor()),
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

}
