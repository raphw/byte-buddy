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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class LambdaMetaFactory {

    private static final Set<ClassFileTransformer> CLASS_FILE_TRANSFORMERS = new LinkedHashSet<ClassFileTransformer>();

    private static final AtomicInteger lambdaNameCounter = new AtomicInteger();

    public byte[] metaFactory(MethodHandles.Lookup caller,
                              String invokedName,
                              MethodType invokedType,
                              MethodType samMethodType,
                              MethodHandle implMethod,
                              MethodType instantiatedMethodType) throws Exception {

        JavaInstance.MethodType factoryMethodType = JavaInstance.MethodType.of(invokedType);
        JavaInstance.MethodType lambdaMethodType = JavaInstance.MethodType.of(samMethodType);

        DynamicType.Builder<?> builder = new ByteBuddy()
                .subclass(lambdaMethodType.getReturnType())
                .modifiers(SyntheticState.SYNTHETIC, TypeManifestation.FINAL)
                .implement(factoryMethodType.getReturnType())
                .name(caller.lookupClass().getName() + "$$Lambda$" + lambdaNameCounter.incrementAndGet());
        int index = 0;
        for (TypeDescription parameterTypes : factoryMethodType.getParameterTypes()) {
            builder = builder.defineField("arg$" + index++, parameterTypes, Visibility.PUBLIC, FieldManifestation.FINAL);
        }
        if (!factoryMethodType.getParameterTypes().isEmpty()) {
            builder = builder.defineMethod("get$Lambda", factoryMethodType.getReturnType(), Visibility.PRIVATE, Ownership.STATIC)
                    .intercept(new FactoryImplementation());
        }
        byte[] classFile = builder.defineConstructor(Visibility.PRIVATE)
                .intercept(SuperMethodCall.INSTANCE.andThen(new ConstructorImplementation()))
                .method(named(invokedName).and(takesArguments(factoryMethodType.getParameterTypes())).and(returns(factoryMethodType.getReturnType())))
                .intercept(new LambdaMethodImplementation())
                // Serialization
                .make()
                .getBytes();
        for (ClassFileTransformer classFileTransformer : CLASS_FILE_TRANSFORMERS) {
            byte[] transformedClassFile = classFileTransformer.transform(null, null, null, null, classFile);
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
                List<StackManipulation> fieldAssignment = new ArrayList<StackManipulation>(instrumentedType.getDeclaredFields().size());
                List<FieldDescription.InDefinedShape> fieldDescriptions = instrumentedType.getDeclaredFields();
                for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                    fieldAssignment.add(new StackManipulation.Compound(
                            MethodVariableAccess.REFERENCE.loadOffset(0),
                            MethodVariableAccess.of(parameterDescription.getType()).loadOffset(parameterDescription.getOffset()),
                            FieldAccess.forField(fieldDescriptions.get(parameterDescription.getIndex())).putter()
                    ));
                }
                return new Size(new StackManipulation.Compound(fieldAssignment).apply(methodVisitor, implementationContext)
                        .getMaximalSize(), instrumentedMethod.getStackSize());
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
                return null;
            }
        }
    }
}
