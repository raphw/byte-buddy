package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodNameEqualityResolver;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.*;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static com.blogspot.mydailyjava.bytebuddy.utility.UserInput.*;

public class MethodDelegation implements Instrumentation {

    private static final String NO_METHODS_ERROR_MESSAGE = "The target type does not define any methods for delegation";

    private static interface InstrumentationDelegate {

        static enum ForStaticMethod implements InstrumentationDelegate {
            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType) {
                return LegalTrivialStackManipulation.INSTANCE;
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return MethodDelegationBinder.MethodInvoker.Simple.INSTANCE;
            }
        }

        static class ForStaticFieldInstance implements InstrumentationDelegate, TypeInitializer {

            private static final Object STATIC_FIELD = null;
            private static final String PREFIX = "methodDelegate";

            private final String fieldName;
            private final Object delegate;

            public ForStaticFieldInstance(Object delegate) {
                this.delegate = delegate;
                fieldName = String.format("%s$%d", PREFIX, Math.abs(new Random().nextInt()));
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(fieldName,
                        new TypeDescription.ForLoadedType(delegate.getClass()),
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC)
                        .withInitializer(this);
            }

            @Override
            public StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType) {
                return FieldAccess.forField(instrumentedType.getDeclaredFields().named(fieldName)).getter();
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return new MethodDelegationBinder.MethodInvoker.Virtual(instrumentedType);
            }

            @Override
            public void onLoad(Class<?> type) {
                try {
                    Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(STATIC_FIELD, delegate);
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot set static field " + fieldName + " on " + type, e);
                }
            }
        }

        InstrumentedType prepare(InstrumentedType instrumentedType);

        StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType);

        MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType);
    }

    private static class MethodDelegationByteCodeAppender implements ByteCodeAppender {

        private final StackManipulation preparingStackAssignment;
        private final TypeDescription instrumentedType;
        private final Iterable<? extends MethodDescription> targetMethods;
        private final MethodDelegationBinder.Processor processor;

        private MethodDelegationByteCodeAppender(StackManipulation preparingStackAssignment,
                                                 TypeDescription instrumentedType,
                                                 Iterable<? extends MethodDescription> targetMethods,
                                                 MethodDelegationBinder.Processor processor) {
            this.preparingStackAssignment = preparingStackAssignment;
            this.instrumentedType = instrumentedType;
            this.targetMethods = targetMethods;
            this.processor = processor;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    preparingStackAssignment,
                    processor.process(instrumentedType, instrumentedMethod, targetMethods),
                    MethodReturn.returning(instrumentedMethod.getReturnType())
            ).apply(methodVisitor, instrumentationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    public static Instrumentation to(Class<?> type) {
        if (type == null) {
            throw new NullPointerException();
        } else if (type.isInterface()) {
            throw new IllegalArgumentException("Cannot delegate to interface " + type);
        } else if (type.isArray()) {
            throw new IllegalArgumentException("Cannot delegate to array " + type);
        } else if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot delegate to primitive " + type);
        }
        return new MethodDelegation(InstrumentationDelegate.ForStaticMethod.INSTANCE,
                defaultArgumentBinders(),
                defaultDefaultsProvider(),
                defaultAmbiguityResolver(),
                defaultAssigner(),
                new TypeDescription.ForLoadedType(type).getReachableMethods().filter(isStatic().and(not(isPrivate()))));
    }

    public static Instrumentation to(Object delegate) {
        if (delegate == null) {
            throw new NullPointerException();
        }
        return new MethodDelegation(new InstrumentationDelegate.ForStaticFieldInstance(delegate),
                defaultArgumentBinders(),
                defaultDefaultsProvider(),
                defaultAmbiguityResolver(),
                defaultAssigner(),
                new TypeDescription.ForLoadedType(delegate.getClass()).getReachableMethods().filter(not(isStatic()).and(not(isPrivate()))));
    }

    private static List<AnnotationDrivenBinder.ArgumentBinder<?>> defaultArgumentBinders() {
        return Arrays.<AnnotationDrivenBinder.ArgumentBinder<?>>asList(Argument.Binder.INSTANCE,
                This.Binder.INSTANCE,
                AllArguments.Binder.INSTANCE,
                SuperCall.Binder.INSTANCE);
    }

    private static AnnotationDrivenBinder.DefaultsProvider<?> defaultDefaultsProvider() {
        return Argument.NextUnboundAsDefaultsProvider.INSTANCE;
    }

    private static MethodDelegationBinder.AmbiguityResolver defaultAmbiguityResolver() {
        return new MethodDelegationBinder.AmbiguityResolver.Chain(
                MethodNameEqualityResolver.INSTANCE,
                MostSpecificTypeResolver.INSTANCE
        );
    }

    private static Assigner defaultAssigner() {
        return new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), false);
    }

    private final InstrumentationDelegate instrumentationDelegate;
    private final List<AnnotationDrivenBinder.ArgumentBinder<?>> argumentBinders;
    private final AnnotationDrivenBinder.DefaultsProvider<?> defaultsProvider;
    private final MethodDelegationBinder.AmbiguityResolver ambiguityResolver;
    private final Assigner assigner;
    private final MethodList methodList;

    protected MethodDelegation(InstrumentationDelegate instrumentationDelegate,
                               List<AnnotationDrivenBinder.ArgumentBinder<?>> argumentBinders,
                               AnnotationDrivenBinder.DefaultsProvider<?> defaultsProvider,
                               MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                               Assigner assigner,
                               MethodList methodList) {
        this.instrumentationDelegate = instrumentationDelegate;
        this.argumentBinders = argumentBinders;
        this.defaultsProvider = defaultsProvider;
        this.ambiguityResolver = ambiguityResolver;
        this.assigner = assigner;
        this.methodList = containsElements(methodList, NO_METHODS_ERROR_MESSAGE);
    }

    public MethodDelegation appendArgumentBinder(AnnotationDrivenBinder.ArgumentBinder<?> argumentBinder) {
        return new MethodDelegation(instrumentationDelegate,
                join(argumentBinders, nonNull(argumentBinder)),
                defaultsProvider,
                ambiguityResolver,
                assigner,
                methodList);
    }

    public MethodDelegation defineArgumentBinder(AnnotationDrivenBinder.ArgumentBinder<?>... argumentBinder) {
        return new MethodDelegation(instrumentationDelegate,
                Arrays.asList(nonNull(argumentBinder)),
                defaultsProvider,
                ambiguityResolver,
                assigner,
                methodList);
    }

    public MethodDelegation defaultsProvider(AnnotationDrivenBinder.DefaultsProvider defaultsProvider) {
        return new MethodDelegation(instrumentationDelegate,
                argumentBinders,
                nonNull(defaultsProvider),
                ambiguityResolver,
                assigner,
                methodList);
    }

    public MethodDelegation appendAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver ambiguityResolver) {
        return defineAmbiguityResolver(new MethodDelegationBinder.AmbiguityResolver.Chain(
                this.ambiguityResolver, nonNull(ambiguityResolver)));
    }

    public MethodDelegation defineAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver... ambiguityResolver) {
        return new MethodDelegation(instrumentationDelegate,
                argumentBinders,
                defaultsProvider,
                new MethodDelegationBinder.AmbiguityResolver.Chain(nonNull(ambiguityResolver)),
                assigner,
                methodList);
    }

    public MethodDelegation assigner(Assigner assigner) {
        return new MethodDelegation(instrumentationDelegate,
                argumentBinders,
                defaultsProvider,
                ambiguityResolver,
                nonNull(assigner),
                methodList);
    }

    public MethodDelegation filter(MethodMatcher methodMatcher) {
        return new MethodDelegation(instrumentationDelegate,
                argumentBinders,
                defaultsProvider,
                ambiguityResolver,
                assigner,
                containsElements(methodList.filter(nonNull(methodMatcher)), NO_METHODS_ERROR_MESSAGE));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentationDelegate.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(TypeDescription instrumentedType) {
        return new MethodDelegationByteCodeAppender(instrumentationDelegate.getPreparingStackAssignment(instrumentedType),
                instrumentedType,
                methodList,
                new MethodDelegationBinder.Processor(new AnnotationDrivenBinder(
                        argumentBinders,
                        defaultsProvider,
                        assigner,
                        instrumentationDelegate.getMethodInvoker(instrumentedType)
                ), ambiguityResolver));
    }
}
