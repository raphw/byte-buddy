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

    public byte[] make(Object targetTypeLookup,
                       String lambdaMethodName,
                       Object factoryMethodType,
                       Object lambdaMethodType,
                       Object targetMethodHandle,
                       Object specializedLambdaMethodType,
                       boolean enforceSerialization,
                       List<Class<?>> markerInterfaces,
                       Collection<? extends ClassFileTransformer> classFileTransformers) throws Exception {
        JavaInstance.MethodType factoryMethod = JavaInstance.MethodType.of(factoryMethodType);
        JavaInstance.MethodType lambdaMethod = JavaInstance.MethodType.of(lambdaMethodType);
        JavaInstance.MethodHandle targetMethod = JavaInstance.MethodHandle.of(targetMethodHandle, targetTypeLookup);
        Class<?> targetType = JavaInstance.MethodHandle.lookupType(targetTypeLookup);
        String lambdaClassName = targetType.getName() + LAMBDA_TYPE_INFIX + lambdaNameCounter.incrementAndGet();
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(factoryMethod.getReturnType(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .modifiers(SyntheticState.SYNTHETIC, TypeManifestation.FINAL, lambdaMethod.getParameterTypes().isEmpty()
                        ? Visibility.PUBLIC
                        : Visibility.PACKAGE_PRIVATE)
                .implement(markerInterfaces)
                .name(lambdaClassName)
                .defineConstructor(factoryMethod.getParameterTypes().isEmpty() ? Visibility.PUBLIC : Visibility.PRIVATE)
                .withParameters(factoryMethod.getParameterTypes())
                .intercept(new ConstructorImplementation())
                .method(named(lambdaMethodName)
                        .and(takesArguments(lambdaMethod.getParameterTypes()))
                        .and(returns(lambdaMethod.getReturnType())))
                .intercept(new LambdaMethodImplementation(targetMethod.asMethodDescription()));
        int index = 0;
        for (TypeDescription capturedType : factoryMethod.getParameterTypes()) {
            builder = builder.defineField(FIELD_PREFIX + index++, capturedType, Visibility.PUBLIC, FieldManifestation.FINAL);
        }
        if (!factoryMethod.getParameterTypes().isEmpty()) {
            builder = builder.defineMethod(LAMBDA_FACTORY, factoryMethod.getReturnType(), Visibility.PRIVATE, Ownership.STATIC)
                    .withParameters(factoryMethod.getParameterTypes())
                    .intercept(new FactoryImplementation());
        }
        if (enforceSerialization || factoryMethod.getReturnType().isAssignableTo(Serializable.class) || markerInterfaces.contains(Serializable.class)) {
            builder = builder.defineMethod("writeReplace", Object.class)
                    .intercept(new SerializationImplementation(new TypeDescription.ForLoadedType(targetType),
                            targetMethod,
                            JavaInstance.MethodType.of(specializedLambdaMethodType)));
        } else {
            builder = builder.defineMethod("readObject", ObjectInputStream.class)
                    .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"))
                    .defineMethod("writeObject", ObjectOutputStream.class)
                    .intercept(ExceptionMethod.throwing(NotSerializableException.class, "Non-serializable lambda"));
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
//                        MethodVariableAccess.allArgumentsOf(lambdaDispatcherMethod),
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
