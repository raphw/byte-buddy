package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.DoubleConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.FloatConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.IntegerConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.LongConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

public class InvokeDynamic implements Instrumentation {

    /**
     * Returns the default assigner to be used by this instrumentation.
     *
     * @return The default assigner to be used by this instrumentation.
     */
    private static Assigner defaultAssigner() {
        return new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE));
    }

    /**
     * Returns the default value for using dynamically typed value assignments.
     *
     * @return The default value for using dynamically typed value assignments.
     */
    private static boolean defaultDynamicallyTyped() {
        return false;
    }

    public static WithImplicitTarget bootstrap(Method method, Object... argument) {
        return bootstrap(new MethodDescription.ForLoadedMethod(nonNull(method)), argument);
    }

    public static WithImplicitTarget bootstrap(Constructor<?> constructor, Object... argument) {
        return bootstrap(new MethodDescription.ForLoadedConstructor(nonNull(constructor)), argument);
    }

    public static WithImplicitTarget bootstrap(MethodDescription bootstrapMethod, Object... argument) {
        List<?> arguments = Arrays.asList(nonNull(argument));
        if (!bootstrapCompatible(bootstrapMethod, arguments)) {
            throw new IllegalArgumentException("Bootstrap method " + bootstrapMethod + " does not accept " + arguments);
        }
        return new WithImplicitTarget(nonNull(bootstrapMethod),
                arguments,
                TargetProvider.ForInterceptedMethod.INSTANCE,
                TerminationHandler.ForMethodReturn.INSTANCE,
                defaultAssigner(),
                defaultDynamicallyTyped());
    }

    private static boolean bootstrapCompatible(MethodDescription bootstrapMethod, List<?> arguments) {
        if (!bootstrapMethod.isBootstrap()) {
            throw new IllegalArgumentException("Not a bootstrap method: " + bootstrapMethod);
        }
        // TODO: Implement validation if arguments match parameters
        return true;
    }

    protected final MethodDescription bootstrapMethod;

    protected final List<?> handleArguments;

    protected final TargetProvider targetProvider;

    protected final TerminationHandler terminationHandler;

    protected final Assigner assigner;

    protected final boolean dynamicallyTyped;

    protected InvokeDynamic(MethodDescription bootstrapMethod,
                            List<?> handleArguments,
                            TargetProvider targetProvider,
                            TerminationHandler terminationHandler,
                            Assigner assigner,
                            boolean dynamicallyTyped) {
        this.bootstrapMethod = bootstrapMethod;
        this.handleArguments = handleArguments;
        this.targetProvider = targetProvider;
        this.terminationHandler = terminationHandler;
        this.assigner = assigner;
        this.dynamicallyTyped = dynamicallyTyped;
    }

    public InvokeDynamic withValue(boolean... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (boolean aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForBooleanValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(byte... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (byte aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForByteValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(short... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (short aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForShortValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(char... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (char aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForCharacterValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(int... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (int aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForIntegerValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(long... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (long aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForLongValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(float... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (float aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForFloatValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(double... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (double aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForDoubleValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withValue(Object... value) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(value.length);
        for (Object aValue : value) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForStaticField(nonNull(aValue)));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withArgument(int... index) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(index.length);
        for (int anIndex : index) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForMethodParameter(anIndex));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withAssigner(Assigner assigner, boolean dynamicallyTyped) {
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider,
                terminationHandler,
                nonNull(assigner),
                dynamicallyTyped);
    }

    public Instrumentation andThen(Instrumentation instrumentation) {
        return new Instrumentation.Compound(new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider,
                TerminationHandler.ForChainedInvocation.INSTANCE,
                assigner,
                dynamicallyTyped),
                nonNull(instrumentation));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return targetProvider.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target instrumentationTarget) {
        return new Appender(instrumentationTarget.getTypeDescription());
    }

    protected class Appender implements ByteCodeAppender {

        private final TypeDescription instrumentedType;

        public Appender(TypeDescription instrumentedType) {
            this.instrumentedType = instrumentedType;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            TargetProvider.Target target = targetProvider.resolve(instrumentedMethod);
            StackManipulation.Size size = new StackManipulation.Compound(
                    target.resolve(instrumentedType, instrumentedMethod),
                    MethodInvocation.invoke(bootstrapMethod)
                            .dynamic(target.getInternalName(),
                                    target.getReturnType(),
                                    target.getParameterTypes(),
                                    handleArguments),
                    terminationHandler.resolve(instrumentedMethod)
            ).apply(methodVisitor, instrumentationContext);
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    public static class WithImplicitTarget extends InvokeDynamic {

        protected WithImplicitTarget(MethodDescription bootstrapMethod,
                                     List<?> handleArguments,
                                     TargetProvider targetProvider,
                                     TerminationHandler terminationHandler,
                                     Assigner assigner,
                                     boolean dynamicallyTyped) {
            super(bootstrapMethod,
                    handleArguments,
                    targetProvider,
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        public InvokeDynamic invoke(Class<?> returnType) {
            return invoke(new TypeDescription.ForLoadedType(nonNull(returnType)));
        }

        public InvokeDynamic invoke(TypeDescription returnType) {
            return invoke(TargetProvider.ForSyntheticCall.NameProvider.ForInterceptedMethod.INSTANCE,
                    new TargetProvider.ForSyntheticCall.ReturnTypeProvider.ForExplicitType(returnType));
        }

        public InvokeDynamic invoke(String methodName) {
            return invoke(new TargetProvider.ForSyntheticCall.NameProvider.ForExplicitName(isValidIdentifier(methodName)),
                    TargetProvider.ForSyntheticCall.ReturnTypeProvider.ForInterceptedMethod.INSTANCE);
        }

        public InvokeDynamic invoke(String methodName, Class<?> returnType) {
            return invoke(methodName, new TypeDescription.ForLoadedType(nonNull(returnType)));
        }

        public InvokeDynamic invoke(String methodName, TypeDescription returnType) {
            return invoke(new TargetProvider.ForSyntheticCall.NameProvider.ForExplicitName(isValidIdentifier(methodName)),
                    new TargetProvider.ForSyntheticCall.ReturnTypeProvider.ForExplicitType(returnType));
        }

        private InvokeDynamic invoke(TargetProvider.ForSyntheticCall.NameProvider nameProvider,
                                     TargetProvider.ForSyntheticCall.ReturnTypeProvider returnTypeProvider) {
            return new InvokeDynamic(bootstrapMethod,
                    handleArguments,
                    new TargetProvider.ForSyntheticCall(nameProvider, returnTypeProvider, Collections.<TargetProvider.ArgumentProvider>emptyList()),
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }
    }

    protected static interface TargetProvider {

        Target resolve(MethodDescription methodDescription);

        TargetProvider withArguments(List<ForSyntheticCall.ArgumentProvider> argumentProviders);

        InstrumentedType prepare(InstrumentedType instrumentedType);

        static interface Target {

            StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

            String getInternalName();

            TypeDescription getReturnType();

            List<? extends TypeDescription> getParameterTypes();

            static class ForMethodDescription implements Target {

                private final MethodDescription methodDescription;

                protected ForMethodDescription(MethodDescription methodDescription) {
                    this.methodDescription = methodDescription;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return MethodVariableAccess.loadArguments(methodDescription);
                }

                @Override
                public String getInternalName() {
                    return methodDescription.getInternalName();
                }

                @Override
                public TypeDescription getReturnType() {
                    return methodDescription.getReturnType();
                }

                @Override
                public List<? extends TypeDescription> getParameterTypes() {
                    return methodDescription.getParameterTypes();
                }
            }
        }

        static interface ArgumentProvider {

            StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

            TypeDescription resolve(MethodDescription instrumentedMethod);

            InstrumentedType prepare(InstrumentedType instrumentedType);

            static class ForStaticField implements ArgumentProvider {

                private static final String FIELD_PREFIX = "dynamicCall";

                private static final int FIELD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;

                private final Object value;

                private final String name;

                public ForStaticField(Object value) {
                    this.value = value;
                    name = String.format("%s$%s", FIELD_PREFIX, RandomString.make());
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(name)).getOnly()).getter();
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(value.getClass());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType
                            .withField(name, new TypeDescription.ForLoadedType(value.getClass()), FIELD_MODIFIER)
                            .withInitializer(LoadedTypeInitializer.ForStaticField.nonAccessible(name, value));
                }
            }

            static class ForMethodParameter implements ArgumentProvider {

                private final int index;

                public ForMethodParameter(int index) {
                    this.index = index;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    TypeList parameterTypes = instrumentedMethod.getParameterTypes();
                    if (parameterTypes.size() >= index) {
                        throw new IllegalArgumentException("No parameter " + index + " for " + instrumentedMethod);
                    }
                    return MethodVariableAccess.forType(parameterTypes.get(index)).loadFromIndex(index);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    TypeList parameterTypes = instrumentedMethod.getParameterTypes();
                    if (parameterTypes.size() >= index) {
                        throw new IllegalArgumentException("No parameter " + index + " for " + instrumentedMethod);
                    }
                    return parameterTypes.get(index);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForBooleanValue implements ArgumentProvider {

                private final boolean value;

                public ForBooleanValue(boolean value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return IntegerConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(boolean.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForByteValue implements ArgumentProvider {

                private final byte value;

                public ForByteValue(byte value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return IntegerConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(byte.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForShortValue implements ArgumentProvider {

                private final short value;

                public ForShortValue(short value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return IntegerConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(short.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForCharacterValue implements ArgumentProvider {

                private final char value;

                public ForCharacterValue(char value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return IntegerConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(char.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForIntegerValue implements ArgumentProvider {

                private final int value;

                public ForIntegerValue(int value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return IntegerConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(int.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForLongValue implements ArgumentProvider {

                private final long value;

                public ForLongValue(long value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return LongConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(long.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForFloatValue implements ArgumentProvider {

                private final float value;

                public ForFloatValue(float value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return FloatConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(float.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForDoubleValue implements ArgumentProvider {

                private final double value;

                public ForDoubleValue(double value) {
                    this.value = value;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    return DoubleConstant.forValue(value);
                }

                @Override
                public TypeDescription resolve(MethodDescription instrumentedMethod) {
                    return new TypeDescription.ForLoadedType(double.class);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }
        }

        static enum ForInterceptedMethod implements TargetProvider {

            INSTANCE;

            @Override
            public Target resolve(MethodDescription methodDescription) {
                return new Target.ForMethodDescription(methodDescription);
            }

            @Override
            public TargetProvider withArguments(List<ArgumentProvider> argumentProviders) {
                return new ForSyntheticCall(ForSyntheticCall.NameProvider.ForInterceptedMethod.INSTANCE,
                        ForSyntheticCall.ReturnTypeProvider.ForInterceptedMethod.INSTANCE,
                        argumentProviders);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

        }

        static class ForSyntheticCall implements TargetProvider {

            private final NameProvider nameProvider;

            private final ReturnTypeProvider returnTypeProvider;

            private final List<ArgumentProvider> argumentProviders;

            public ForSyntheticCall(NameProvider nameProvider,
                                    ReturnTypeProvider returnTypeProvider,
                                    List<ArgumentProvider> argumentProviders) {
                this.nameProvider = nameProvider;
                this.returnTypeProvider = returnTypeProvider;
                this.argumentProviders = argumentProviders;
            }

            @Override
            public Target resolve(MethodDescription methodDescription) {
                return new Target(nameProvider.resolve(methodDescription),
                        returnTypeProvider.resolve(methodDescription),
                        argumentProviders,
                        methodDescription);
            }

            @Override
            public TargetProvider withArguments(List<ArgumentProvider> argumentProviders) {
                return new ForSyntheticCall(nameProvider,
                        returnTypeProvider,
                        join(this.argumentProviders, argumentProviders));
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                for (ArgumentProvider argumentProvider : argumentProviders) {
                    instrumentedType = argumentProvider.prepare(instrumentedType);
                }
                return instrumentedType;
            }

            protected static interface NameProvider {

                String resolve(MethodDescription methodDescription);

                static enum ForInterceptedMethod implements NameProvider {

                    INSTANCE;

                    @Override
                    public String resolve(MethodDescription methodDescription) {
                        return methodDescription.getInternalName();
                    }
                }

                static class ForExplicitName implements NameProvider {

                    private final String internalName;

                    public ForExplicitName(String internalName) {
                        this.internalName = internalName;
                    }

                    @Override
                    public String resolve(MethodDescription methodDescription) {
                        return internalName;
                    }
                }
            }

            protected static interface ReturnTypeProvider {

                TypeDescription resolve(MethodDescription methodDescription);

                static enum ForInterceptedMethod implements ReturnTypeProvider {

                    INSTANCE;

                    @Override
                    public TypeDescription resolve(MethodDescription methodDescription) {
                        return methodDescription.getReturnType();
                    }
                }

                static class ForExplicitType implements ReturnTypeProvider {

                    private final TypeDescription typeDescription;

                    public ForExplicitType(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    @Override
                    public TypeDescription resolve(MethodDescription methodDescription) {
                        return typeDescription;
                    }
                }
            }

            protected static class Target implements TargetProvider.Target {

                private final String internalName;

                private final TypeDescription returnType;

                private final List<ArgumentProvider> argumentProviders;

                private final MethodDescription instrumentedMethod;

                public Target(String internalName,
                              TypeDescription returnType,
                              List<ArgumentProvider> argumentProviders,
                              MethodDescription instrumentedMethod) {
                    this.internalName = internalName;
                    this.returnType = returnType;
                    this.argumentProviders = argumentProviders;
                    this.instrumentedMethod = instrumentedMethod;
                }

                @Override
                public StackManipulation resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                    StackManipulation[] stackManipulation = new StackManipulation[argumentProviders.size()];
                    int index = 0;
                    for (ArgumentProvider argumentProvider : argumentProviders) {
                        stackManipulation[index++] = argumentProvider.resolve(instrumentedType, instrumentedMethod);
                    }
                    return new StackManipulation.Compound(stackManipulation);
                }

                @Override
                public String getInternalName() {
                    return internalName;
                }

                @Override
                public TypeDescription getReturnType() {
                    return returnType;
                }

                @Override
                public List<? extends TypeDescription> getParameterTypes() {
                    List<TypeDescription> parameterTypes = new ArrayList<TypeDescription>(argumentProviders.size());
                    for (ArgumentProvider argumentProvider : argumentProviders) {
                        parameterTypes.add(argumentProvider.resolve(instrumentedMethod));
                    }
                    return parameterTypes;
                }
            }
        }
    }

    /**
     * A termination handler is responsible to handle the return value of a method that is invoked via a
     * {@link net.bytebuddy.instrumentation.MethodCall}.
     */
    protected static interface TerminationHandler {

        /**
         * Returns a stack manipulation that handles the method return.
         *
         * @param interceptedMethod The method being intercepted.
         * @return A stack manipulation that handles the method return.
         */
        StackManipulation resolve(MethodDescription interceptedMethod);

        /**
         * Returns the return value if the method call from the intercepted method.
         */
        static enum ForMethodReturn implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription interceptedMethod) {
                return MethodReturn.returning(interceptedMethod.getReturnType());
            }
        }

        /**
         * Drops the return value of the called method from the operand stack without returning from the intercepted
         * method.
         */
        static enum ForChainedInvocation implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription interceptedMethod) {
                return Removal.pop(interceptedMethod.isConstructor()
                        ? interceptedMethod.getDeclaringType()
                        : interceptedMethod.getReturnType());
            }
        }
    }

    // TODO: Add possibility for constant pool storage of wrapper typpes and string?
    // TODO: Add possibility to add instance field or existing field.
}
