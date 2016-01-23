package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.JavaInstance;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class LambdaCreationDispatcher {

    private static final String LAMBDA_FACTORY = "get$Lambda";

    private static final String FIELD_PREFIX = "arg$";

    private static final String LAMBDA_TYPE_INFIX = "$$Lambda$";

    private static final Class<?> NOT_PREVIOUSLY_DEFINED = null;

    private static final AtomicInteger lambdaNameCounter = new AtomicInteger();

    public static byte[] createClass(Object callerClassLookup,
                                     Class<?> lookupClass,
                                     String functionalMethodName,
                                     Object functionalMethodType,
                                     Object expectedMethodType,
                                     Object targetMethodHandle,
                                     Set<ClassFileTransformer> classFileTransformers) throws Exception {
        JavaInstance.MethodType factoryMethodType = JavaInstance.MethodType.of(expectedMethodType);
        JavaInstance.MethodType lambdaMethodType = JavaInstance.MethodType.of(functionalMethodType);
        JavaInstance.MethodHandle lambdaImplementationHandle = JavaInstance.MethodHandle.of(targetMethodHandle, callerClassLookup);
        String lambdaClassName = lookupClass.getName() + LAMBDA_TYPE_INFIX + lambdaNameCounter.incrementAndGet();
        DynamicType.Builder<?> builder = new ByteBuddy()
                .subclass(lambdaMethodType.getReturnType())
                .modifiers(SyntheticState.SYNTHETIC, TypeManifestation.FINAL)
                .implement(factoryMethodType.getReturnType())
                .name(lambdaClassName);
        int index = 0;
        for (TypeDescription parameterTypes : factoryMethodType.getParameterTypes()) {
            builder = builder.defineField(FIELD_PREFIX + index++, parameterTypes, Visibility.PUBLIC, FieldManifestation.FINAL);
        }
        if (!factoryMethodType.getParameterTypes().isEmpty()) {
            builder = builder.defineMethod(LAMBDA_FACTORY, factoryMethodType.getReturnType(), Visibility.PRIVATE, Ownership.STATIC)
                    .intercept(new FactoryImplementation());
        }
        byte[] classFile = builder.defineConstructor(Visibility.PRIVATE)
                .intercept(SuperMethodCall.INSTANCE.andThen(new ConstructorImplementation()))
                .method(named(functionalMethodName).and(takesArguments(factoryMethodType.getParameterTypes())).and(returns(factoryMethodType.getReturnType())))
                .intercept(new LambdaMethodImplementation(lambdaImplementationHandle))
                // Serialization
                .make()
                .getBytes();
        for (ClassFileTransformer classFileTransformer : classFileTransformers) {
            byte[] transformedClassFile = classFileTransformer.transform(lookupClass.getClassLoader(),
                    lambdaClassName.replace('.', '/'),
                    NOT_PREVIOUSLY_DEFINED,
                    lookupClass.getProtectionDomain(),
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

        private final JavaInstance.MethodHandle lambdaImplementationHandle;

        public LambdaMethodImplementation(JavaInstance.MethodHandle lambdaImplementationHandle) {
            this.lambdaImplementationHandle = lambdaImplementationHandle;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(lambdaImplementationHandle.asMethodDescription(), implementationTarget.getInstrumentedType().getDeclaredFields());
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
}
