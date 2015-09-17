package net.bytebuddy.implementation;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.JavaInstance;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * This {@link Implementation} allows the invocation of a specified method while
 * providing explicit arguments to this method.
 */
public class MethodCall implements Implementation {

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
     * Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected final Assigner.Typing typing;

    /**
     * Creates a new method call implementation.
     *
     * @param methodLocator      The method locator to use.
     * @param targetHandler      The target handler to use.
     * @param argumentLoaders    The argument loader to load arguments onto the operand stack in
     *                           their application order.
     * @param methodInvoker      The method invoker to use.
     * @param terminationHandler The termination handler to use.
     * @param assigner           The assigner to use.
     * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
     */
    protected MethodCall(MethodLocator methodLocator,
                         TargetHandler targetHandler,
                         List<ArgumentLoader> argumentLoaders,
                         MethodInvoker methodInvoker,
                         TerminationHandler terminationHandler,
                         Assigner assigner,
                         Assigner.Typing typing) {
        this.methodLocator = methodLocator;
        this.targetHandler = targetHandler;
        this.argumentLoaders = argumentLoaders;
        this.methodInvoker = methodInvoker;
        this.terminationHandler = terminationHandler;
        this.assigner = assigner;
        this.typing = typing;
    }

    /**
     * Invokes the given method. Without further specification, the method is invoked without any arguments on
     * the instance of the instrumented class or statically, if the given method is {@code static}.
     *
     * @param method The method to invoke.
     * @return A method call implementation that invokes the given method without providing any arguments.
     */
    public static WithoutSpecifiedTarget invoke(Method method) {
        return invoke(new MethodDescription.ForLoadedMethod(nonNull(method)));
    }

    /**
     * Invokes the given constructor on the instance of the instrumented type.
     *
     * @param constructor The constructor to invoke.
     * @return A method call implementation that invokes the given constructor without providing any arguments.
     */
    public static WithoutSpecifiedTarget invoke(Constructor<?> constructor) {
        return invoke(new MethodDescription.ForLoadedConstructor(nonNull(constructor)));
    }

    /**
     * Invokes the given method. If the method description describes a constructor, it is automatically invoked as
     * a special method invocation on the instance of the instrumented type. The same is true for {@code private}
     * methods. Finally, {@code static} methods are invoked statically.
     *
     * @param methodDescription The method to invoke.
     * @return A method call implementation that invokes the given method without providing any arguments.
     */
    public static WithoutSpecifiedTarget invoke(MethodDescription methodDescription) {
        return invoke(new MethodLocator.ForExplicitMethod(nonNull(methodDescription)));
    }

    /**
     * Invokes a method using the provided method locator.
     *
     * @param methodLocator The method locator to apply for locating the method to invoke given the instrumented
     *                      method.
     * @return A method call implementation that uses the provided method locator for resolving the method
     * to be invoked.
     */
    public static WithoutSpecifiedTarget invoke(MethodLocator methodLocator) {
        return new WithoutSpecifiedTarget(nonNull(methodLocator));
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
                MethodInvoker.ForContextualInvocation.INSTANCE,
                TerminationHandler.ForMethodReturn.INSTANCE,
                Assigner.DEFAULT,
                Assigner.Typing.STATIC);
    }

    /**
     * Invokes the method that is instrumented by the returned instance by a super method invocation.
     *
     * @return A method call that invokes the method being instrumented.
     */
    public static MethodCall invokeSuper() {
        return new WithoutSpecifiedTarget(MethodLocator.ForInterceptedMethod.INSTANCE).onSuper();
    }

    /**
     * Defines a number of arguments to be handed to the method that is being invoked by this implementation. Any
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
            argumentLoaders.add(ArgumentLoader.ForStaticField.of(anArgument));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines the given types to be provided as arguments to the invoked method where the represented types
     * are stored in the generated class's constant pool.
     *
     * @param typeDescription The type descriptions to provide as arguments.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall with(TypeDescription... typeDescription) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            argumentLoaders.add(new ArgumentLoader.ForClassConstant(nonNull(aTypeDescription)));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines the given enumeration values to be provided as arguments to the invoked method where the values
     * are read from the enumeration class on demand.
     *
     * @param enumerationDescription The enumeration descriptions to provide as arguments.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall with(EnumerationDescription... enumerationDescription) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(enumerationDescription.length);
        for (EnumerationDescription anEnumerationDescription : enumerationDescription) {
            argumentLoaders.add(new ArgumentLoader.ForEnumerationValue(nonNull(anEnumerationDescription)));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines the given Java instances to be provided as arguments to the invoked method where the given
     * instances are stored in the generated class's constant pool.
     *
     * @param javaInstance The Java instances to provide as arguments.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall with(JavaInstance... javaInstance) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(javaInstance.length);
        for (JavaInstance aJavaInstance : javaInstance) {
            argumentLoaders.add(new ArgumentLoader.ForJavaInstance(nonNull(aJavaInstance)));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines a number of arguments to be handed to the method that is being invoked by this implementation. Any
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
                    : new ArgumentLoader.ForStaticField(anArgument));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines a number of arguments of the instrumented method by their parameter indices to be handed
     * to the invoked method as an argument.
     *
     * @param index The parameter indices of the instrumented method to be handed to the invoked method as an
     *              argument in their order. The indices are zero-based.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall withArgument(int... index) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(index.length);
        for (int anIndex : index) {
            if (anIndex < 0) {
                throw new IllegalArgumentException("Negative index: " + anIndex);
            }
            argumentLoaders.add(new ArgumentLoader.ForMethodParameter(anIndex));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Assigns the {@code this} reference to the next parameter.
     *
     * @return This method call where the next parameter is a assigned a reference to the {@code this} reference
     * of the instance of the intercepted method.
     */
    public MethodCall withThis() {
        return new MethodCall(methodLocator,
                targetHandler,
                join(argumentLoaders, ArgumentLoader.ForThisReference.INSTANCE),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Assigns the {@link java.lang.Class} value of the instrumented type.
     *
     * @return This method call where the next parameter is a assigned a reference to the {@link java.lang.Class}
     * value of the instrumented type.
     */
    public MethodCall withOwnType() {
        return new MethodCall(methodLocator,
                targetHandler,
                join(argumentLoaders, ArgumentLoader.ForOwnType.INSTANCE),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines a method call which fetches a value from an instance field. The value of the field needs to be
     * defined manually and is initialized with {@code null}. The field itself is defined by this implementation.
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
     * defined manually and is initialized with {@code null}. The field itself is defined by this implementation.
     *
     * @param typeDescription The type of the field.
     * @param name            The name of the field.
     * @return A method call which assigns the next parameter to the value of the instance field.
     */
    public MethodCall withInstanceField(TypeDescription typeDescription, String name) {
        return new MethodCall(methodLocator,
                targetHandler,
                join(argumentLoaders, new ArgumentLoader.ForInstanceField(nonNull(typeDescription), nonNull(name))),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines a method call which fetches a value from an existing field. The field is not defines by this
     * implementation.
     *
     * @param fieldName The name of the field.
     * @return A method call which assigns the next parameter to the value of the given field.
     */
    public MethodCall withField(String... fieldName) {
        List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(fieldName.length);
        for (String aFieldName : fieldName) {
            argumentLoaders.add(new ArgumentLoader.ForExistingField(aFieldName));
        }
        return new MethodCall(methodLocator,
                targetHandler,
                join(this.argumentLoaders, argumentLoaders),
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    /**
     * Defines an assigner to be used for assigning values to the parameters of the invoked method. This assigner
     * is also used for assigning the invoked method's return value to the return type of the instrumented method,
     * if this method is not chained with
     * {@link net.bytebuddy.implementation.MethodCall#andThen(Implementation)} such
     * that a return value of this method call is discarded.
     *
     * @param assigner The assigner to use.
     * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
     * @return This method call using the provided assigner.
     */
    public MethodCall withAssigner(Assigner assigner, Assigner.Typing typing) {
        return new MethodCall(methodLocator,
                targetHandler,
                argumentLoaders,
                methodInvoker,
                terminationHandler,
                nonNull(assigner),
                nonNull(typing));
    }

    /**
     * Applies another implementation after invoking this method call. A return value that is the result of this
     * method call is dropped.
     *
     * @param implementation The implementation that is to be applied after applying this method call implementation.
     * @return An implementation that first applies this method call and the given implementation right after.
     */
    public Implementation andThen(Implementation implementation) {
        return new Implementation.Compound(new MethodCall(methodLocator,
                targetHandler,
                argumentLoaders,
                methodInvoker,
                TerminationHandler.ForChainedInvocation.INSTANCE,
                assigner,
                typing), nonNull(implementation));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        for (ArgumentLoader argumentLoader : argumentLoaders) {
            instrumentedType = argumentLoader.prepare(instrumentedType);
        }
        return targetHandler.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationTarget);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MethodCall)) return false;
        MethodCall that = (MethodCall) other;
        return typing == that.typing
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
        result = 31 * result + typing.hashCode();
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
                ", typing=" + typing +
                '}';
    }

    /**
     * A method locator is responsible for identifying the method that is to be invoked
     * by a {@link net.bytebuddy.implementation.MethodCall}.
     */
    public interface MethodLocator {

        /**
         * Resolves the method to be invoked.
         *
         * @param instrumentedMethod The method being instrumented.
         * @return The method to invoke.
         */
        MethodDescription resolve(MethodDescription instrumentedMethod);

        /**
         * A method locator that simply returns the intercepted method.
         */
        enum ForInterceptedMethod implements MethodLocator {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodDescription resolve(MethodDescription instrumentedMethod) {
                return instrumentedMethod;
            }

            @Override
            public String toString() {
                return "MethodCall.MethodLocator.ForInterceptedMethod." + name();
            }
        }

        /**
         * Invokes a given method.
         */
        class ForExplicitMethod implements MethodLocator {

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
    }

    /**
     * A target handler is responsible for invoking a method for a
     * {@link net.bytebuddy.implementation.MethodCall}.
     */
    protected interface TargetHandler {

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
         * A target handler that invokes a method either on the instance of the instrumented
         * type or as a static method.
         */
        enum ForSelfOrStaticInvocation implements TargetHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType) {
                return new StackManipulation.Compound(
                        methodDescription.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadOffset(0),
                        methodDescription.isConstructor()
                                ? Duplication.SINGLE
                                : StackManipulation.Trivial.INSTANCE
                );
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public String toString() {
                return "MethodCall.TargetHandler.ForSelfOrStaticInvocation." + name();
            }
        }

        /**
         * Invokes a method in order to construct a new instance.
         */
        enum ForConstructingInvocation implements TargetHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType) {
                return new StackManipulation.Compound(TypeCreation.forType(methodDescription.getDeclaringType().asErasure()), Duplication.SINGLE);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public String toString() {
                return "MethodCall.TargetHandler.ForConstructingInvocation." + name();
            }
        }

        /**
         * A target handler that invokes a method on an instance that is stored in a static field.
         */
        class ForStaticField implements TargetHandler {

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
             * Creates a new target handler for a static field.
             *
             * @param target The target on which the method is to be invoked.
             */
            public ForStaticField(Object target) {
                this.target = target;
                fieldName = String.format("%s$%s", FIELD_PREFIX, RandomString.make());
            }

            @Override
            public StackManipulation resolve(MethodDescription methodDescription, TypeDescription instrumentedType) {
                return FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(fieldName, FIELD_MODIFIERS, new TypeDescription.ForLoadedType(target.getClass())))
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
        class ForInstanceField implements TargetHandler {

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
                return new StackManipulation.Compound(
                        methodDescription.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadOffset(0),
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter());
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(new FieldDescription.Token(fieldName, FIELD_MODIFIERS, fieldType));
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
    }

    /**
     * An argument loader is responsible for loading an argument for an invoked method
     * onto the operand stack.
     */
    protected interface ArgumentLoader {

        /**
         * Loads the argument that is represented by this instance onto the operand stack.
         *
         * @param instrumentedType  The instrumented type.
         * @param interceptedMethod The method being intercepted.
         * @param targetType        The target type.
         * @param assigner          The assigner to be used.
         * @param typing            Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return The stack manipulation that loads the represented argument onto the stack.
         */
        StackManipulation resolve(TypeDescription instrumentedType,
                                  MethodDescription interceptedMethod,
                                  TypeDescription targetType,
                                  Assigner assigner,
                                  Assigner.Typing typing);

        /**
         * Prepares the instrumented type in order to allow the loading of the represented argument.
         *
         * @param instrumentedType The instrumented type.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * An argument loader that loads the {@code null} value onto the operand stack.
         */
        enum ForNullConstant implements ArgumentLoader {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                if (targetType.isPrimitive()) {
                    throw new IllegalStateException("Cannot assign null to " + targetType);
                }
                return NullConstant.INSTANCE;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForNullConstant." + name();
            }
        }

        /**
         * An argument loader that assigns the {@code this} reference to a parameter.
         */
        enum ForThisReference implements ArgumentLoader {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                if (interceptedMethod.isStatic()) {
                    throw new IllegalStateException(interceptedMethod + " has no instance");
                }
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.REFERENCE.loadOffset(0),
                        assigner.assign(instrumentedType, targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + instrumentedType + " to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForThisReference." + name();
            }
        }

        /**
         * Loads the instrumented type onto the operand stack.
         */
        enum ForOwnType implements ArgumentLoader {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        ClassConstant.of(instrumentedType),
                        assigner.assign(TypeDescription.CLASS, targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign Class value to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForOwnType." + name();
            }
        }

        /**
         * Loads a parameter of the instrumented method onto the operand stack.
         */
        class ForMethodParameter implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                if (index >= interceptedMethod.getParameters().size()) {
                    throw new IllegalStateException(interceptedMethod + " does not have a parameter with index " + index);
                }
                ParameterDescription parameterDescription = interceptedMethod.getParameters().get(index);
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.forType(parameterDescription.getType().asErasure()).loadOffset(parameterDescription.getOffset()),
                        assigner.assign(parameterDescription.getType().asErasure(), targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + targetType + " for " + interceptedMethod);
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
        class ForStaticField implements ArgumentLoader {

            /**
             * The name prefix of the field to store the argument.
             */
            private static final String FIELD_PREFIX = "methodCall";

            /**
             * The modifier of the field.
             */
            private static final int FIELD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

            /**
             * The value to be stored in the field.
             */
            private final Object value;

            /**
             * The name of the field.
             */
            private final String fieldName;

            /**
             * Creates a new argument loader that stores the value in a field.
             *
             * @param value The value to be stored and loaded onto the operand stack.
             */
            protected ForStaticField(Object value) {
                this.value = value;
                fieldName = String.format("%s$%s", FIELD_PREFIX, RandomString.make());
            }

            /**
             * Resolves a value to be stored to be stored in the constant pool of the class, if possible.
             *
             * @param value The value to be stored in the field.
             * @return An argument loader that loads the given value onto the operand stack.
             */
            public static ArgumentLoader of(Object value) {
                if (value == null) {
                    return ForNullConstant.INSTANCE;
                } else if (value instanceof String) {
                    return new ForTextConstant((String) value);
                } else if (value instanceof Boolean) {
                    return new ForBooleanConstant((Boolean) value);
                } else if (value instanceof Byte) {
                    return new ForByteConstant((Byte) value);
                } else if (value instanceof Short) {
                    return new ForShortConstant((Short) value);
                } else if (value instanceof Character) {
                    return new ForCharacterConstant((Character) value);
                } else if (value instanceof Integer) {
                    return new ForIntegerConstant((Integer) value);
                } else if (value instanceof Long) {
                    return new ForLongConstant((Long) value);
                } else if (value instanceof Float) {
                    return new ForFloatConstant((Float) value);
                } else if (value instanceof Double) {
                    return new ForDoubleConstant((Double) value);
                } else if (value instanceof Class) {
                    return new ForClassConstant(new TypeDescription.ForLoadedType((Class<?>) value));
                } else if (JavaType.METHOD_HANDLE.getTypeStub().isInstance(value)) {
                    return new ForJavaInstance(JavaInstance.MethodHandle.of(value));
                } else if (JavaType.METHOD_TYPE.getTypeStub().isInstance(value)) {
                    return new ForJavaInstance(JavaInstance.MethodType.of(value));
                } else if (value instanceof Enum<?>) {
                    return new ForEnumerationValue(new EnumerationDescription.ForLoadedEnumeration((Enum<?>) value));
                } else {
                    return new ForStaticField(value);
                }
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter(),
                        assigner.assign(new TypeDescription.ForLoadedType(value.getClass()), targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + value.getClass() + " to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(fieldName, FIELD_MODIFIER, new TypeDescription.ForLoadedType(value.getClass())))
                        .withInitializer(new LoadedTypeInitializer.ForStaticField<Object>(fieldName, value, true));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && value.equals(((ForStaticField) other).value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForStaticField{" +
                        "value=" + value +
                        ", fieldName='" + fieldName + '\'' +
                        '}';
            }
        }

        /**
         * Loads a value onto the operand stack that is stored in an instance field.
         */
        class ForInstanceField implements ArgumentLoader {

            /**
             * The modifier to be applied to the instance field.
             */
            private static final int MODIFIERS = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

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
            public ForInstanceField(TypeDescription fieldType, String fieldName) {
                this.fieldType = fieldType;
                this.fieldName = fieldName;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                if (interceptedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot access instance field from static " + interceptedMethod);
                }
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.REFERENCE.loadOffset(0),
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter(),
                        assigner.assign(fieldType, targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign field " + fieldName + " of type "
                            + fieldType + " to " + targetType);
                }
                return stackManipulation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(new FieldDescription.Token(fieldName, MODIFIERS, fieldType));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForInstanceField that = (ForInstanceField) other;
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
                return "MethodCall.ArgumentLoader.ForInstanceField{" +
                        "fieldType=" + fieldType +
                        ", fieldName='" + fieldName + '\'' +
                        '}';
            }
        }

        /**
         * Loads the value of an existing field onto the operand stack.
         */
        class ForExistingField implements ArgumentLoader {

            /**
             * The name of the field.
             */
            private final String fieldName;

            /**
             * Creates a new argument loader for an existing field.
             *
             * @param fieldName The name of the field.
             */
            public ForExistingField(String fieldName) {
                this.fieldName = fieldName;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                FieldDescription fieldDescription = locate(instrumentedType);
                if (!fieldDescription.isStatic() && interceptedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot access non-static " + fieldDescription + " from " + interceptedMethod);
                }
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        fieldDescription.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadOffset(0),
                        FieldAccess.forField(fieldDescription).getter(),
                        assigner.assign(fieldDescription.getType().asErasure(), targetType, typing)
                );
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + fieldDescription + " to " + targetType);
                }
                return stackManipulation;
            }

            /**
             * Locates the specified field on the instrumented type.
             *
             * @param instrumentedType The instrumented type.
             * @return The located field.
             */
            private FieldDescription locate(TypeDescription instrumentedType) {
                for (GenericTypeDescription currentType : instrumentedType) {
                    FieldList<?> fieldList = currentType.asErasure().getDeclaredFields().filter(named(fieldName).and(isVisibleTo(instrumentedType)));
                    if (fieldList.size() != 0) {
                        return fieldList.getOnly();
                    }
                }
                throw new IllegalStateException(instrumentedType + " does not define a visible field " + fieldName);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && fieldName.equals(((ForExistingField) other).fieldName);
            }

            @Override
            public int hashCode() {
                return fieldName.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForExistingField{fieldName='" + fieldName + '\'' + '}';
            }
        }

        /**
         * Loads a {@code boolean} value onto the operand stack.
         */
        class ForBooleanConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(boolean.class), targetType, typing));
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
        class ForByteConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(byte.class), targetType, typing));
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
        class ForShortConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(short.class), targetType, typing));
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
        class ForCharacterConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(char.class), targetType, typing));
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
        class ForIntegerConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        IntegerConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(int.class), targetType, typing));
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
        class ForLongConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        LongConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(long.class), targetType, typing));
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
        class ForFloatConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        FloatConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(float.class), targetType, typing));
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
        class ForDoubleConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        DoubleConstant.forValue(value),
                        assigner.assign(new TypeDescription.ForLoadedType(double.class), targetType, typing));
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
        class ForTextConstant implements ArgumentLoader {

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
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        new TextConstant(value),
                        assigner.assign(TypeDescription.STRING, targetType, typing));
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

        /**
         * Loads a {@link java.lang.Class} value onto the operand stack.
         */
        class ForClassConstant implements ArgumentLoader {

            /**
             * The type to load onto the operand stack.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a new class constant representation.
             *
             * @param typeDescription The type to represent.
             */
            public ForClassConstant(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        ClassConstant.of(typeDescription),
                        assigner.assign(TypeDescription.CLASS, targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign class value to " + targetType);
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
                        && typeDescription.equals(((ForClassConstant) other).typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForClassConstant{" +
                        "typeDescription=" + typeDescription +
                        '}';
            }
        }

        /**
         * An argument loader that loads an enumeration constant.
         */
        class ForEnumerationValue implements ArgumentLoader {

            /**
             * The enumeration to describe.
             */
            private final EnumerationDescription enumerationDescription;

            /**
             * Creates a new argument loader for an enumeration constant.
             *
             * @param enumerationDescription The enumeration to describe.
             */
            public ForEnumerationValue(EnumerationDescription enumerationDescription) {
                this.enumerationDescription = enumerationDescription;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        FieldAccess.forEnumeration(enumerationDescription),
                        assigner.assign(enumerationDescription.getEnumerationType(), targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + enumerationDescription.getEnumerationType() + " value to " + targetType);
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
                        && enumerationDescription.equals(((ForEnumerationValue) other).enumerationDescription);
            }

            @Override
            public int hashCode() {
                return enumerationDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForEnumerationValue{" +
                        "enumerationDescription=" + enumerationDescription +
                        '}';
            }
        }

        /**
         * Loads a Java instance onto the operand stack.
         */
        class ForJavaInstance implements ArgumentLoader {

            /**
             * The Java instance to load onto the operand stack.
             */
            private final JavaInstance javaInstance;

            /**
             * Creates a new argument loader for a Java instance.
             *
             * @param javaInstance The Java instance to load as an argument.
             */
            public ForJavaInstance(JavaInstance javaInstance) {
                this.javaInstance = javaInstance;
            }

            @Override
            public StackManipulation resolve(TypeDescription instrumentedType,
                                             MethodDescription interceptedMethod,
                                             TypeDescription targetType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        javaInstance.asStackManipulation(),
                        assigner.assign(javaInstance.getInstanceType(), targetType, typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign Class value to " + targetType);
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
                        && javaInstance.equals(((ForJavaInstance) other).javaInstance);
            }

            @Override
            public int hashCode() {
                return javaInstance.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.ArgumentLoader.ForJavaInstance{" +
                        "javaInstance=" + javaInstance +
                        '}';
            }
        }
    }

    /**
     * A method invoker is responsible for creating a method invocation that is to be applied by a
     * {@link net.bytebuddy.implementation.MethodCall}.
     */
    protected interface MethodInvoker {

        /**
         * Invokes the method.
         *
         * @param methodDescription    The method to be invoked.
         * @param implementationTarget The implementation target of the instrumented instance.
         * @return A stack manipulation that represents the method invocation.
         */
        StackManipulation invoke(MethodDescription methodDescription, Target implementationTarget);

        /**
         * Applies a contextual invocation of the provided method, i.e. a static invocation for static methods,
         * a special invocation for constructors and private methods and a virtual invocation for any other method.
         */
        enum ForContextualInvocation implements MethodInvoker {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation invoke(MethodDescription methodDescription, Target implementationTarget) {
                if (methodDescription.isVirtual() && !methodDescription.isInvokableOn(implementationTarget.getTypeDescription())) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " on " + implementationTarget.getTypeDescription());
                } else if (!methodDescription.isVisibleTo(implementationTarget.getTypeDescription())) {
                    throw new IllegalStateException(implementationTarget.getTypeDescription() + " cannot see " + methodDescription);
                }
                return methodDescription.isVirtual()
                        ? MethodInvocation.invoke(methodDescription).virtual(implementationTarget.getTypeDescription())
                        : MethodInvocation.invoke(methodDescription);
            }

            @Override
            public String toString() {
                return "MethodCall.MethodInvoker.ForContextualInvocation." + name();
            }
        }

        /**
         * Applies a virtual invocation on a given type.
         */
        class ForVirtualInvocation implements MethodInvoker {

            /**
             * The type description to virtually invoke the method upon.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a new method invoking for a virtual method invocation.
             *
             * @param typeDescription The type description to virtually invoke the method upon.
             */
            protected ForVirtualInvocation(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public StackManipulation invoke(MethodDescription methodDescription, Target implementationTarget) {
                if (!methodDescription.isVirtual()) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " virtually");
                } else if (!methodDescription.isInvokableOn(typeDescription)) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " on " + typeDescription);
                } else if (!typeDescription.isVisibleTo(implementationTarget.getTypeDescription())) {
                    throw new IllegalStateException(typeDescription + " is not visible to " + implementationTarget.getTypeDescription());
                }
                return MethodInvocation.invoke(methodDescription).virtual(typeDescription);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForVirtualInvocation that = (ForVirtualInvocation) other;
                return typeDescription.equals(that.typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodCall.MethodInvoker.ForVirtualInvocation{" +
                        "typeDescription=" + typeDescription +
                        '}';
            }
        }

        /**
         * Applies a super method invocation of the provided method.
         */
        enum ForSuperMethodInvocation implements MethodInvoker {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation invoke(MethodDescription methodDescription, Target implementationTarget) {
                if (implementationTarget.getTypeDescription().getSuperType() == null) {
                    throw new IllegalStateException("Cannot invoke super method for " + implementationTarget.getTypeDescription());
                } else if (!methodDescription.isInvokableOn(implementationTarget.getOriginType())) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " as super method of " + implementationTarget.getTypeDescription());
                }
                StackManipulation stackManipulation = implementationTarget.invokeDominant(methodDescription.asToken());
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " as a super method");
                }
                return stackManipulation;
            }

            @Override
            public String toString() {
                return "MethodCall.MethodInvoker.ForSuperMethodInvocation." + name();
            }
        }

        /**
         * Invokes a method as a Java 8 default method.
         */
        enum ForDefaultMethodInvocation implements MethodInvoker {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation invoke(MethodDescription methodDescription, Target implementationTarget) {
                if (!methodDescription.isInvokableOn(implementationTarget.getTypeDescription())) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " as default method of " + implementationTarget.getTypeDescription());
                }
                StackManipulation stackManipulation = implementationTarget.invokeDefault(methodDescription.getDeclaringType().asErasure(), methodDescription.asToken());
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + methodDescription + " on " + implementationTarget.getTypeDescription());
                }
                return stackManipulation;
            }

            @Override
            public String toString() {
                return "MethodCall.MethodInvoker.ForDefaultMethodInvocation." + name();
            }
        }
    }

    /**
     * A termination handler is responsible to handle the return value of a method that is invoked via a
     * {@link net.bytebuddy.implementation.MethodCall}.
     */
    protected interface TerminationHandler {

        /**
         * Returns a stack manipulation that handles the method return.
         *
         * @param invokedMethod     The method that was invoked by the method call.
         * @param interceptedMethod The method being intercepted.
         * @param assigner          The assigner to be used.
         * @param typing            Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return A stack manipulation that handles the method return.
         */
        StackManipulation resolve(MethodDescription invokedMethod,
                                  MethodDescription interceptedMethod,
                                  Assigner assigner,
                                  Assigner.Typing typing);

        /**
         * Returns the return value of the method call from the intercepted method.
         */
        enum ForMethodReturn implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription invokedMethod, MethodDescription interceptedMethod, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = assigner.assign(invokedMethod.isConstructor()
                        ? invokedMethod.getDeclaringType().asErasure()
                        : invokedMethod.getReturnType().asErasure(), interceptedMethod.getReturnType().asErasure(), typing);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot return " + invokedMethod.getReturnType() + " from " + interceptedMethod);
                }
                return new StackManipulation.Compound(stackManipulation,
                        MethodReturn.returning(interceptedMethod.getReturnType().asErasure()));
            }

            @Override
            public String toString() {
                return "MethodCall.TerminationHandler.ForMethodReturn." + name();
            }
        }

        /**
         * Drops the return value of the called method from the operand stack without returning from the intercepted
         * method.
         */
        enum ForChainedInvocation implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription invokedMethod, MethodDescription interceptedMethod, Assigner assigner, Assigner.Typing typing) {
                return Removal.pop(invokedMethod.isConstructor()
                        ? invokedMethod.getDeclaringType().asErasure()
                        : invokedMethod.getReturnType().asErasure());
            }

            @Override
            public String toString() {
                return "MethodCall.TerminationHandler.ForChainedInvocation." + name();
            }
        }
    }

    /**
     * Represents a {@link net.bytebuddy.implementation.MethodCall} that invokes a method without specifying
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
                    MethodInvoker.ForContextualInvocation.INSTANCE,
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
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
                    new MethodInvoker.ForVirtualInvocation(new TypeDescription.ForLoadedType(target.getClass())),
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    assigner,
                    typing);
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
                    new TargetHandler.ForInstanceField(nonNull(fieldName), isActualType(typeDescription)),
                    argumentLoaders,
                    new MethodInvoker.ForVirtualInvocation(typeDescription),
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    assigner,
                    typing);
        }

        /**
         * Invokes the given method by a super method invocation on the instance of the instrumented type.
         * Note that the super method is resolved depending on the type of implementation when this method is called.
         * In case that a subclass is created, the super type is invoked. If a type is rebased, the rebased method
         * is invoked if such a method exists.
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
                    typing);
        }

        /**
         * Invokes the given method by a Java 8default method invocation on the instance of the instrumented type.
         *
         * @return A method call where the given method is invoked as a super method invocation.
         */
        public MethodCall onDefault() {
            return new MethodCall(methodLocator,
                    TargetHandler.ForSelfOrStaticInvocation.INSTANCE,
                    argumentLoaders,
                    MethodInvoker.ForDefaultMethodInvocation.INSTANCE,
                    TerminationHandler.ForMethodReturn.INSTANCE,
                    assigner,
                    typing);
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
                    ", typing=" + typing +
                    '}';
        }
    }

    /**
     * The appender being used to implement a {@link net.bytebuddy.implementation.MethodCall}.
     */
    protected class Appender implements ByteCodeAppender {

        /**
         * The implementation target of the current implementation.
         */
        private final Target implementationTarget;

        /**
         * Creates a new appender.
         *
         * @param implementationTarget The implementation target of the current implementation.
         */
        protected Appender(Target implementationTarget) {
            this.implementationTarget = implementationTarget;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            MethodDescription invokedMethod = methodLocator.resolve(instrumentedMethod);
            TypeList methodParameters = invokedMethod.getParameters().asTypeList().asErasures();
            if (methodParameters.size() != argumentLoaders.size()) {
                throw new IllegalStateException(invokedMethod + " does not take " + argumentLoaders.size() + " arguments");
            }
            int index = 0;
            StackManipulation[] argumentInstruction = new StackManipulation[argumentLoaders.size()];
            for (ArgumentLoader argumentLoader : argumentLoaders) {
                argumentInstruction[index] = argumentLoader.resolve(implementationTarget.getTypeDescription(),
                        instrumentedMethod,
                        methodParameters.get(index++),
                        assigner,
                        typing);
            }
            StackManipulation.Size size = new StackManipulation.Compound(
                    targetHandler.resolve(invokedMethod, implementationTarget.getTypeDescription()),
                    new StackManipulation.Compound(argumentInstruction),
                    methodInvoker.invoke(invokedMethod, implementationTarget),
                    terminationHandler.resolve(invokedMethod, instrumentedMethod, assigner, typing)
            ).apply(methodVisitor, implementationContext);
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
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Appender appender = (Appender) other;
            return implementationTarget.equals(appender.implementationTarget)
                    && MethodCall.this.equals(appender.getOuter());

        }

        @Override
        public int hashCode() {
            return implementationTarget.hashCode() + 31 * MethodCall.this.hashCode();
        }

        @Override
        public String toString() {
            return "MethodCall.Appender{" +
                    "methodCall=" + MethodCall.this +
                    ", implementationTarget=" + implementationTarget +
                    '}';
        }
    }
}
