package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.*;
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
        for (Object argument : arguments) {
            if (!new TypeDescription.ForLoadedType(argument.getClass()).isConstantPool()) {
                throw new IllegalArgumentException("Not a constant pool value: " + argument);
            }
        }
        List<TypeDescription> parameterTypes = bootstrapMethod.getParameterTypes();
        // The following assumes that the bootstrap method is a valid bootstrap method.
        if (parameterTypes.size() < 3) {
            return arguments.size() == 0 || parameterTypes.get(parameterTypes.size() - 1).represents(Object[].class);
        } else {
            int index = 3;
            Iterator<?> argumentIterator = arguments.iterator();
            for (TypeDescription parameterType : parameterTypes.subList(3, parameterTypes.size())) {
                if (!argumentIterator.hasNext() || !parameterType.isAssignableFrom(argumentIterator.next().getClass())) {
                    return index == parameterTypes.size() && parameterType.represents(Object[].class);
                }
                index++;
            }
            return true;
        }
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
            argumentProviders.add(TargetProvider.ArgumentProvider.ConstantPoolWrapper.of(nonNull(aValue)));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withReference(Object... value) {
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

    public InvokeDynamic withNullValue(Class<?>... type) {
        return withNullValue(new TypeList.ForLoadedType(nonNull(type)).toArray(new TypeDescription[type.length]));
    }

    public InvokeDynamic withNullValue(TypeDescription... typeDescription) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            if (aTypeDescription.isPrimitive()) {
                throw new IllegalArgumentException("Cannot assign null to primitive type: " + aTypeDescription);
            }
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForNullValue(aTypeDescription));
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
            if (anIndex < 0) {
                throw new IllegalArgumentException("Method parameter indices cannot be negative: " + anIndex);
            }
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForMethodParameter(anIndex));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withThis() {
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(Collections.<TargetProvider.ArgumentProvider>singletonList(TargetProvider
                        .ArgumentProvider.ForThisInstance.INSTANCE)),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withInstanceField(String fieldName, Class<?> fieldType) {
        return withInstanceField(fieldName, new TypeDescription.ForLoadedType(nonNull(fieldType)));
    }

    public InvokeDynamic withInstanceField(String fieldName, TypeDescription fieldType) {
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                targetProvider.withArguments(Collections.<TargetProvider.ArgumentProvider>singletonList(new TargetProvider
                        .ArgumentProvider.ForInstanceField(nonNull(fieldName), nonNull(fieldType)))),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    public InvokeDynamic withField(String... fieldName) {
        List<TargetProvider.ArgumentProvider> argumentProviders = new ArrayList<TargetProvider.ArgumentProvider>(fieldName.length);
        for (String aFieldName : fieldName) {
            argumentProviders.add(new TargetProvider.ArgumentProvider.ForExistingField(nonNull(aFieldName)));
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
            TargetProvider.Target.Resolved target = targetProvider.make(instrumentedMethod)
                    .resolve(instrumentedType, assigner, dynamicallyTyped);
            StackManipulation.Size size = new StackManipulation.Compound(
                    target.getStackManipulation(),
                    MethodInvocation.invoke(bootstrapMethod)
                            .dynamic(target.getInternalName(),
                                    target.getReturnType(),
                                    target.getParameterTypes(),
                                    handleArguments),
                    terminationHandler.resolve(instrumentedMethod, target.getReturnType(), assigner, dynamicallyTyped)
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

        public InvokeDynamic withoutArguments() {
            return new InvokeDynamic(bootstrapMethod,
                    handleArguments,
                    new TargetProvider.ForSyntheticCall(TargetProvider.ForSyntheticCall.NameProvider.ForInterceptedMethod.INSTANCE,
                            TargetProvider.ForSyntheticCall.ReturnTypeProvider.ForInterceptedMethod.INSTANCE,
                            Collections.<TargetProvider.ArgumentProvider>emptyList()),
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }
    }

    protected static interface TargetProvider {

        Target make(MethodDescription methodDescription);

        TargetProvider withArguments(List<ForSyntheticCall.ArgumentProvider> argumentProviders);

        InstrumentedType prepare(InstrumentedType instrumentedType);

        static interface Target {

            Resolved resolve(TypeDescription instrumentedType, Assigner assigner, boolean dynamicallyTyped);

            static interface Resolved {

                StackManipulation getStackManipulation();

                TypeDescription getReturnType();

                String getInternalName();

                List<TypeDescription> getParameterTypes();

                static class Simple implements Resolved {

                    private final StackManipulation stackManipulation;

                    private final String internalName;

                    private final TypeDescription returnType;

                    private final List<TypeDescription> parameterTypes;

                    public Simple(StackManipulation stackManipulation,
                                  String internalName,
                                  TypeDescription returnType,
                                  List<TypeDescription> parameterTypes) {
                        this.stackManipulation = stackManipulation;
                        this.internalName = internalName;
                        this.returnType = returnType;
                        this.parameterTypes = parameterTypes;
                    }

                    @Override
                    public StackManipulation getStackManipulation() {
                        return stackManipulation;
                    }

                    @Override
                    public TypeDescription getReturnType() {
                        return returnType;
                    }

                    @Override
                    public String getInternalName() {
                        return internalName;
                    }

                    @Override
                    public List<TypeDescription> getParameterTypes() {
                        return parameterTypes;
                    }
                }
            }

            static class ForMethodDescription implements Target, Target.Resolved {

                private final MethodDescription methodDescription;

                protected ForMethodDescription(MethodDescription methodDescription) {
                    this.methodDescription = methodDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType, Assigner assigner, boolean dynamicallyTyped) {
                    return this;
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
                public StackManipulation getStackManipulation() {
                    return MethodVariableAccess.loadThisReferenceAndArguments(methodDescription);
                }

                @Override
                public List<TypeDescription> getParameterTypes() {
                    return methodDescription.getParameterTypes();
                }
            }
        }

        static interface ArgumentProvider {

            Resolved resolve(TypeDescription instrumentedType,
                             MethodDescription instrumentedMethod,
                             Assigner assigner,
                             boolean dynamicallyTyped);

            InstrumentedType prepare(InstrumentedType instrumentedType);

            static class ForStaticField implements ArgumentProvider {

                private static final String FIELD_PREFIX = "dynamicCall";

                private static final int FIELD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

                private final Object value;

                private final String name;

                public ForStaticField(Object value) {
                    this.value = value;
                    name = String.format("%s$%s", FIELD_PREFIX, RandomString.make());
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    FieldDescription fieldDescription = instrumentedType.getDeclaredFields().filter(named(name)).getOnly();
                    return new Resolved.Simple(FieldAccess.forField(fieldDescription).getter(), fieldDescription.getFieldType());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType
                            .withField(name, new TypeDescription.ForLoadedType(value.getClass()), FIELD_MODIFIER)
                            .withInitializer(LoadedTypeInitializer.ForStaticField.nonAccessible(name, value));
                }
            }

            static class ForInstanceField implements ArgumentProvider {

                private static final int FIELD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

                private final String fieldName;

                private final TypeDescription fieldType;

                public ForInstanceField(String fieldName, TypeDescription fieldType) {
                    this.fieldName = fieldName;
                    this.fieldType = fieldType;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(FieldAccess.forField(instrumentedType.getDeclaredFields()
                            .filter(named(fieldName)).getOnly()).getter(), fieldType);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType.withField(fieldName, fieldType, FIELD_MODIFIER);
                }
            }

            static class ForExistingField implements ArgumentProvider {

                private final String fieldName;

                public ForExistingField(String fieldName) {
                    this.fieldName = fieldName;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    TypeDescription currentType = instrumentedType;
                    FieldDescription fieldDescription = null;
                    do {
                        FieldList fieldList = currentType.getDeclaredFields().filter(named(fieldName));
                        if (fieldList.size() != 0) {
                            fieldDescription = fieldList.getOnly();
                        }
                        currentType = currentType.getSupertype();
                    } while (currentType != null
                            && (fieldDescription == null || !fieldDescription.isVisibleTo(instrumentedType)));
                    if (fieldDescription == null) {
                        throw new IllegalStateException(instrumentedType + " does not define a visible field " + fieldName);
                    } else if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot access non-static " + fieldDescription + " from " + instrumentedMethod);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.LegalTrivial.INSTANCE
                                    : MethodVariableAccess.REFERENCE.loadFromIndex(0),
                            FieldAccess.forField(fieldDescription).getter()
                    ), fieldDescription.getFieldType());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForMethodParameter implements ArgumentProvider {

                private final int index;

                public ForMethodParameter(int index) {
                    this.index = index;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    TypeList parameterTypes = instrumentedMethod.getParameterTypes();
                    if (parameterTypes.size() >= index) {
                        throw new IllegalArgumentException("No parameter " + index + " for " + instrumentedMethod);
                    }
                    return new Resolved.Simple(MethodVariableAccess.forType(parameterTypes.get(index))
                            .loadFromIndex(instrumentedMethod.getParameterOffset(index)), parameterTypes.get(index));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static enum ForThisInstance implements ArgumentProvider {

                INSTANCE;

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot get this instance from static method: " + instrumentedMethod);
                    }
                    return new Resolved.Simple(MethodVariableAccess.REFERENCE.loadFromIndex(0), instrumentedType);
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(boolean.class));
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(byte.class));
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(short.class));
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(char.class));
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(IntegerConstant.forValue(value), new TypeDescription.ForLoadedType(int.class));
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(LongConstant.forValue(value), new TypeDescription.ForLoadedType(long.class));
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(FloatConstant.forValue(value), new TypeDescription.ForLoadedType(float.class));
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
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(DoubleConstant.forValue(value), new TypeDescription.ForLoadedType(double.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForStringValue implements ArgumentProvider {

                private final String value;

                public ForStringValue(String value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(new TextConstant(value), new TypeDescription.ForLoadedType(String.class));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static class ForNullValue implements ArgumentProvider {

                private final TypeDescription typeDescription;

                public ForNullValue(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(NullConstant.INSTANCE, typeDescription);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            }

            static interface Resolved {

                StackManipulation getLoadInstruction();

                TypeDescription getLoadedType();

                static class Simple implements Resolved {

                    private final StackManipulation stackManipulation;

                    private final TypeDescription loadedType;

                    public Simple(StackManipulation stackManipulation, TypeDescription loadedType) {
                        this.stackManipulation = stackManipulation;
                        this.loadedType = loadedType;
                    }

                    @Override
                    public StackManipulation getLoadInstruction() {
                        return stackManipulation;
                    }

                    @Override
                    public TypeDescription getLoadedType() {
                        return loadedType;
                    }
                }
            }

            static enum ConstantPoolWrapper {

                BOOLEAN(boolean.class, Boolean.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Boolean) value));
                    }
                },

                BYTE(byte.class, Byte.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Byte) value));
                    }
                },

                SHORT(short.class, Short.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Short) value));
                    }
                },

                CHARACTER(char.class, Character.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Character) value));
                    }
                },

                INTEGER(int.class, Integer.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Integer) value));
                    }
                },

                LONG(long.class, Long.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(LongConstant.forValue((Long) value));
                    }
                },

                FLOAT(float.class, Float.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(FloatConstant.forValue((Float) value));
                    }
                },

                DOUBLE(double.class, Double.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(DoubleConstant.forValue((Double) value));
                    }
                };

                public static ArgumentProvider of(Object value) {
                    if (value instanceof Boolean) {
                        return BOOLEAN.make(value);
                    } else if (value instanceof Byte) {
                        return BYTE.make(value);
                    } else if (value instanceof Short) {
                        return SHORT.make(value);
                    } else if (value instanceof Character) {
                        return CHARACTER.make(value);
                    } else if (value instanceof Integer) {
                        return INTEGER.make(value);
                    } else if (value instanceof Long) {
                        return LONG.make(value);
                    } else if (value instanceof Float) {
                        return FLOAT.make(value);
                    } else if (value instanceof Double) {
                        return DOUBLE.make(value);
                    } else if (value instanceof String) {
                        return new ForStringValue((String) value);
                    } else {
                        return new ForStaticField(value);
                    }
                }

                private final TypeDescription primitiveType;

                private final TypeDescription wrapperType;

                private ConstantPoolWrapper(Class<?> primitiveType, Class<?> wrapperType) {
                    this.primitiveType = new TypeDescription.ForLoadedType(primitiveType);
                    this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
                }

                protected abstract ArgumentProvider make(Object value);

                protected class WrappingArgumentProvider implements ArgumentProvider {

                    private final StackManipulation stackManipulation;

                    protected WrappingArgumentProvider(StackManipulation stackManipulation) {
                        this.stackManipulation = stackManipulation;
                    }

                    @Override
                    public Resolved resolve(TypeDescription instrumentedType,
                                            MethodDescription instrumentedMethod,
                                            Assigner assigner,
                                            boolean dynamicallyTyped) {
                        return new Resolved.Simple(new StackManipulation.Compound(stackManipulation,
                                assigner.assign(primitiveType, wrapperType, dynamicallyTyped)), wrapperType);
                    }

                    @Override
                    public InstrumentedType prepare(InstrumentedType instrumentedType) {
                        return instrumentedType;
                    }
                }
            }
        }

        static enum ForInterceptedMethod implements TargetProvider {

            INSTANCE;

            @Override
            public Target make(MethodDescription methodDescription) {
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
            public Target make(MethodDescription methodDescription) {
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
                public TargetProvider.Target.Resolved resolve(TypeDescription instrumentedType,
                                                              Assigner assigner,
                                                              boolean dynamicallyTyped) {
                    StackManipulation[] stackManipulation = new StackManipulation[argumentProviders.size()];
                    List<TypeDescription> parameterTypes = new ArrayList<TypeDescription>(argumentProviders.size());
                    int index = 0;
                    for (ArgumentProvider argumentProvider : argumentProviders) {
                        ArgumentProvider.Resolved resolved = argumentProvider.resolve(instrumentedType,
                                instrumentedMethod,
                                assigner,
                                dynamicallyTyped);
                        parameterTypes.add(resolved.getLoadedType());
                        stackManipulation[index++] = resolved.getLoadInstruction();
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(stackManipulation),
                            internalName,
                            returnType,
                            parameterTypes);
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
         * @param returnType
         * @param assigner
         * @param dynamicallyTyped  @return A stack manipulation that handles the method return.
         */
        StackManipulation resolve(MethodDescription interceptedMethod,
                                  TypeDescription returnType,
                                  Assigner assigner,
                                  boolean dynamicallyTyped);

        /**
         * Returns the return value if the method call from the intercepted method.
         */
        static enum ForMethodReturn implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription interceptedMethod,
                                             TypeDescription returnType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = assigner.assign(returnType, interceptedMethod.getReturnType(), dynamicallyTyped);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot return " + returnType + " from " + interceptedMethod);
                }
                return new StackManipulation.Compound(stackManipulation, MethodReturn.returning(interceptedMethod.getReturnType()));
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
            public StackManipulation resolve(MethodDescription interceptedMethod,
                                             TypeDescription returnType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                return Removal.pop(interceptedMethod.isConstructor()
                        ? interceptedMethod.getDeclaringType()
                        : interceptedMethod.getReturnType());
            }
        }
    }
}
