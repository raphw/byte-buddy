package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
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
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.utility.ByteBuddyCommons.join;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * An instrumentation that applies a
 * <a href="http://docs.oracle.com/javase/8/docs/api/java/lang/invoke/package-summary.html">dynamic method invocation</a>.
 */
public class InvokeDynamic implements Instrumentation {

    /**
     * The bootstrap method.
     */
    protected final MethodDescription bootstrapMethod;

    /**
     * The arguments that are provided to the bootstrap method.
     */
    protected final List<?> handleArguments;

    /**
     * The target provided that identifies the method to be bootstrapped.
     */
    protected final InvocationProvider invocationProvider;

    /**
     * A handler that handles the method return.
     */
    protected final TerminationHandler terminationHandler;

    /**
     * The assigner to be used.
     */
    protected final Assigner assigner;

    /**
     * {@code true} if the assigner should attempt dynamically-typed assignments.
     */
    protected final boolean dynamicallyTyped;

    /**
     * Creates a new invoke dynamic instrumentation.
     *
     * @param bootstrapMethod    The bootstrap method.
     * @param handleArguments    The arguments that are provided to the bootstrap method.
     * @param invocationProvider The target provided that identifies the method to be bootstrapped.
     * @param terminationHandler A handler that handles the method return.
     * @param assigner           The assigner to be used.
     * @param dynamicallyTyped   {@code true} if the assigner should attempt dynamically-typed assignments.
     */
    protected InvokeDynamic(MethodDescription bootstrapMethod,
                            List<?> handleArguments,
                            InvocationProvider invocationProvider,
                            TerminationHandler terminationHandler,
                            Assigner assigner,
                            boolean dynamicallyTyped) {
        this.bootstrapMethod = bootstrapMethod;
        this.handleArguments = handleArguments;
        this.invocationProvider = invocationProvider;
        this.terminationHandler = terminationHandler;
        this.assigner = assigner;
        this.dynamicallyTyped = dynamicallyTyped;
    }

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
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method.
     *
     * @param method   The bootstrap method that is used to link the instrumented method.
     * @param argument The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                 constant pool, i.e. primitive types (represented as their wrapper types), the
     *                 {@link java.lang.String} type as well as {@code MethodType} and {@code MethodHandle} instances.
     * @return An instrumentation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Method method, Object... argument) {
        return bootstrap(new MethodDescription.ForLoadedMethod(nonNull(method)), argument);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap constructor.
     *
     * @param constructor The bootstrap constructor that is used to link the instrumented method.
     * @param argument    The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                    constant pool, i.e. primitive types (represented as their wrapper types), the
     *                    {@link java.lang.String} type as well as {@code MethodType} and {@code MethodHandle} instances.
     * @return An instrumentation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(Constructor<?> constructor, Object... argument) {
        return bootstrap(new MethodDescription.ForLoadedConstructor(nonNull(constructor)), argument);
    }

    /**
     * Implements the instrumented method with a dynamic method invocation which is linked at runtime using the
     * specified bootstrap method or constructor.
     *
     * @param bootstrapMethod The bootstrap method or constructor that is used to link the instrumented method.
     * @param argument        The arguments that are handed to the bootstrap method. Any argument must be saved in the
     *                        constant pool, i.e. primitive types (represented as their wrapper types) with a size of
     *                        at least 32 bit, {@link java.lang.String} types, {@link java.lang.Class} types as well
     *                        as {@code MethodType} and {@code MethodHandle} instances.
     * @return An instrumentation where a {@code this} reference, if available, and all arguments of the
     * instrumented method are passed to the bootstrapped method unless explicit parameters are specified.
     */
    public static WithImplicitTarget bootstrap(MethodDescription bootstrapMethod, Object... argument) {
        List<?> arguments = Arrays.asList(nonNull(argument));
        if (!bootstrapMethod.isBootstrap(arguments)) {
            throw new IllegalArgumentException("Not a valid bootstrap method " + bootstrapMethod + " for " + arguments);
        }
        List<Object> wrappedArguments = new ArrayList<Object>(arguments.size());
        for (Object anArgument : argument) {
            if (anArgument instanceof Class) {
                anArgument = Type.getType((Class<?>) anArgument);
            } else if (anArgument instanceof TypeDescription) {
                anArgument = Type.getType(((TypeDescription) anArgument).getDescriptor());
            } else if (anArgument instanceof TypeDescription.MethodTypeToken) {
                anArgument = ((TypeDescription.MethodTypeToken) anArgument).resolve();
            } else if (anArgument instanceof TypeDescription.MethodHandleToken) {
                anArgument = ((TypeDescription.MethodHandleToken) anArgument).resolve();
            }
            wrappedArguments.add(anArgument);
        }
        return new WithImplicitTarget(bootstrapMethod,
                wrappedArguments,
                new InvocationProvider.Default(),
                TerminationHandler.ForMethodReturn.INSTANCE,
                defaultAssigner(),
                defaultDynamicallyTyped());
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code boolean} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withBooleanValue(boolean... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (boolean aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForBooleanValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code byte} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withByteValue(byte... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (byte aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForByteValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code short} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withShortValue(short... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (short aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForShortValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code char} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withCharacterValue(char... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (char aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForCharacterValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code int} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withIntegerValue(int... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (int aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForIntegerValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code long} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withLongValue(long... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (long aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForLongValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code float} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withFloatValue(float... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (float aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForFloatValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified {@code double} arguments
     * as its next parameters.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withDoubleValue(double... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (double aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForDoubleValue(aValue));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * <p>
     * Requires the bootstrap method to bootstrap a method that takes the specified arguments as its next parameters.
     * Note that any primitive parameters are passed as their wrapper types. Furthermore, values that can be stored
     * in the instrumented class's constant pool might be of different object identity when passed to the
     * bootstrapped method.
     * </p>
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withValue(Object... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (Object aValue : value) {
            argumentProviders.add(InvocationProvider.ArgumentProvider.ConstantPoolWrapper.of(nonNull(aValue)));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Requires the bootstrap method to bootstrap a method that takes the specified arguments as its next parameters.
     * Note that any primitive parameters are passed as their wrapper types. Any value that is passed to the
     * bootstrapped method is guaranteed to be of the same object identity.
     *
     * @param value The arguments to pass to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withReference(Object... value) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(value.length);
        for (Object aValue : value) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForStaticField(nonNull(aValue)));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Passes {@code null} values of the given types to the bootstrapped method.
     *
     * @param type The type that the {@code null} values should represent.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withNullValue(Class<?>... type) {
        return withNullValue(new TypeList.ForLoadedType(nonNull(type)).toArray(new TypeDescription[type.length]));
    }

    /**
     * Passes {@code null} values of the given types to the bootstrapped method.
     *
     * @param typeDescription The type that the {@code null} values should represent.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withNullValue(TypeDescription... typeDescription) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            if (aTypeDescription.isPrimitive()) {
                throw new IllegalArgumentException("Cannot assign null to primitive type: " + aTypeDescription);
            }
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForNullValue(aTypeDescription));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Passes parameters of the instrumented method to the bootstrapped method.
     *
     * @param index The indices of the parameters that should be passed to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withArgument(int... index) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(index.length);
        for (int anIndex : index) {
            if (anIndex < 0) {
                throw new IllegalArgumentException("Method parameter indices cannot be negative: " + anIndex);
            }
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForMethodParameter(anIndex));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Passes references to {@code this} onto the operand stack where the instance is represented as
     * the given types.
     *
     * @param type The types as which the {@code this} reference of the intercepted method should be masked.
     * @return This instrumentation where {@code this} references are passed as the next arguments.
     */
    public InvokeDynamic withThis(Class<?>... type) {
        return withThis(new TypeList.ForLoadedType(type).toArray(new TypeDescription[type.length]));
    }

    /**
     * Passes references to {@code this} onto the operand stack where the instance is represented as
     * the given types.
     *
     * @param typeDescription The types as which the {@code this} reference of the intercepted method should be masked.
     * @return This instrumentation where {@code this} references are passed as the next arguments.
     */
    public InvokeDynamic withThis(TypeDescription... typeDescription) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForThisInstance(nonNull(aTypeDescription)));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Passes the value of the specified instance field to the bootstrapped method. The field value of the created
     * private field must be set manually on any instrumented instance.
     *
     * @param fieldName The name of the field.
     * @param fieldType The type of the field.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the value of the defined
     * field.
     */
    public InvokeDynamic withInstanceField(String fieldName, Class<?> fieldType) {
        return withInstanceField(fieldName, new TypeDescription.ForLoadedType(nonNull(fieldType)));
    }

    /**
     * Passes the value of the specified instance field to the bootstrapped method. The field value of the created
     * private field must be set manually on any instrumented instance.
     *
     * @param fieldName The name of the field.
     * @param fieldType The type of the field.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the value of the defined
     * field.
     */
    public InvokeDynamic withInstanceField(String fieldName, TypeDescription fieldType) {
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArgument(new InvocationProvider.ArgumentProvider.ForInstanceField(nonNull(fieldName), nonNull(fieldType))),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Passes the values of the specified fields to the bootstrap method. Any of the specified fields must already
     * exist for the instrumented type.
     *
     * @param fieldName The names of the fields to be passed to the bootstrapped method.
     * @return This invoke dynamic instrumentation where the bootstrapped method is passed the specified arguments.
     */
    public InvokeDynamic withField(String... fieldName) {
        List<InvocationProvider.ArgumentProvider> argumentProviders = new ArrayList<InvocationProvider.ArgumentProvider>(fieldName.length);
        for (String aFieldName : fieldName) {
            argumentProviders.add(new InvocationProvider.ArgumentProvider.ForExistingField(nonNull(aFieldName)));
        }
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider.appendArguments(argumentProviders),
                terminationHandler,
                assigner,
                dynamicallyTyped);
    }

    /**
     * Instructs this instrumentation to use the provided assigner and decides if the assigner should apply
     * dynamic typing.
     *
     * @param assigner         The assigner to use.
     * @param dynamicallyTyped {@code true} if the assigner should attempt dynamic typing.
     * @return The invoke dynamic instruction where the given assigner and dynamic-typing directive are applied.
     */
    public InvokeDynamic withAssigner(Assigner assigner, boolean dynamicallyTyped) {
        return new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider,
                terminationHandler,
                nonNull(assigner),
                dynamicallyTyped);
    }

    /**
     * Applies this invoke dynamic instrumentation and removes the return value of the bootstrapped method from
     * the operand stack before applying the provided instrumentation.
     *
     * @param instrumentation The instrumentation to apply after executing the dynamic method invocation.
     * @return An instrumentation that first applies this instrumentation and then the provided one.
     */
    public Instrumentation andThen(Instrumentation instrumentation) {
        return new Instrumentation.Compound(new InvokeDynamic(bootstrapMethod,
                handleArguments,
                invocationProvider,
                TerminationHandler.ForChainedInvocation.INSTANCE,
                assigner,
                dynamicallyTyped),
                nonNull(instrumentation));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return invocationProvider.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target instrumentationTarget) {
        return new Appender(instrumentationTarget.getTypeDescription());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InvokeDynamic)) return false;
        InvokeDynamic that = (InvokeDynamic) other;
        return dynamicallyTyped == that.dynamicallyTyped
                && assigner.equals(that.assigner)
                && bootstrapMethod.equals(that.bootstrapMethod)
                && handleArguments.equals(that.handleArguments)
                && invocationProvider.equals(that.invocationProvider)
                && terminationHandler.equals(that.terminationHandler);
    }

    @Override
    public int hashCode() {
        int result = bootstrapMethod.hashCode();
        result = 31 * result + handleArguments.hashCode();
        result = 31 * result + invocationProvider.hashCode();
        result = 31 * result + terminationHandler.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + (dynamicallyTyped ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "InvokeDynamic{" +
                "bootstrapMethod=" + bootstrapMethod +
                ", handleArguments=" + handleArguments +
                ", invocationProvider=" + invocationProvider +
                ", terminationHandler=" + terminationHandler +
                ", assigner=" + assigner +
                ", dynamicallyTyped=" + dynamicallyTyped +
                '}';
    }

    /**
     * An invocation provider is responsible for loading the arguments of the invoked method onto the operand
     * stack and for creating the actual <i>invoke dynamic</i> instruction.
     */
    protected interface InvocationProvider {

        /**
         * Creates a target for the invocation.
         *
         * @param methodDescription The method that is being intercepted.
         * @return The target for the invocation.
         */
        Target make(MethodDescription methodDescription);

        /**
         * Appends the given arguments to the invocation to be loaded onto the operand stack.
         *
         * @param argumentProviders The next arguments to be loaded onto the operand stack.
         * @return An invocation provider for this target that loads the given arguments onto the operand stack.
         */
        InvocationProvider appendArguments(List<ArgumentProvider> argumentProviders);

        /**
         * Appends the given argument to the invocation to be loaded onto the operand stack.
         *
         * @param argumentProvider The next argument to be loaded onto the operand stack.
         * @return An invocation provider for this target that loads the given arguments onto the operand stack.
         */
        InvocationProvider appendArgument(ArgumentProvider argumentProvider);

        /**
         * Returns a copy of this invocation provider that does not add any arguments.
         *
         * @return A copy of this invocation provider that does not add any arguments.
         */
        InvocationProvider withoutArguments();

        /**
         * Returns a copy of this invocation provider that applies the given name provider.
         *
         * @param nameProvider The name provider to be used.
         * @return A copy of this invocation provider that applies the given name provider.
         */
        InvocationProvider withNameProvider(NameProvider nameProvider);

        /**
         * Returns a copy of this invocation provider that applies the given return type provider.
         *
         * @param returnTypeProvider The return type provider to be used.
         * @return A copy of this invocation provider that applies the given return type provider.
         */
        InvocationProvider withReturnTypeProvider(ReturnTypeProvider returnTypeProvider);

        /**
         * Prepares the instrumented type.
         *
         * @param instrumentedType The instrumented type to prepare.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * A target for a dynamic method invocation.
         */
        interface Target {

            /**
             * Resolves the target.
             *
             * @param instrumentedType The instrumented type.
             * @param assigner         The assigner to be used.
             * @param dynamicallyTyped {@code true} if the assigner should attempt to assign objects by their
             *                         runtime type.
             * @return The resolved target.
             */
            Resolved resolve(TypeDescription instrumentedType, Assigner assigner, boolean dynamicallyTyped);

            /**
             * Represents a resolved {@link net.bytebuddy.instrumentation.InvokeDynamic.InvocationProvider.Target}.
             */
            interface Resolved {

                /**
                 * Returns the stack manipulation that loads the arguments onto the operand stack.
                 *
                 * @return The stack manipulation that loads the arguments onto the operand stack.
                 */
                StackManipulation getStackManipulation();

                /**
                 * Returns the requested return type.
                 *
                 * @return The requested return type.
                 */
                TypeDescription getReturnType();

                /**
                 * Returns the internal name of the requested method.
                 *
                 * @return The internal name of the requested method.
                 */
                String getInternalName();

                /**
                 * Returns the types of the values on the operand stack.
                 *
                 * @return The types of the values on the operand stack.
                 */
                List<TypeDescription> getParameterTypes();

                /**
                 * A simple implementation of
                 * {@link net.bytebuddy.instrumentation.InvokeDynamic.InvocationProvider.Target.Resolved}.
                 */
                class Simple implements Resolved {

                    /**
                     * The stack manipulation that loads the arguments onto the operand stack.
                     */
                    private final StackManipulation stackManipulation;

                    /**
                     * The internal name of the requested method.
                     */
                    private final String internalName;

                    /**
                     * The requested return type.
                     */
                    private final TypeDescription returnType;

                    /**
                     * The types of the values on the operand stack.
                     */
                    private final List<TypeDescription> parameterTypes;

                    /**
                     * Creates a new simple instance.
                     *
                     * @param stackManipulation The stack manipulation that loads the arguments onto the operand stack.
                     * @param internalName      The internal name of the requested method.
                     * @param returnType        The requested return type.
                     * @param parameterTypes    The types of the values on the operand stack.
                     */
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

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Simple simple = (Simple) other;
                        return internalName.equals(simple.internalName)
                                && parameterTypes.equals(simple.parameterTypes)
                                && returnType.equals(simple.returnType)
                                && stackManipulation.equals(simple.stackManipulation);
                    }

                    @Override
                    public int hashCode() {
                        int result = stackManipulation.hashCode();
                        result = 31 * result + internalName.hashCode();
                        result = 31 * result + returnType.hashCode();
                        result = 31 * result + parameterTypes.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "InvokeDynamic.InvocationProvider.Target.Resolved.Simple{" +
                                "stackManipulation=" + stackManipulation +
                                ", internalName='" + internalName + '\'' +
                                ", returnType=" + returnType +
                                ", parameterTypes=" + parameterTypes +
                                '}';
                    }
                }
            }

            /**
             * A target that requests to dynamically invoke a method to substitute for a given method.
             */
            class ForMethodDescription implements Target, Target.Resolved {

                /**
                 * The method that is being substituted.
                 */
                private final MethodDescription methodDescription;

                /**
                 * Creates a new target for substituting a given method.
                 *
                 * @param methodDescription The method that is being substituted.
                 */
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
                    return methodDescription.isStatic()
                            ? methodDescription.getParameters().asTypeList()
                            : new TypeList.Explicit(join(methodDescription.getDeclaringType(), methodDescription.getParameters().asTypeList()));
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && methodDescription.equals(((ForMethodDescription) other).methodDescription);
                }

                @Override
                public int hashCode() {
                    return methodDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.Target.ForMethodDescription{" +
                            "methodDescription=" + methodDescription +
                            '}';
                }
            }
        }

        /**
         * An argument provider is responsible for loading arguments to a bootstrapped method onto the operand
         * stack and providing the types of these arguments.
         */
        interface ArgumentProvider {

            /**
             * Resolves an argument provider.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param assigner           The assigner to be used.
             * @param dynamicallyTyped   {@code true} if the assigner should attempt to resolve an assigned type
             *                           at runtime.
             * @return A resolved version of this argument provider.
             */
            Resolved resolve(TypeDescription instrumentedType,
                             MethodDescription instrumentedMethod,
                             Assigner assigner,
                             boolean dynamicallyTyped);

            /**
             * Prepares the instrumented type.
             *
             * @param instrumentedType The instrumented type.
             * @return The prepared instrumented type.
             */
            InstrumentedType prepare(InstrumentedType instrumentedType);

            /**
             * An argument provider that loads a reference to the intercepted instance and all arguments of
             * the intercepted method.
             */
            enum ForInterceptedMethodInstanceAndParameters implements ArgumentProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                            instrumentedMethod.isStatic()
                                    ? instrumentedMethod.getParameters().asTypeList()
                                    : join(instrumentedMethod.getDeclaringType(), instrumentedMethod.getParameters().asTypeList()));
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForInterceptedMethodInstanceAndParameters." + name();
                }
            }

            /**
             * An argument provider that loads all arguments of the intercepted method.
             */
            enum ForInterceptedMethodParameters implements ArgumentProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(MethodVariableAccess.loadArguments(instrumentedMethod), instrumentedMethod.getParameters().asTypeList());
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForInterceptedMethodParameters." + name();
                }
            }

            /**
             * Represents wrapper types and types that could be stored in a class's constant pool as such
             * constant pool values.
             */
            enum ConstantPoolWrapper {

                /**
                 * Stores a {@link java.lang.Boolean} as a {@code boolean} and wraps it on load.
                 */
                BOOLEAN(boolean.class, Boolean.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Boolean) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Byte} as a {@code byte} and wraps it on load.
                 */
                BYTE(byte.class, Byte.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Byte) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Short} as a {@code short} and wraps it on load.
                 */
                SHORT(short.class, Short.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Short) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Character} as a {@code char} and wraps it on load.
                 */
                CHARACTER(char.class, Character.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Character) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Integer} as a {@code int} and wraps it on load.
                 */
                INTEGER(int.class, Integer.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(IntegerConstant.forValue((Integer) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Long} as a {@code long} and wraps it on load.
                 */
                LONG(long.class, Long.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(LongConstant.forValue((Long) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Float} as a {@code float} and wraps it on load.
                 */
                FLOAT(float.class, Float.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(FloatConstant.forValue((Float) value));
                    }
                },

                /**
                 * Stores a {@link java.lang.Double} as a {@code double} and wraps it on load.
                 */
                DOUBLE(double.class, Double.class) {
                    @Override
                    protected ArgumentProvider make(Object value) {
                        return new WrappingArgumentProvider(DoubleConstant.forValue((Double) value));
                    }
                };

                /**
                 * The primitive type that can be stored on the constant pool.
                 */
                private final TypeDescription primitiveType;

                /**
                 * The wrapper type that is to be represented.
                 */
                private final TypeDescription wrapperType;

                /**
                 * Creates a new wrapper delegate for a primitive type.
                 *
                 * @param primitiveType The primitive type that can be stored on the constant pool.
                 * @param wrapperType   The wrapper type that is to be represented.
                 */
                ConstantPoolWrapper(Class<?> primitiveType, Class<?> wrapperType) {
                    this.primitiveType = new TypeDescription.ForLoadedType(primitiveType);
                    this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
                }

                /**
                 * Represents the given value by a constant pool value or as a field if this is not possible.
                 *
                 * @param value The value to provide to the bootstrapped method.
                 * @return An argument provider for this value.
                 */
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
                    } else if (value instanceof Class) {
                        return new ForClassValue(new TypeDescription.ForLoadedType((Class<?>) value));
                    } else {
                        return new ForStaticField(value);
                    }
                }

                /**
                 * Creates an argument provider for a given primitive value.
                 *
                 * @param value The wrapper-type value to provide to the bootstrapped method.
                 * @return An argument provider for this value.
                 */
                protected abstract ArgumentProvider make(Object value);

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper." + name();
                }

                /**
                 * An argument provider that loads a primitive value from the constant pool and wraps it.
                 */
                protected class WrappingArgumentProvider implements ArgumentProvider {

                    /**
                     * The stack manipulation that represents the loading of the primitive value.
                     */
                    private final StackManipulation stackManipulation;

                    /**
                     * Creates a new wrapping argument provider.
                     *
                     * @param stackManipulation The stack manipulation that represents the loading of the
                     *                          primitive value.
                     */
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

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && ConstantPoolWrapper.this.equals(((WrappingArgumentProvider) other).getOuter())
                                && stackManipulation.equals(((WrappingArgumentProvider) other).stackManipulation);
                    }

                    /**
                     * Returns the outer instance.
                     *
                     * @return The outer instance.
                     */
                    private ConstantPoolWrapper getOuter() {
                        return ConstantPoolWrapper.this;
                    }

                    @Override
                    public int hashCode() {
                        return stackManipulation.hashCode() + 31 * ConstantPoolWrapper.this.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper.WrappingArgumentProvider{" +
                                "constantPoolWrapper=" + ConstantPoolWrapper.this +
                                ", stackManipulation=" + stackManipulation +
                                '}';
                    }
                }
            }

            /**
             * A resolved {@link net.bytebuddy.instrumentation.InvokeDynamic.InvocationProvider.ArgumentProvider}.
             */
            interface Resolved {

                /**
                 * Returns a stack manipulation that loads the arguments onto the operand stack.
                 *
                 * @return A stack manipulation that loads the arguments onto the operand stack.
                 */
                StackManipulation getLoadInstruction();

                /**
                 * Returns a list of all types of the arguments that were loaded onto the operand stack.
                 *
                 * @return A list of all types of the arguments that were loaded onto the operand stack.
                 */
                List<TypeDescription> getLoadedTypes();

                /**
                 * A simple implementation of a resolved argument provider.
                 */
                class Simple implements Resolved {

                    /**
                     * A stack manipulation that loads the arguments onto the operand stack.
                     */
                    private final StackManipulation stackManipulation;

                    /**
                     * A list of all types of the arguments that were loaded onto the operand stack.
                     */
                    private final List<TypeDescription> loadedTypes;

                    /**
                     * Creates a simple resolved argument provider.
                     *
                     * @param stackManipulation A stack manipulation that loads the argument onto the operand stack.
                     * @param loadedType        The type of the arguments that is loaded onto the operand stack.
                     */
                    public Simple(StackManipulation stackManipulation, TypeDescription loadedType) {
                        this(stackManipulation, Collections.singletonList(loadedType));
                    }

                    /**
                     * Creates a simple resolved argument provider.
                     *
                     * @param stackManipulation A stack manipulation that loads the arguments onto the operand stack.
                     * @param loadedTypes       A list of all types of the arguments that were loaded onto the
                     *                          operand stack.
                     */
                    public Simple(StackManipulation stackManipulation, List<TypeDescription> loadedTypes) {
                        this.stackManipulation = stackManipulation;
                        this.loadedTypes = loadedTypes;
                    }

                    @Override
                    public StackManipulation getLoadInstruction() {
                        return stackManipulation;
                    }

                    @Override
                    public List<TypeDescription> getLoadedTypes() {
                        return loadedTypes;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Simple simple = (Simple) other;
                        return loadedTypes.equals(simple.loadedTypes)
                                && stackManipulation.equals(simple.stackManipulation);
                    }

                    @Override
                    public int hashCode() {
                        int result = stackManipulation.hashCode();
                        result = 31 * result + loadedTypes.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "InvokeDynamic.InvocationProvider.ArgumentProvider.Resolved.Simple{" +
                                "stackManipulation=" + stackManipulation +
                                ", loadedTypes=" + loadedTypes +
                                '}';
                    }
                }
            }

            /**
             * An argument provider that loads the intercepted instance.
             */
            class ForThisInstance implements ArgumentProvider {

                /**
                 * The type as which the intercepted instance should be loaded onto the operand stack.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new argument provider for the instance of the instrumented type.
                 *
                 * @param typeDescription The type as which the instrumented type should be loaded onto the operand stack.
                 */
                public ForThisInstance(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot get this instance from static method: " + instrumentedMethod);
                    } else if (!instrumentedType.isAssignableTo(typeDescription)) {
                        throw new IllegalStateException(instrumentedType + " is not assignable to " + instrumentedType);
                    }
                    return new Resolved.Simple(MethodVariableAccess.REFERENCE.loadOffset(0), typeDescription);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForThisInstance) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForThisInstance{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }

            /**
             * An argument provider for a value that is stored in a randomly named static field.
             */
            class ForStaticField implements ArgumentProvider {

                /**
                 * The prefix of any field generated by this argument provider.
                 */
                private static final String FIELD_PREFIX = "invokeDynamic";

                /**
                 * The field modifier for the randomly created fields.
                 */
                private static final int FIELD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

                /**
                 * The value that is stored in the static field.
                 */
                private final Object value;

                /**
                 * The name of the field.
                 */
                private final String name;

                /**
                 * Creates a new argument provider that stores the given value in a static field.
                 *
                 * @param value The value that is to be provided to the bootstrapped method.
                 */
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
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForStaticField{" +
                            "value=" + value +
                            ", name='" + name + '\'' +
                            '}';
                }
            }

            /**
             * An argument provider that loads a value from an instance field.
             */
            class ForInstanceField implements ArgumentProvider {

                /**
                 * The modifier for the generated instance fields.
                 */
                private static final int FIELD_MODIFIER = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;

                /**
                 * The name of the field.
                 */
                private final String fieldName;

                /**
                 * The type of the field.
                 */
                private final TypeDescription fieldType;

                /**
                 * Creates a new argument provider that provides a value from an instance field.
                 *
                 * @param fieldName The name of the field.
                 * @param fieldType The type of the field.
                 */
                public ForInstanceField(String fieldName, TypeDescription fieldType) {
                    this.fieldName = fieldName;
                    this.fieldType = fieldType;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot access " + fieldName + " from " + instrumentedMethod);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(MethodVariableAccess.REFERENCE.loadOffset(0),
                            FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(fieldName)).getOnly()).getter())
                            , fieldType);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType.withField(fieldName, fieldType, FIELD_MODIFIER);
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
                    int result = fieldName.hashCode();
                    result = 31 * result + fieldType.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForInstanceField{" +
                            "fieldName='" + fieldName + '\'' +
                            ", fieldType=" + fieldType +
                            '}';
                }
            }

            /**
             * Provides an argument from an existing field.
             */
            class ForExistingField implements ArgumentProvider {

                /**
                 * The name of the field.
                 */
                private final String fieldName;

                /**
                 * Creates a new argument provider that loads the value of an existing field.
                 *
                 * @param fieldName The name of the field.
                 */
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
                    }
                    while (currentType != null && (fieldDescription == null || !fieldDescription.isVisibleTo(instrumentedType)));
                    if (fieldDescription == null || !fieldDescription.isVisibleTo(instrumentedType)) {
                        throw new IllegalStateException(instrumentedType + " does not define a visible field " + fieldName);
                    } else if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot access non-static " + fieldDescription + " from " + instrumentedMethod);
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(
                            fieldDescription.isStatic()
                                    ? StackManipulation.LegalTrivial.INSTANCE
                                    : MethodVariableAccess.REFERENCE.loadOffset(0),
                            FieldAccess.forField(fieldDescription).getter()
                    ), fieldDescription.getFieldType());
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
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForExistingField{" +
                            "fieldName='" + fieldName + '\'' +
                            '}';
                }
            }

            /**
             * An argument provider that loads an argument of the intercepted method.
             */
            class ForMethodParameter implements ArgumentProvider {

                /**
                 * The index of the parameter.
                 */
                private final int index;

                /**
                 * Creates an argument provider for an argument of the intercepted method.
                 *
                 * @param index The index of the parameter.
                 */
                public ForMethodParameter(int index) {
                    this.index = index;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    ParameterList parameters = instrumentedMethod.getParameters();
                    if (index >= parameters.size()) {
                        throw new IllegalStateException("No parameter " + index + " for " + instrumentedMethod);
                    }
                    return new Resolved.Simple(MethodVariableAccess.forType(parameters.get(index).getTypeDescription())
                            .loadOffset(instrumentedMethod.getParameters().get(index).getOffset()), parameters.get(index).getTypeDescription());
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
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForMethodParameter{" +
                            "index=" + index +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code boolean} value.
             */
            class ForBooleanValue implements ArgumentProvider {

                /**
                 * The represented {@code boolean} value.
                 */
                private final boolean value;

                /**
                 * Creates a new argument provider for a {@code boolean} value.
                 *
                 * @param value The represented {@code boolean} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForBooleanValue) other).value;
                }

                @Override
                public int hashCode() {
                    return (value ? 1 : 0);
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForBooleanValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code byte} value.
             */
            class ForByteValue implements ArgumentProvider {

                /**
                 * The represented {@code byte} value.
                 */
                private final byte value;

                /**
                 * Creates a new argument provider for a {@code byte} value.
                 *
                 * @param value The represented {@code byte} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForByteValue) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForByteValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code short} value.
             */
            class ForShortValue implements ArgumentProvider {

                /**
                 * The represented {@code short} value.
                 */
                private final short value;

                /**
                 * Creates a new argument provider for a {@code short} value.
                 *
                 * @param value The represented {@code short} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForShortValue) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForShortValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code char} value.
             */
            class ForCharacterValue implements ArgumentProvider {

                /**
                 * The represented {@code char} value.
                 */
                private final char value;

                /**
                 * Creates a new argument provider for a {@code char} value.
                 *
                 * @param value The represented {@code char} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForCharacterValue) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForCharacterValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code int} value.
             */
            class ForIntegerValue implements ArgumentProvider {

                /**
                 * The represented {@code int} value.
                 */
                private final int value;

                /**
                 * Creates a new argument provider for a {@code int} value.
                 *
                 * @param value The represented {@code int} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForIntegerValue) other).value;
                }

                @Override
                public int hashCode() {
                    return value;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForIntegerValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code long} value.
             */
            class ForLongValue implements ArgumentProvider {

                /**
                 * The represented {@code long} value.
                 */
                private final long value;

                /**
                 * Creates a new argument provider for a {@code long} value.
                 *
                 * @param value The represented {@code long} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value == ((ForLongValue) other).value;
                }

                @Override
                public int hashCode() {
                    return (int) (value ^ (value >>> 32));
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForLongValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code float} value.
             */
            class ForFloatValue implements ArgumentProvider {

                /**
                 * The represented {@code float} value.
                 */
                private final float value;

                /**
                 * Creates a new argument provider for a {@code float} value.
                 *
                 * @param value The represented {@code float} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && Float.compare(((ForFloatValue) other).value, value) == 0;
                }

                @Override
                public int hashCode() {
                    return (value != +0.0f ? Float.floatToIntBits(value) : 0);
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForFloatValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@code double} value.
             */
            class ForDoubleValue implements ArgumentProvider {

                /**
                 * The represented {@code double} value.
                 */
                private final double value;

                /**
                 * Creates a new argument provider for a {@code double} value.
                 *
                 * @param value The represented {@code double} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && Double.compare(((ForDoubleValue) other).value, value) == 0;
                }

                @Override
                public int hashCode() {
                    long temp = Double.doubleToLongBits(value);
                    return (int) (temp ^ (temp >>> 32));
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForDoubleValue{" +
                            "value=" + value +
                            '}';
                }
            }

            /**
             * An argument provider for a {@link java.lang.String} value.
             */
            class ForStringValue implements ArgumentProvider {

                /**
                 * The represented {@link java.lang.String} value.
                 */
                private final String value;

                /**
                 * Creates a new argument provider for a {@link java.lang.String} value.
                 *
                 * @param value The represented {@link java.lang.String} value.
                 */
                public ForStringValue(String value) {
                    this.value = value;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(new TextConstant(value), TypeDescription.STRING);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && value.equals(((ForStringValue) other).value);
                }

                @Override
                public int hashCode() {
                    return value.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForStringValue{" +
                            "value='" + value + '\'' +
                            '}';
                }
            }

            /**
             * An argument provider for a {@link java.lang.Class} constant.
             */
            class ForClassValue implements ArgumentProvider {

                /**
                 * The type that is represented by this constant.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new argument provider for the given type description.
                 *
                 * @param typeDescription The type to represent.
                 */
                public ForClassValue(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public Resolved resolve(TypeDescription instrumentedType,
                                        MethodDescription instrumentedMethod,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
                    return new Resolved.Simple(ClassConstant.of(typeDescription), TypeDescription.CLASS);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForClassValue) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForClassValue{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }

            /**
             * An argument provider for the {@code null} value.
             */
            class ForNullValue implements ArgumentProvider {

                /**
                 * The type to be represented by the {@code null} value.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new argument provider for the {@code null} value.
                 *
                 * @param typeDescription The type to be represented by the {@code null} value.
                 */
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForNullValue) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ArgumentProvider.ForNullValue{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }
        }

        /**
         * Provides the name of the method that is to be bound by a dynamic method call.
         */
        interface NameProvider {

            /**
             * Resolves the name given the intercepted method.
             *
             * @param methodDescription The intercepted method.
             * @return The name of the method to be bound by the bootstrap method.
             */
            String resolve(MethodDescription methodDescription);

            /**
             * A name provider that provides the name of the intercepted method.
             */
            enum ForInterceptedMethod implements NameProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public String resolve(MethodDescription methodDescription) {
                    return methodDescription.getInternalName();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.NameProvider.ForInterceptedMethod." + name();
                }
            }

            /**
             * A name provider that provides an explicit name.
             */
            class ForExplicitName implements NameProvider {

                /**
                 * The name to be provided.
                 */
                private final String internalName;

                /**
                 * Creates a new name provider for an explicit name.
                 *
                 * @param internalName The name to be provided.
                 */
                public ForExplicitName(String internalName) {
                    this.internalName = internalName;
                }

                @Override
                public String resolve(MethodDescription methodDescription) {
                    return internalName;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && internalName.equals(((ForExplicitName) other).internalName);
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.NameProvider.ForExplicitName{" +
                            "internalName='" + internalName + '\'' +
                            '}';
                }
            }
        }

        /**
         * Provides the return type that is requested from the bootstrap method.
         */
        interface ReturnTypeProvider {

            /**
             * Resolves the return type that is requested from the bootstrap method.
             *
             * @param methodDescription The intercepted method.
             * @return The return type that is requested from the bootstrap method.
             */
            TypeDescription resolve(MethodDescription methodDescription);

            /**
             * Requests the return type of the intercepted method.
             */
            enum ForInterceptedMethod implements ReturnTypeProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public TypeDescription resolve(MethodDescription methodDescription) {
                    return methodDescription.getReturnType();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ReturnTypeProvider.ForInterceptedMethod." + name();
                }
            }

            /**
             * Requests an explicit return type.
             */
            class ForExplicitType implements ReturnTypeProvider {

                /**
                 * The requested return type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new return type provider for an explicit return type.
                 *
                 * @param typeDescription The requested return type.
                 */
                public ForExplicitType(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public TypeDescription resolve(MethodDescription methodDescription) {
                    return typeDescription;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && typeDescription.equals(((ForExplicitType) other).typeDescription);
                }

                @Override
                public int hashCode() {
                    return typeDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.ReturnTypeProvider.ForExplicitType{" +
                            "typeDescription=" + typeDescription +
                            '}';
                }
            }
        }

        /**
         * An invocation provider that requests a synthetic dynamic invocation where all arguments are explicitly
         * provided by the user.
         */
        class Default implements InvocationProvider {

            /**
             * The provider for the name of the intercepted method.
             */
            private final NameProvider nameProvider;

            /**
             * The provider for the required return type.
             */
            private final ReturnTypeProvider returnTypeProvider;

            /**
             * The providers for the method arguments in their order.
             */
            private final List<ArgumentProvider> argumentProviders;

            /**
             * Creates a new default invocation provider that provides information and arguments of the
             * intercepted method.
             */
            public Default() {
                this(NameProvider.ForInterceptedMethod.INSTANCE,
                        ReturnTypeProvider.ForInterceptedMethod.INSTANCE,
                        Collections.<ArgumentProvider>singletonList(ArgumentProvider.ForInterceptedMethodInstanceAndParameters.INSTANCE));
            }

            /**
             * Creates a new default invocation provider.
             *
             * @param nameProvider       The provider for the name of the intercepted method.
             * @param returnTypeProvider The provider for the required return type.
             * @param argumentProviders  The providers for the method arguments in their order.
             */
            public Default(NameProvider nameProvider,
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
            public InvocationProvider appendArguments(List<ArgumentProvider> argumentProviders) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        join(this.argumentProviders, argumentProviders));
            }

            @Override
            public InvocationProvider appendArgument(ArgumentProvider argumentProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        join(this.argumentProviders, argumentProvider));
            }

            @Override
            public InvocationProvider withoutArguments() {
                return new Default(nameProvider,
                        returnTypeProvider,
                        Collections.<ArgumentProvider>emptyList());
            }

            @Override
            public InvocationProvider withNameProvider(NameProvider nameProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        argumentProviders);
            }

            @Override
            public InvocationProvider withReturnTypeProvider(ReturnTypeProvider returnTypeProvider) {
                return new Default(nameProvider,
                        returnTypeProvider,
                        argumentProviders);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                for (ArgumentProvider argumentProvider : argumentProviders) {
                    instrumentedType = argumentProvider.prepare(instrumentedType);
                }
                return instrumentedType;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Default that = (Default) other;
                return argumentProviders.equals(that.argumentProviders)
                        && nameProvider.equals(that.nameProvider)
                        && returnTypeProvider.equals(that.returnTypeProvider);
            }

            @Override
            public int hashCode() {
                int result = nameProvider.hashCode();
                result = 31 * result + returnTypeProvider.hashCode();
                result = 31 * result + argumentProviders.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "InvokeDynamic.InvocationProvider.Default{" +
                        "nameProvider=" + nameProvider +
                        ", returnTypeProvider=" + returnTypeProvider +
                        ", argumentProviders=" + argumentProviders +
                        '}';
            }

            /**
             * A target for a synthetically bound method call.
             */
            protected static class Target implements InvocationProvider.Target {

                /**
                 * The name to be passed to the bootstrap method.
                 */
                private final String internalName;

                /**
                 * The return type to be requested from the bootstrapping method.
                 */
                private final TypeDescription returnType;

                /**
                 * The arguments to be passed to the bootstrap method.
                 */
                private final List<ArgumentProvider> argumentProviders;

                /**
                 * The intercepted method.
                 */
                private final MethodDescription instrumentedMethod;

                /**
                 * Creates a new target.
                 *
                 * @param internalName       The name to be passed to the bootstrap method.
                 * @param returnType         The return type to be requested from the bootstrapping method.
                 * @param argumentProviders  The arguments to be passed to the bootstrap method.
                 * @param instrumentedMethod The intercepted method.
                 */
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
                public InvocationProvider.Target.Resolved resolve(TypeDescription instrumentedType,
                                                                  Assigner assigner,
                                                                  boolean dynamicallyTyped) {
                    StackManipulation[] stackManipulation = new StackManipulation[argumentProviders.size()];
                    List<TypeDescription> parameterTypes = new LinkedList<TypeDescription>();
                    int index = 0;
                    for (ArgumentProvider argumentProvider : argumentProviders) {
                        ArgumentProvider.Resolved resolved = argumentProvider.resolve(instrumentedType,
                                instrumentedMethod,
                                assigner,
                                dynamicallyTyped);
                        parameterTypes.addAll(resolved.getLoadedTypes());
                        stackManipulation[index++] = resolved.getLoadInstruction();
                    }
                    return new Resolved.Simple(new StackManipulation.Compound(stackManipulation),
                            internalName,
                            returnType,
                            parameterTypes);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Target target = (Target) other;
                    return argumentProviders.equals(target.argumentProviders)
                            && instrumentedMethod.equals(target.instrumentedMethod)
                            && internalName.equals(target.internalName)
                            && returnType.equals(target.returnType);
                }

                @Override
                public int hashCode() {
                    int result = internalName.hashCode();
                    result = 31 * result + returnType.hashCode();
                    result = 31 * result + argumentProviders.hashCode();
                    result = 31 * result + instrumentedMethod.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "InvokeDynamic.InvocationProvider.Default.Target{" +
                            "internalName='" + internalName + '\'' +
                            ", returnType=" + returnType +
                            ", argumentProviders=" + argumentProviders +
                            ", instrumentedMethod=" + instrumentedMethod +
                            '}';
                }
            }
        }
    }

    /**
     * A termination handler is responsible to handle the return value of a method that is invoked via a
     * {@link net.bytebuddy.instrumentation.InvokeDynamic}.
     */
    protected interface TerminationHandler {

        /**
         * Returns a stack manipulation that handles the method return.
         *
         * @param interceptedMethod The method being intercepted.
         * @param returnType        The return type of the instrumented method.
         * @param assigner          The assigner to use.
         * @param dynamicallyTyped  {@code true} if the assigner should attempt to apply dynamic type conversion.
         * @return A stack manipulation that handles the method return.
         */
        StackManipulation resolve(MethodDescription interceptedMethod,
                                  TypeDescription returnType,
                                  Assigner assigner,
                                  boolean dynamicallyTyped);

        /**
         * Returns the return value of the dynamic invocation from the intercepted method.
         */
        enum ForMethodReturn implements TerminationHandler {

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

            @Override
            public String toString() {
                return "InvokeDynamic.TerminationHandler.ForMethodReturn." + name();
            }
        }

        /**
         * Drops the return value of the dynamic invocation from the operand stack without returning from the
         * intercepted method.
         */
        enum ForChainedInvocation implements TerminationHandler {

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

            @Override
            public String toString() {
                return "InvokeDynamic.TerminationHandler.ForChainedInvocation." + name();
            }
        }
    }

    /**
     * Representation of an {@link net.bytebuddy.instrumentation.InvokeDynamic} instrumentation where the bootstrapped
     * method is passed a {@code this} reference, if available, and any arguments of the instrumented method.
     */
    public static class WithImplicitArguments extends InvokeDynamic {

        /**
         * Creates a new dynamic method invocation with implicit arguments.
         *
         * @param bootstrapMethod    The bootstrap method.
         * @param handleArguments    The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param dynamicallyTyped   {@code true} if the assigner should attempt dynamically-typed assignments.
         */
        protected WithImplicitArguments(MethodDescription bootstrapMethod,
                                        List<?> handleArguments,
                                        InvocationProvider invocationProvider,
                                        TerminationHandler terminationHandler,
                                        Assigner assigner,
                                        boolean dynamicallyTyped) {
            super(bootstrapMethod,
                    handleArguments,
                    invocationProvider,
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        /**
         * Returns an instance of this instrumentation where the bootstrapped method is not passed any arguments.
         *
         * @return This instrumentation where the bootstrapped method is not passed any arguments.
         */
        public InvokeDynamic withoutImplicitArguments() {
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
                    invocationProvider.withoutArguments(),
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        /**
         * Returns an instance of this instrumentation where only the explicit arguments of an intercepted method but
         * not the {@code this} reference, if available, are passed to the bootstrapped method.
         *
         * @return This instrumentation where only the arguments of the intercepted method but not the {@code this}
         * reference of the intercepted method, if available, are passed to the bootstrapped method.
         */
        public InvokeDynamic withMethodArgumentsOnly() {
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
                    invocationProvider
                            .withoutArguments()
                            .appendArgument(InvocationProvider.ArgumentProvider.ForInterceptedMethodParameters.INSTANCE),
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        @Override
        public String toString() {
            return "InvokeDynamic.WithImplicitArguments{" +
                    "bootstrapMethod=" + bootstrapMethod +
                    ", handleArguments=" + handleArguments +
                    ", invocationProvider=" + invocationProvider +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", dynamicallyTyped=" + dynamicallyTyped +
                    '}';
        }
    }

    /**
     * Representation of an {@link net.bytebuddy.instrumentation.InvokeDynamic} instrumentation where the bootstrapped
     * method is passed a {@code this} reference, if available, and any arguments of the instrumented method and
     * where the invocation target is implicit.
     */
    public static class WithImplicitTarget extends WithImplicitArguments {

        /**
         * Creates a new dynamic method invocation with implicit arguments and an implicit invocation target.
         *
         * @param bootstrapMethod    The bootstrap method.
         * @param handleArguments    The arguments that are provided to the bootstrap method.
         * @param invocationProvider The target provided that identifies the method to be bootstrapped.
         * @param terminationHandler A handler that handles the method return.
         * @param assigner           The assigner to be used.
         * @param dynamicallyTyped   {@code true} if the assigner should attempt dynamically-typed assignments.
         */
        protected WithImplicitTarget(MethodDescription bootstrapMethod,
                                     List<?> handleArguments,
                                     InvocationProvider invocationProvider,
                                     TerminationHandler terminationHandler,
                                     Assigner assigner,
                                     boolean dynamicallyTyped) {
            super(bootstrapMethod,
                    handleArguments,
                    invocationProvider,
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type.
         *
         * @param returnType The return type to request from the bootstrapping method.
         * @return This instrumentation where the bootstrap method is requested to bind a method with the given
         * return type.
         */
        public InvokeDynamic.WithImplicitArguments invoke(Class<?> returnType) {
            return invoke(new TypeDescription.ForLoadedType(nonNull(returnType)));
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type.
         *
         * @param returnType The return type to request from the bootstrapping method.
         * @return This instrumentation where the bootstrap method is requested to bind a method with the given
         * return type.
         */
        public InvokeDynamic.WithImplicitArguments invoke(TypeDescription returnType) {
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
                    invocationProvider.withReturnTypeProvider(new InvocationProvider.ReturnTypeProvider.ForExplicitType(nonNull(returnType))),
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        /**
         * Requests the bootstrap method is passed the given method name.
         *
         * @param methodName The method name to pass to the bootstrapping method.
         * @return This instrumentation where the bootstrap method is passed the given method name.
         */
        public InvokeDynamic.WithImplicitArguments invoke(String methodName) {
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
                    invocationProvider.withNameProvider(new InvocationProvider.NameProvider.ForExplicitName(nonNull(methodName))),
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type. Also, the bootstrap method is passed the
         * given method name,
         *
         * @param methodName The method name to pass to the bootstrapping method.
         * @param returnType The return type to request from the bootstrapping method.
         * @return This instrumentation where the bootstrap method is requested to bind a method with the given
         * return type while being passed the given method name.
         */
        public InvokeDynamic.WithImplicitArguments invoke(String methodName, Class<?> returnType) {
            return invoke(methodName, new TypeDescription.ForLoadedType(nonNull(returnType)));
        }

        /**
         * Requests the bootstrap method to bind a method with the given return type. The return type
         * is he assigned to the intercepted method's return type. Also, the bootstrap method is passed the
         * given method name,
         *
         * @param methodName The method name to pass to the bootstrapping method.
         * @param returnType The return type to request from the bootstrapping method.
         * @return This instrumentation where the bootstrap method is requested to bind a method with the given
         * return type while being passed the given method name.
         */
        public InvokeDynamic.WithImplicitArguments invoke(String methodName, TypeDescription returnType) {
            return new WithImplicitArguments(bootstrapMethod,
                    handleArguments,
                    invocationProvider
                            .withNameProvider(new InvocationProvider.NameProvider.ForExplicitName(nonNull(methodName)))
                            .withReturnTypeProvider(new InvocationProvider.ReturnTypeProvider.ForExplicitType(nonNull(returnType))),
                    terminationHandler,
                    assigner,
                    dynamicallyTyped);
        }

        @Override
        public String toString() {
            return "InvokeDynamic.WithImplicitTarget{" +
                    "bootstrapMethod=" + bootstrapMethod +
                    ", handleArguments=" + handleArguments +
                    ", invocationProvider=" + invocationProvider +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", dynamicallyTyped=" + dynamicallyTyped +
                    '}';
        }
    }

    /**
     * The byte code appender to be used by the {@link net.bytebuddy.instrumentation.InvokeDynamic} instrumentation.
     */
    protected class Appender implements ByteCodeAppender {

        /**
         * The instrumented type of the current instrumentation.
         */
        private final TypeDescription instrumentedType;

        /**
         * Creates a new byte code appender for an invoke dynamic instrumentation.
         *
         * @param instrumentedType The instrumented type of the current instrumentation.
         */
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
            InvocationProvider.Target.Resolved target = invocationProvider.make(instrumentedMethod)
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

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Appender appender = (Appender) other;
            return instrumentedType.equals(appender.instrumentedType)
                    && InvokeDynamic.this.equals(appender.getOuter());
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private InvokeDynamic getOuter() {
            return InvokeDynamic.this;
        }

        @Override
        public int hashCode() {
            return instrumentedType.hashCode();
        }

        @Override
        public String toString() {
            return "InvokeDynamic.Appender{" +
                    "invokeDynamic=" + InvokeDynamic.this +
                    ", instrumentedType=" + instrumentedType +
                    '}';
        }
    }
}
