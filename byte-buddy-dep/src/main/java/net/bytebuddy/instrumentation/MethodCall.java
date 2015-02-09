package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * This {@link net.bytebuddy.instrumentation.Instrumentation} allows the invocation of a specified method while
 * providing explicit arguments to this method.
 */
public class MethodCall implements Instrumentation {

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

    /**
     * Invokes the given method. Without further specification, the method is invoked without any arguments on
     * the instance of the instrument class or statically, if the given method is {@code static}.
     *
     * @param method The method to invoke.
     * @return A method call instrumentation that invokes the given method without providing any arguments.
     */
    public static MethodCall.WithoutSpecifiedTarget invoke(Method method) {
        return invoke(new MethodDescription.ForLoadedMethod(nonNull(method)));
    }

    /**
     * Invokes the given constructor on the instance of the instrumented type.
     *
     * @param constructor The constructor to invoke.
     * @return A method call instrumentation that invokes the given constructor without providing any arguments.
     */
    public static MethodCall.WithoutSpecifiedTarget invoke(Constructor<?> constructor) {
        return invoke(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
    }

    /**
     * Invokes the given method. If the method description describes a constructor, it is automatically invoked as
     * a special method invocation on the instance of the instrumented type. The same is true for {@code private}
     * methods. Finally, {@code static} methods are invoked statically.
     *
     * @param methodDescription The method to invoke.
     * @return A method call instrumentation that invokes the given method without providing any arguments.
     */
    public static MethodCall.WithoutSpecifiedTarget invoke(MethodDescription methodDescription) {
        return new WithoutSpecifiedTarget(new MethodLocator.ForExplicitMethod(nonNull(methodDescription)));
    }

    /**
     * Invokes the given constructor in order to create an instance.
     *
     * @param constructor The constructor to invoke.
     * @return A method call that invokes the given constructor without providing any arguments.
     */
    public static MethodCall construct(Constructor<?> constructor) {
        return construct(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
    }

    /**
     * Invokes the given constructor in order to create an instance.
     *
     * @param methodDescription A description of the constructor to invoke.
     * @return A method call that invokes the given constructor without providing any arguments.
     */
    public static MethodCall construct(MethodDescription methodDescription) {
        if (!methodDescription.isConstructor()) {
            throw new IllegalArgumentException("Not a constructor: " + methodDescription);
        }
        return new MethodCall(new MethodLocator.ForExplicitMethod(methodDescription),
                TargetHandler.ForConstructingInvocation.INSTANCE,
                Collections.<ArgumentLoader>emptyList(),
                MethodInvoker.ForStandardInvocation.INSTANCE,
                TerminationHandler.ForMethodReturn.INSTANCE,
                defaultAssigner(),
                defaultDynamicallyTyped());
    }

    /**
     * Invokes the method that is instrumented by the returned instance.
     *
     * @return A method call that invokes the method being instrumented.
     */
    public static MethodCall.WithoutSpecifiedTarget invokeSelf() {
        return new WithoutSpecifiedTarget(MethodLocator.ForInterceptedMethod.INSTANCE);
    }

    /**
     * The method locator to use.
     */
    protected final MethodLocator methodLocator;

    /**
     * The target handler to use.
     */
    protected final TargetHandler targetHandler;

    /**
     * The argument loader to load arguments onto the operand stack in their application order.
     */
    protected final List<ArgumentLoader> argumentLoaders;

    /**
     * The method invoker to use.
     */
    protected final MethodInvoker methodInvoker;

    /**
     * The termination handler to use.
     */
    protected final TerminationHandler terminationHandler;

    /**
     * The assigner to use.
     */
    protected final Assigner assigner;

    /**
     * {@code true} if a return value of the called method should be attempted to be type-casted to the return
     * type of the instrumented method.
     */
    protected final boolean dynamicallyTyped;

    /**
     * Creates a new method call instrumentation.
     *
     * @param methodLocator      The method locator to use.
     * @param targetHandler      The target handler to use.
     * @param argumentLoaders    The argument loader to load arguments onto the operand stack in
     *                           their application order.
     * @param methodInvoker      The method invoker to use.
     * @param terminationHandler The termination handler to use.
     * @param assigner           The assigner to use.
     * @param dynamicallyTyped   {@code true} if a return value of the called method should be attempted
     *                           to be type-casted to the return type of the instrumented method.
     */
    protected MethodCall(MethodLocator methodLocator,
                         TargetHandler targetHandler,
                         List<ArgumentLoader> argumentLoaders,
                         MethodInvoker methodInvoker,
                         TerminationHandler terminationHandler,
                         Assigner assigner,
                         boolean dynamicallyTyped) {
        this.methodLocator = methodLocator;
        this.targetHandler = targetHandler;
        this.argumentLoaders = argumentLoaders;
        this.methodInvoker = methodInvoker;
        this.terminationHandler = terminationHandler;
        this.assigner = assigner;
        this.dynamicallyTyped = dynamicallyTyped;
    }

    /**
     * Defines a number of arguments to be handed to the method that is being invoked by this instrumentation. Any
     * wrapper type instances for primitive values, instances of {@link java.lang.String} or {@code null} are loaded
     * directly onto the operand stack. This might corrupt referential identity for these values. Any other values
     * are stored within a {@code static} field that is added to the instrumented type.
     *
     * @param argument The arguments to provide to the method that is being called in their order.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall with(Object... argument) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(argument.length);
        for (Object anArgument : argument) {
            argumentLoaders.add(ArgumentLoader.ForStaticFieldValue.of(anArgument));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Defines a number of arguments to be handed to the method that is being invoked by this instrumentation. Any
     * value is stored within a field in order to preserve referential identity. As an exception, the {@code null}
     * value is not stored within a field.
     *
     * @param argument The arguments to provide to the method that is being called in their order.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall withReference(Object... argument) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(argument.length);
        for (Object anArgument : argument) {
            argumentLoaders.add(anArgument == null
                    ? ArgumentLoader.ForNullConstant.INSTANCE
                    : new ArgumentLoader.ForStaticFieldValue(anArgument));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Defines a number of arguments of the instrumented method by their parameter indices to be handed
     * to the invoked method as an argument.
     *
     * @param index The parameter indices of the instrumented method to be handed to the invoked method as an
     *              argument in their order.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall withArgument(int... index) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(index.length);
        for (int anIndex : index) {
            argumentLoaders.add(new ArgumentLoader.ForMethodParameter(anIndex));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Defines a method call which fetches a value from an instance field. The value of the field needs to be
     * defined manually and is initialized with {@code null}.
     *
     * @param type The type of the field.
     * @param name The name of the field.
     * @return A method call which assigns the next parameter to the value of the instance field.
     */
    public MethodCall withInstanceField(Class<?> type, String name) {
        return withInstanceField(new TypeDescription.ForLoadedType(nonNull(type)), name);
    }

    /**
     * Defines a method call which fetches a value from an instance field. The value of the field needs to be
     * defined manually and is initialized with {@code null}.
     *
     * @param typeDescription The type of the field.
     * @param name            The name of the field.
     * @return A method call which assigns the next parameter to the value of the instance field.
     */
    public MethodCall withInstanceField(TypeDescription typeDescription, String name) {
        return new MethodCall(methodLocator,
                targetHandler,
                join(argumentLoaders, new ArgumentLoader.ForInstanceFieldValue(nonNull(typeDescription), nonNull(name))),
                methodInvoker,
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Defines an assigner to be used for assigning values to the parameters of the invoked method. This assigner
     * is also used for assigning the invoked method's return value to the return type of the instrumented method,
     * if this method is not chained with
     * {@link net.bytebuddy.instrumentation.MethodCall#andThen(net.bytebuddy.instrumentation.Instrumentation)} such
     * that a return value of this method call is discarded.
     *
     * @param assigner         The assigner to use.
     * @param dynamicallyTyped {@code true} if the return value assignment should attempt a type casting when
     *                         assigning incompatible values.
     * @return This method call using the provided assigner.
     */
    public MethodCall withAssigner(Assigner assigner, boolean dynamicallyTyped) {
        return new MethodCall(methodLocator,
                targetHandler,
                argumentLoaders,
                methodInvoker,
                terminationHandler,
                nonNull(assigner),
                dynamicallyTyped);
    }

    /**
     * Applies another instrumentation after invoking this method call. A return value that is the result of this
     * method call is dropped.
     *
     * @param instrumentation The instrumentation that is to be applied after applying this
     *                        method call instrumentation.
     * @return An instrumentation that first applies this method call and the given instrumentation right after.
     */
    public Instrumentation andThen(Instrumentation instrumentation) {
        return new Instrumentation.Compound(new MethodCall(methodLocator,
                targetHandler,
                argumentLoaders,
                methodInvoker,
                TerminationHandler.ForChainedInvocation.INSTANCE,
                assigner,
                dynamicallyTyped), nonNull(instrumentation));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        for (ArgumentLoader argumentLoader : argumentLoaders) {
            instrumentedType = argumentLoader.prepare(instrumentedType);
        }
        return targetHandler.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target instrumentationTarget) {
        return new Appender(instrumentationTarget);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MethodCall)) return false;
        MethodCall that = (MethodCall) other;
        return dynamicallyTyped == that.dynamicallyTyped
                && argumentLoaders.equals(that.argumentLoaders)
                && assigner.equals(that.assigner)
                && methodInvoker.equals(that.methodInvoker)
                && methodLocator.equals(that.methodLocator)
                && targetHandler.equals(that.targetHandler)
                && terminationHandler.equals(that.terminationHandler);
    }

    @Override
    public int hashCode() {
        int result = methodLocator.hashCode();
        result = 31 * result + targetHandler.hashCode();
        result = 31 * result + argumentLoaders.hashCode();
        result = 31 * result + methodInvoker.hashCode();
        result = 31 * result + terminationHandler.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + (dynamicallyTyped ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MethodCall{" +
                "methodLocator=" + methodLocator +
                ", targetHandler=" + targetHandler +
                ", argumentLoaders=" + argumentLoaders +
                ", methodInvoker=" + methodInvoker +
                ", terminationHandler=" + terminationHandler +
                ", assigner=" + assigner +
                ", dynamicallyTyped=" + dynamicallyTyped +
                '}';
    }

    /**
     * The appender being used to implement a {@link net.bytebuddy.instrumentation.MethodCall}.
     */
    protected class Appender implements ByteCodeAppender {

        /**
         * The instrumentation target of the current instrumentation.
         */
        private final Target instrumentationTarget;

        /**
         * Creates a new appender.
         *
         * @param instrumentationTarget The instrumentation target of the current instrumentation.
         */
        protected Appender(Target instrumentationTarget) {
            this.instrumentationTarget = instrumentationTarget;
        }

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            MethodDescription invokedMethod = methodLocator.resolve(instrumentedMethod);
            TypeList methodParameters = invokedMethod.getParameterTypes();
            if (methodParameters.size() != argumentLoaders.size()) {
                throw new IllegalStateException(invokedMethod + " does not take "
                        + argumentLoaders.size() + " arguments");
            }
            int index = 0;
            StackManipulation[] argumentInstruction = new StackManipulation[argumentLoaders.size()];
            for (ArgumentLoader argumentLoader : argumentLoaders) {
                argumentInstruction[index] = argumentLoader.resolve(instrumentationTarget.getTypeDescription(),
                        invokedMethod,
                        methodParameters.get(index++),
                        assigner,
                        dynamicallyTyped);
            }
            StackManipulation.Size size = new StackManipulation.Compound(
                    targetHandler.resolve(invokedMethod, instrumentationTarget.getTypeDescription()),
                    new StackManipulation.Compound(argumentInstruction),
                    methodInvoker.invoke(invokedMethod, instrumentationTarget),
                    terminationHandler.resolve(invokedMethod, instrumentedMethod, assigner, dynamicallyTyped)
            ).apply(methodVisitor, instrumentationContext);
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private MethodCall getOuter() {
            return MethodCall.this;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null || getClass() != other.getClass())
                return false;
            Appender appender = (Appender) other;
            return instrumentationTarget.equals(appender.instrumentationTarget)
                    && MethodCall.this.equals(appender.getOuter());

        }

        @Override
        public int hashCode() {
            return instrumentationTarget.hashCode() + 31 * MethodCall.this.hashCode();
        }

        @Override
        public String toString() {
            return "MethodCall.Appender{" +
                    "methodCall=" + MethodCall.this +
                    ", instrumentationTarget=" + instrumentationTarget +
                    '}';
        }
    }

    /**
     * Represents a {@link net.bytebuddy.instrumentation.MethodCall} that invokes a method without specifying
     * an invocation method. Some methods can for example be invoked both virtually or as a super method invocation.
     * Similarly, interface methods can be invoked virtually or as an explicit invocation of a default method. If
     * no explicit invocation type is set, a method is always invoked virtually unless the method
     * represents a static methods or a constructor.
     */
    public static class WithoutSpecifiedTarget extends MethodCall {

        /**
         * Creates a new method call without a specified target.
         *
         * @param methodLocator The method locator to use.
         */
        protected WithoutSpecifiedTarget(MethodLocator methodLocator) {
            super(methodLocator,
                    TargetHandler.ForSelfOrStaticInvocation.INSTANCE,
                    Collections.<ArgumentLoader>emptyList(),
                    MethodInvoker.ForStandardInvocation.INSTANCE,
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    defaultAssigner(),
                    defaultDynamicallyTyped());
        }

        /**
         * Invokes the specified method on the given instance.
         *
         * @param target The object on which the method is to be invoked upon.
         * @return A method call that invokes the provided method on the given object.
         */
        public MethodCall on(Object target) {
            return new MethodCall(methodLocator,
                    new TargetHandler.ForStaticField(nonNull(target)),
                    argumentLoaders,
                    MethodInvoker.ForStandardInvocation.INSTANCE,
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    assigner,
                    dynamicallyTyped);
        }

        /**
         * Invokes the given method on an instance that is stored in an instance field. This field's value needs
         * to be set by the user such that the method call does not throw a {@link java.lang.NullPointerException}.
         *
         * @param type      The type of the field.
         * @param fieldName The name of the field.
         * @return A method call that invokes the given method on an instance that is read from an instance field.
         */
        public MethodCall onInstanceField(Class<?> type, String fieldName) {
            return onInstanceField(new TypeDescription.ForLoadedType(nonNull(type)), nonNull(fieldName));
        }

        /**
         * Invokes the given method on an instance that is stored in an instance field. This field's value needs
         * to be set by the user such that the method call does not throw a {@link java.lang.NullPointerException}.
         *
         * @param typeDescription The type of the field.
         * @param fieldName       The name of the field.
         * @return A method call that invokes the given method on an instance that is read from an instance field.
         */
        public MethodCall onInstanceField(TypeDescription typeDescription, String fieldName) {
            return new MethodCall(methodLocator,
                    new TargetHandler.ForInstanceField(nonNull(fieldName), nonVoid(typeDescription)),
                    argumentLoaders,
                    MethodInvoker.ForStandardInvocation.INSTANCE,
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    assigner,
                    dynamicallyTyped);
        }

        /**
         * Invokes the given method by a super method invocation on the instance of the instrumented type.
         * Note that
         *
         * @return A method call where the given method is invoked as a super method invocation.
         */
        public MethodCall onSuper() {
            return new MethodCall(methodLocator,
                    TargetHandler.ForSelfOrStaticInvocation.INSTANCE,
                    argumentLoaders,
                    MethodInvoker.ForSuperMethodInvocation.INSTANCE,
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    assigner,
                    dynamicallyTyped);
        }

        public MethodCall onDefault() {
            return new MethodCall(methodLocator,
                    TargetHandler.ForSelfOrStaticInvocation.INSTANCE,
                    argumentLoaders,
                    MethodInvoker.ForDefaultMethodInvocation.INSTANCE,
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    assigner,
                    dynamicallyTyped);
        }

        @Override
        public String toString() {
            return "MethodCall.WithoutSpecifiedTarget{" +
                    "methodLocator=" + methodLocator +
                    ", targetHandler=" + targetHandler +
                    ", argumentLoaders=" + argumentLoaders +
                    ", methodInvoker=" + methodInvoker +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", dynamicallyTyped=" + dynamicallyTyped +
                    '}';
        }
    }

    /**
     * A method locator is responsible for identifying the method that is to be invoked
     * by a {@link net.bytebuddy.instrumentation.MethodCall}.
     */
    protected static interface MethodLocator {

        /**
         * Resolves the method to be invoked.
         *
         * @param instrumentedMethod The method being instrumented.
         * @return The method to invoke.
         */
        MethodDescription resolve(MethodDescription instrumentedMethod);

        /**
         * Invokes a given method.
         */
        static class ForExplicitMethod implements MethodLocator {

            /**
             * The method to be invoked.
             */
            private final MethodDescription methodDescription;

            /**
             * Creates a new method locator for a given method.
             *
             * @param methodDescription The method to be invoked.
             */
            public ForExplicitMethod(MethodDescription methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public MethodDescription resolve(MethodDescription instrumentedMethod) {
                return methodDescription;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodDescription.equals(((ForExplicitMethod) other).methodDescription);
            }

            @Override
            public int hashCode() {
                return methodDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.MethodLocator.ForExplicitMethod{" +
                        "methodDescription=" + methodDescription +
                        '}';
            }
        }

        /**
         * A method locator that simply returns the intercepted method.
         */
        static enum ForInterceptedMethod implements MethodLocator {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodDescription resolve(MethodDescription instrumentedMethod) {
                return instrumentedMethod;
            }
        }
    }

    /**
     * A target handler is responsible for invoking a method for a
     * {@link net.bytebuddy.instrumentation.MethodCall}.
     */
    protected static interface TargetHandler {

        /**
         * Creates a stack manipulation that represents the method's invocation.
         *
         * @param methodDescription The method to be invoked.
         * @param instrumentedType  The instrumented type.
         * @return A stack manipulation that invokes the method.
         */
        StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType);

        /**
         * Prepares the instrumented type in order to allow for the represented invocation.
         *
         * @param instrumentedType The instrumented type.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * A target handler that invokes a method on an instance that is stored in a static field.
         */
        static class ForStaticField implements TargetHandler {

            /**
             * The name prefix of the field to store the instance.
             */
            private static final String FIELD_PREFIX = "invocationTarget";

            /**
             * The modifiers of the static field to store the instance.
             */
            private static final int FIELD_MODIFIERS = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

            /**
             * The target on which the method is to be invoked.
             */
            private final Object target;

            /**
             * The name of the field to store the target.
             */
            private final String fieldName;

            /**
             * Creares a new target handler for a static field.
             *
             * @param target The target on which the method is to be invoked.
             */
            public ForStaticField(Object target) {
                this.target = target;
                fieldName = String.format("%s$%s", FIELD_PREFIX, RandomString.make());
            }

            @Override
            public StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType) {
                if (methodDescription.isStatic() || !methodDescription.getDeclaringType().isInstance(target)) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " on " + target);
                }
                return FieldAccess.forField(instrumentedType.getDeclaredFields()
                        .filter(named(fieldName)).getOnly()).getter();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(fieldName, new TypeDescription.ForLoadedType(target.getClass()), FIELD_MODIFIERS)
                        .withInitializer(LoadedTypeInitializer.ForStaticField.nonAccessible(fieldName, target));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && target.equals(((ForStaticField) other).target);
            }

            @Override
            public int hashCode() {
                return target.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.TargetHandler.ForStaticField{" +
                        "target=" + target +
                        ", fieldName='" + fieldName + '\'' +
                        '}';
            }
        }

        /**
         * Creates a target handler that stores the instance to invoke a method on in an instance field.
         */
        static class ForInstanceField implements TargetHandler {

            /**
             * The modifiers of the field.
             */
            private static final int FIELD_MODIFIERS = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

            /**
             * The name of the field.
             */
            private final String fieldName;

            /**
             * The type of the field.
             */
            private final TypeDescription fieldType;

            /**
             * Creates a new target handler for storing a method invocation target in an
             * instance field.
             *
             * @param fieldName The name of the field.
             * @param fieldType The type of the field.
             */
            public ForInstanceField(String fieldName, TypeDescription fieldType) {
                this.fieldName = fieldName;
                this.fieldType = fieldType;
            }

            @Override
            public StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType) {
                if (methodDescription.isStatic() || !methodDescription.isInvokableOn(fieldType)) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " on " + fieldType);
                }
                return FieldAccess.forField(instrumentedType.getDeclaredFields()
                        .filter(named(fieldName)).getOnly()).getter();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(fieldName, fieldType, FIELD_MODIFIERS);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass()) && fieldName
                        .equals(((ForInstanceField) other).fieldName) && fieldType
                        .equals(((ForInstanceField) other).fieldType);
            }

            @Override
            public int hashCode() {
                int result = fieldName.hashCode();
                result = 31 * result + fieldType.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodCall.TargetHandler.ForInstanceField{" +
                        "fieldName='" + fieldName + '\'' +
                        ", fieldType=" + fieldType +
                        '}';
            }
        }

        /**
         * A target handler that invokes a method either on the instance of the instrumented
         * type or as a static method.
         */
        static enum ForSelfOrStaticInvocation implements TargetHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType) {
                return methodDescription.isStatic()
                        ? StackManipulation.LegalTrivial.INSTANCE
                        : MethodVariableAccess.REFERENCE.loadFromIndex(0);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }
        }

        /**
         * Invokes a method in order to construct a new instance.
         */
        static enum ForConstructingInvocation implements TargetHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType) {
                return TypeCreation.forType(methodDescription.getDeclaringType());
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }
        }
    }

    /**
     * An argument loader is responsible for loading an argument for an invoked method
     * onto the operand stack.
     */
    protected static interface ArgumentLoader {

        /**
         * Loads the argument that is represented by this instance onto the operand stack.
         *
         * @param instrumentedType  The instrumented type.
         * @param interceptedMethod The method being intercepted.
         * @param targetType        The target type.
         * @param assigner          The assigner to be used.
         * @param dynamicallyTyped  {@code true} if an assignment should be consider type castings.
         * @return The stack manipulation that loads the represented argument onto the stack.
         */
        StackManipulation resolve(TypeDescription instrumentedType,
                                  MethodDescription interceptedMethod,
                                  TypeDescription targetType,
                                  Assigner assigner,
                                  boolean dynamicallyTyped);

        /**
         * Prepares the instrumented type in order to allow the loading of the represented argument.
         *
         * @param instrumentedType The instrumented type.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * Loads a parameter of the instrumented method onto the operand stack.
         */
        static class ForMethodParameter implements ArgumentLoader {

            /**
             * The index of the parameter to be loaded onto the operand stack.
             */
            private final int index;

            /**
             * Creates an argument loader for a parameter of the instrumented method.
             *
             * @param index The index of the parameter to be loaded onto the operand stack.
             */
            public ForMethodParameter(int index) {
                this.index = index;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                if (interceptedMethod.getParameterTypes().size() < index) {
                    throw new IllegalStateException(interceptedMethod + " does not have a parameter " + index);
                }
                TypeDescription originType = interceptedMethod.getParameterTypes().get(index);
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.forType(originType).loadFromIndex(index),
                        assigner.assign(originType, targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + originType + " to " + targetType
                            + " for " + interceptedMethod);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && index == ((ForMethodParameter) other).index;
            }

            @Override
            public int hashCode() {
                return index;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForMethodParameter{" +
                        "index=" + index +
                        '}';
            }
        }

        /**
         * Loads a value onto the operand stack that is stored in a static field.
         */
        static class ForStaticFieldValue implements ArgumentLoader {

            /**
             * The name prefix of the field to store the argument.
             */
            private static final String FIELD_PREFIX = "methodCall";

            /**
             * The modifier of the field.
             */
            private static final int FIELD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;

            /**
             * The value to be stored in the field.
             */
            private final Object value;

            /**
             * The name of the field.
             */
            private final String fieldName;

            /**
             * Resolves a value to be stored to be stored in the constant pool of the class, if possible.
             *
             * @param value The value to be stored in the field.
             * @return An argument loader that loads the given value onto the operand stack.
             */
            public static ArgumentLoader of(Object value) {
                if (value == null) {
                    return ForNullConstant.INSTANCE;
                } else if (value.getClass() == String.class) {
                    return new ForTextConstant((String) value);
                } else if (value.getClass() == Boolean.class) {
                    return new ForBooleanConstant((Boolean) value);
                } else if (value.getClass() == Byte.class) {
                    return new ForByteConstant((Byte) value);
                } else if (value.getClass() == Short.class) {
                    return new ForShortConstant((Short) value);
                } else if (value.getClass() == Character.class) {
                    return new ForCharacterConstant((Character) value);
                } else if (value.getClass() == Integer.class) {
                    return new ForIntegerConstant((Integer) value);
                } else if (value.getClass() == Long.class) {
                    return new ForLongConstant((Long) value);
                } else if (value.getClass() == Float.class) {
                    return new ForFloatConstant((Float) value);
                } else if (value.getClass() == Double.class) {
                    return new ForDoubleConstant((Double) value);
                } else {
                    return new ForStaticFieldValue(value);
                }
            }

            /**
             * Creates a new argument loader that stores the value in a field.
             *
             * @param value The value to be stored and loaded onto the operand stack.
             */
            protected ForStaticFieldValue(Object value) {
                this.value = value;
                fieldName = String.format("%s$%s", FIELD_PREFIX, RandomString.make());
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter(),
                        assigner.assign(new TypeDescription.ForLoadedType(value.getClass()), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + value.getClass() + " to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(fieldName,
                        new TypeDescription.ForLoadedType(value.getClass()),
                        FIELD_MODIFIER)
                        .withInitializer(new LoadedTypeInitializer.ForStaticField<Object>(fieldName, value, true));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value.equals(((ForStaticFieldValue) other).value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForStaticFieldValue{" +
                        "value=" + value +
                        ", fieldName='" + fieldName + '\'' +
                        '}';
            }
        }

        /**
         * Loads a value onto the operand stack that is stored in an instance field.
         */
        static class ForInstanceFieldValue implements ArgumentLoader {

            /**
             * The modifier to be applied to the instance field.
             */
            private static final int MODIFIERS = Opcodes.ACC_PRIVATE;

            /**
             * The type of the field.
             */
            private final TypeDescription fieldType;

            /**
             * The name of the field.
             */
            private final String fieldName;

            /**
             * Creates a new argument loader for a value of an instance field.
             *
             * @param fieldType The name of the field.
             * @param fieldName The type of the field.
             */
            public ForInstanceFieldValue(TypeDescription fieldType, String fieldName) {
                this.fieldType = fieldType;
                this.fieldName = fieldName;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter(),
                        assigner.assign(fieldType, targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign field " + fieldName + " of type "
                            + fieldType + " to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(fieldName, fieldType, MODIFIERS);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForInstanceFieldValue that = (ForInstanceFieldValue) other;
                return fieldName.equals(that.fieldName) && fieldType.equals(that.fieldType);
            }

            @Override
            public int hashCode() {
                int result = fieldType.hashCode();
                result = 31 * result + fieldName.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForInstanceFieldValue{" +
                        "fieldType=" + fieldType +
                        ", fieldName='" + fieldName + '\'' +
                        '}';
            }
        }

        /**
         * An argument loader that loads the {@code null} value onto the operand stack.
         */
        static enum ForNullConstant implements ArgumentLoader {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                if (targetType.isPrimitive()) {
                    throw new IllegalStateException("Cannot assign null to " + targetType);
                }
                return NullConstant.INSTANCE;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }
        }

        /**
         * Loads a {@code boolean} value onto the operand stack.
         */
        static class ForBooleanConstant implements ArgumentLoader {

            /**
             * The {@code boolean} value to load onto the operand stack.
             */
            private final boolean value;

            /**
             * Creates a new argument loader for a {@code boolean} value.
             *
             * @param value The {@code boolean} value to load onto the operand stack.
             */
            public ForBooleanConstant(boolean value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(boolean.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign boolean value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value == ((ForBooleanConstant) other).value;
            }

            @Override
            public int hashCode() {
                return (value ? 1 : 0);
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForBooleanConstant{value=" + value + '}';
            }
        }

        /**
         * Loads a {@code byte} value onto the operand stack.
         */
        static class ForByteConstant implements ArgumentLoader {

            /**
             * The {@code boolean} value to load onto the operand stack.
             */
            private final byte value;

            /**
             * Creates a new argument loader for a {@code boolean} value.
             *
             * @param value The {@code boolean} value to load onto the operand stack.
             */
            public ForByteConstant(byte value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(byte.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign byte value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value == ((ForByteConstant) other).value;
            }

            @Override
            public int hashCode() {
                return value;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForByteConstant{value=" + value + '}';
            }
        }

        /**
         * Loads a {@code short} value onto the operand stack.
         */
        static class ForShortConstant implements ArgumentLoader {

            /**
             * The {@code short} value to load onto the operand stack.
             */
            private final short value;

            /**
             * Creates a new argument loader for a {@code short} value.
             *
             * @param value The {@code short} value to load onto the operand stack.
             */
            public ForShortConstant(short value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(short.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign short value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value == ((ForShortConstant) other).value;
            }

            @Override
            public int hashCode() {
                return value;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForShortConstant{value=" + value + '}';
            }
        }

        /**
         * Loads a {@code char} value onto the operand stack.
         */
        static class ForCharacterConstant implements ArgumentLoader {

            /**
             * The {@code char} value to load onto the operand stack.
             */
            private final char value;

            /**
             * Creates a new argument loader for a {@code char} value.
             *
             * @param value The {@code char} value to load onto the operand stack.
             */
            public ForCharacterConstant(char value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(char.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign char value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value == ((ForCharacterConstant) other).value;
            }

            @Override
            public int hashCode() {
                return value;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForCharacterConstant{value=" + value + '}';
            }
        }

        /**
         * Loads an {@code int} value onto the operand stack.
         */
        static class ForIntegerConstant implements ArgumentLoader {

            /**
             * The {@code int} value to load onto the operand stack.
             */
            private final int value;

            /**
             * Creates a new argument loader for a {@code int} value.
             *
             * @param value The {@code int} value to load onto the operand stack.
             */
            public ForIntegerConstant(int value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(int.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign integer value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value == ((ForIntegerConstant) other).value;
            }

            @Override
            public int hashCode() {
                return value;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForIntegerConstant{value=" + value + '}';
            }
        }

        /**
         * Loads a {@code long} value onto the operand stack.
         */
        static class ForLongConstant implements ArgumentLoader {

            /**
             * The {@code long} value to load onto the operand stack.
             */
            private final long value;

            /**
             * Creates a new argument loader for a {@code long} value.
             *
             * @param value The {@code long} value to load onto the operand stack.
             */
            public ForLongConstant(long value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        LongConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(long.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign long value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value == ((ForLongConstant) other).value;
            }

            @Override
            public int hashCode() {
                return (int) (value ^ (value >>> 32));
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForLongConstant{" +
                        "value=" + value +
                        '}';
            }
        }

        /**
         * Loads a {@code float} value onto the operand stack.
         */
        static class ForFloatConstant implements ArgumentLoader {

            /**
             * The {@code float} value to load onto the operand stack.
             */
            private final float value;

            /**
             * Creates a new argument loader for a {@code float} value.
             *
             * @param value The {@code float} value to load onto the operand stack.
             */
            public ForFloatConstant(float value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        FloatConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(float.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign float value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Float.compare(((ForFloatConstant) other).value, value) == 0;
            }

            @Override
            public int hashCode() {
                return (value != +0.0f ? Float.floatToIntBits(value) : 0);
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForFloatConstant{value=" + value + '}';
            }
        }

        /**
         * Loads a {@code double} value onto the operand stack.
         */
        static class ForDoubleConstant implements ArgumentLoader {

            /**
             * The {@code double} value to load onto the operand stack.
             */
            private final double value;

            /**
             * Creates a new argument loader for a {@code double} value.
             *
             * @param value The {@code double} value to load onto the operand stack.
             */
            public ForDoubleConstant(double value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        DoubleConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(double.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign double value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Double.compare(((ForDoubleConstant) other).value, value) == 0;
            }

            @Override
            public int hashCode() {
                long temp = Double.doubleToLongBits(value);
                return (int) (temp ^ (temp >>> 32));
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForDoubleConstant{value=" + value + '}';
            }
        }

        /**
         * Loads a {@link java.lang.String} value onto the operand stack.
         */
        static class ForTextConstant implements ArgumentLoader {

            /**
             * The {@link java.lang.String} value to load onto the operand stack.
             */
            private final String value;

            /**
             * Creates a new argument loader for a {@link java.lang.String} value.
             *
             * @param value The {@link java.lang.String} value to load onto the operand stack.
             */
            public ForTextConstant(String value) {
                this.value = value;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        new TextConstant(value),
                        assigner.assign(new TypeDescription.ForLoadedType(String.class), targetType, dynamicallyTyped));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign String value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value.equals(((ForTextConstant) other).value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForTextConstant{" +
                        "value='" + value + '\'' +
                        '}';
            }
        }
    }

    /**
     * A method invoker is responsible for creating a method invokation that is to be applied by a
     * {@link net.bytebuddy.instrumentation.MethodCall}.
     */
    protected static interface MethodInvoker {

        /**
         * Invokes the method.
         *
         * @param methodDescription     The method to be invoked.
         * @param instrumentationTarget The instrumentation target of the instrumented instance.
         * @return A stack manipulation that represents the method invocation.
         */
        StackManipulation invoke(MethodDescription methodDescription, Target instrumentationTarget);

        /**
         * Applies a standard invocation of the provided method, i.e. a static invocation for static methods,
         * a special invocation for constructors and private methods and a virtual invocation for any other method.
         */
        static enum ForStandardInvocation implements MethodInvoker {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation invoke(MethodDescription methodDescription, Target instrumentationTarget) {
                if (!methodDescription.isStatic()
                        && !methodDescription.isInvokableOn(instrumentationTarget.getTypeDescription())) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription
                            + " for " + instrumentationTarget);
                }
                return MethodInvocation.invoke(methodDescription);
            }
        }

        /**
         * Applies a super method invocation of the provided method.
         */
        static enum ForSuperMethodInvocation implements MethodInvoker {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation invoke(MethodDescription methodDescription, Target instrumentationTarget) {
                StackManipulation stackManipulation = instrumentationTarget.invokeSuper(methodDescription,
                        Target.MethodLookup.Default.EXACT);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " as a super method");
                }
                return stackManipulation;
            }
        }

        /**
         * Invokes a method as a Java 8 default method.
         */
        static enum ForDefaultMethodInvocation implements MethodInvoker {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation invoke(MethodDescription methodDescription, Target instrumentationTarget) {
                if (!methodDescription.isDefaultMethod()) {
                    throw new IllegalStateException("Not a default method: " + methodDescription);
                }
                StackManipulation stackManipulation = instrumentationTarget.invokeDefault(methodDescription
                        .getDeclaringType(), methodDescription.getUniqueSignature());
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " on " + instrumentationTarget);
                }
                return stackManipulation;
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
         * @param invokedMethod     The method that was invoked by the method call.
         * @param interceptedMethod The method being intercepted.
         * @param assigner          The assigner to be used.
         * @param dynamicallyTyped  {@code true} if type-castings should be applied.
         * @return A stack manipulation that handles the method return.
         */
        StackManipulation resolve(MethodDescription invokedMethod,
                                  MethodDescription interceptedMethod,
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
            public StackManipulation resolve(MethodDescription invokedMethod,
                                             MethodDescription interceptedMethod,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                StackManipulation stackManipulation = assigner.assign(invokedMethod.getReturnType(),
                        interceptedMethod.getReturnType(),
                        dynamicallyTyped);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot return " + invokedMethod.getReturnType()
                            + " from " + interceptedMethod);
                }
                return new StackManipulation.Compound(stackManipulation,
                        MethodReturn.returning(interceptedMethod.getReturnType()));
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
            public StackManipulation resolve(MethodDescription invokedMethod,
                                             MethodDescription interceptedMethod,
                                             Assigner assigner,
                                             boolean dynamicallyTyped) {
                return Removal.pop(invokedMethod.getReturnType());
            }
        }
    }
}
