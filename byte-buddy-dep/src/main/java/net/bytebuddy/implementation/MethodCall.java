package net.bytebuddy.implementation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.collection.ArrayAccess;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * This {@link Implementation} allows the invocation of a specified method while
 * providing explicit arguments to this method.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodCall implements Implementation.Composable {

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
    protected final List<ArgumentLoader.Factory> argumentLoaders;

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
                         List<ArgumentLoader.Factory> argumentLoaders,
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
        return invoke(new MethodDescription.ForLoadedMethod(method));
    }

    /**
     * Invokes the given constructor on the instance of the instrumented type.
     *
     * @param constructor The constructor to invoke.
     * @return A method call implementation that invokes the given constructor without providing any arguments.
     */
    public static WithoutSpecifiedTarget invoke(Constructor<?> constructor) {
        return invoke(new MethodDescription.ForLoadedConstructor(constructor));
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
        return invoke(new MethodLocator.ForExplicitMethod(methodDescription));
    }

    /**
     * Invokes a unique virtual method of the instrumented type that is matched by the specified matcher.
     *
     * @param matcher The matcher to identify the method to invoke.
     * @return A method call for the uniquely identified method.
     */
    public static WithoutSpecifiedTarget invoke(ElementMatcher<? super MethodDescription> matcher) {
        return invoke(matcher, MethodGraph.Compiler.DEFAULT);
    }

    /**
     * Invokes a unique virtual method of the instrumented type that is matched by the specified matcher.
     *
     * @param matcher             The matcher to identify the method to invoke.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A method call for the uniquely identified method.
     */
    public static WithoutSpecifiedTarget invoke(ElementMatcher<? super MethodDescription> matcher, MethodGraph.Compiler methodGraphCompiler) {
        return invoke(new MethodLocator.ForElementMatcher(matcher, methodGraphCompiler));
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
        return new WithoutSpecifiedTarget(methodLocator);
    }

    /**
     * Invokes the instrumented method recursively. Invoking this method on the same instance causes a {@link StackOverflowError} due to
     * infinite recursion.
     *
     * @return A method call that invokes the method being instrumented.
     */
    public static WithoutSpecifiedTarget invokeSelf() {
        return new WithoutSpecifiedTarget(MethodLocator.ForInstrumentedMethod.INSTANCE);
    }

    /**
     * Invokes the instrumented method as a super method call on the instance itself. This is a shortcut for {@code invokeSelf().onSuper()}.
     *
     * @return A method call that invokes the method being instrumented as a super method call.
     */
    public static MethodCall invokeSuper() {
        return invokeSelf().onSuper();
    }

    /**
     * Implements a method by invoking the provided {@link Callable}. The return value of the provided object is casted to the implemented method's
     * return type, if necessary.
     *
     * @param callable The callable to invoke when a method is intercepted.
     * @return A composable method implementation that invokes the given callable.
     */
    public static Composable call(Callable<?> callable) {
        try {
            return invoke(Callable.class.getMethod("call")).on(callable, Callable.class).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Could not locate Callable::call method", exception);
        }
    }

    /**
     * Implements a method by invoking the provided {@link Runnable}. If the instrumented method returns a value, {@code null} is returned.
     *
     * @param runnable The runnable to invoke when a method is intercepted.
     * @return A composable method implementation that invokes the given runnable.
     */
    public static Composable run(Runnable runnable) {
        try {
            return invoke(Runnable.class.getMethod("run")).on(runnable, Runnable.class).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Could not locate Runnable::run method", exception);
        }
    }

    /**
     * Invokes the given constructor in order to create an instance.
     *
     * @param constructor The constructor to invoke.
     * @return A method call that invokes the given constructor without providing any arguments.
     */
    public static MethodCall construct(Constructor<?> constructor) {
        return construct(new MethodDescription.ForLoadedConstructor(constructor));
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
                Collections.<ArgumentLoader.Factory>emptyList(),
                MethodInvoker.ForContextualInvocation.INSTANCE,
                TerminationHandler.RETURNING,
                Assigner.DEFAULT,
                Assigner.Typing.STATIC);
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
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(argument.length);
        for (Object anArgument : argument) {
            argumentLoaders.add(ArgumentLoader.ForStackManipulation.of(anArgument));
        }
        return with(argumentLoaders);
    }

    /**
     * Defines the given types to be provided as arguments to the invoked method where the represented types
     * are stored in the generated class's constant pool.
     *
     * @param typeDescription The type descriptions to provide as arguments.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall with(TypeDescription... typeDescription) {
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(typeDescription.length);
        for (TypeDescription aTypeDescription : typeDescription) {
            argumentLoaders.add(new ArgumentLoader.ForStackManipulation(ClassConstant.of(aTypeDescription), Class.class));
        }
        return with(argumentLoaders);
    }

    /**
     * Defines the given enumeration values to be provided as arguments to the invoked method where the values
     * are read from the enumeration class on demand.
     *
     * @param enumerationDescription The enumeration descriptions to provide as arguments.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall with(EnumerationDescription... enumerationDescription) {
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(enumerationDescription.length);
        for (EnumerationDescription anEnumerationDescription : enumerationDescription) {
            argumentLoaders.add(new ArgumentLoader.ForStackManipulation(FieldAccess.forEnumeration(anEnumerationDescription), anEnumerationDescription.getEnumerationType()));
        }
        return with(argumentLoaders);
    }

    /**
     * Defines the given Java instances to be provided as arguments to the invoked method where the given
     * instances are stored in the generated class's constant pool.
     *
     * @param javaConstant The Java instances to provide as arguments.
     * @return A method call that hands the provided arguments to the invoked method.
     */
    public MethodCall with(JavaConstant... javaConstant) {
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(javaConstant.length);
        for (JavaConstant aJavaConstant : javaConstant) {
            argumentLoaders.add(new ArgumentLoader.ForStackManipulation(aJavaConstant.asStackManipulation(), aJavaConstant.getType()));
        }
        return with(argumentLoaders);
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
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(argument.length);
        for (Object anArgument : argument) {
            argumentLoaders.add(anArgument == null
                    ? ArgumentLoader.ForNullConstant.INSTANCE
                    : new ArgumentLoader.ForInstance.Factory(anArgument));
        }
        return with(argumentLoaders);
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
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(index.length);
        for (int anIndex : index) {
            if (anIndex < 0) {
                throw new IllegalArgumentException("Negative index: " + anIndex);
            }
            argumentLoaders.add(new ArgumentLoader.ForMethodParameter.Factory(anIndex));
        }
        return with(argumentLoaders);
    }

    /**
     * Adds all arguments of the instrumented method as arguments to the invoked method to this method call.
     *
     * @return A method call that hands all arguments of the instrumented method to the invoked method.
     */
    public MethodCall withAllArguments() {
        return with(ArgumentLoader.ForMethodParameter.OfInstrumentedMethod.INSTANCE);
    }

    /**
     * Adds an array containing all arguments of the instrumented method to this method call.
     *
     * @return A method call that adds an array containing all arguments of the instrumented method to the invoked method.
     */
    public MethodCall withArgumentArray() {
        return with(ArgumentLoader.ForMethodParameterArray.ForInstrumentedMethod.INSTANCE);
    }

    /**
     * <p>
     * Creates a method call where the parameter with {@code index} is expected to be an array and where each element of the array
     * is expected to represent an argument for the method being invoked.
     * </p>
     * <p>
     * <b>Note</b>: This is typically used in combination with dynamic type assignments which is activated via
     * {@link MethodCall#withAssigner(Assigner, Assigner.Typing)} using a {@link Assigner.Typing#DYNAMIC}.
     * </p>
     *
     * @param index The index of the parameter.
     * @return A method call that loads {@code size} elements from the array handed to the instrumented method as argument {@code index}.
     */
    public MethodCall withArgumentArrayElements(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("A parameter index cannot be negative: " + index);
        }
        return with(new ArgumentLoader.ForMethodParameterArrayElement.OfInvokedMethod(index));
    }

    /**
     * <p>
     * Creates a method call where the parameter with {@code index} is expected to be an array and where {@code size} elements are loaded
     * from the array as arguments for the invoked method.
     * </p>
     * <p>
     * <b>Note</b>: This is typically used in combination with dynamic type assignments which is activated via
     * {@link MethodCall#withAssigner(Assigner, Assigner.Typing)} using a {@link Assigner.Typing#DYNAMIC}.
     * </p>
     *
     * @param index The index of the parameter.
     * @param size  The amount of elements to load from the array.
     * @return A method call that loads {@code size} elements from the array handed to the instrumented method as argument {@code index}.
     */
    public MethodCall withArgumentArrayElements(int index, int size) {
        return withArgumentArrayElements(index, 0, size);
    }

    /**
     * <p>
     * Creates a method call where the parameter with {@code index} is expected to be an array and where {@code size} elements are loaded
     * from the array as arguments for the invoked method. The first element is loaded from index {@code start}.
     * </p>
     * <p>
     * <b>Note</b>: This is typically used in combination with dynamic type assignments which is activated via
     * {@link MethodCall#withAssigner(Assigner, Assigner.Typing)} using a {@link Assigner.Typing#DYNAMIC}.
     * </p>
     *
     * @param index The index of the parameter.
     * @param start The first array index to consider.
     * @param size  The amount of elements to load from the array with increasing index from {@code start}.
     * @return A method call that loads {@code size} elements from the array handed to the instrumented method as argument {@code index}.
     */
    public MethodCall withArgumentArrayElements(int index, int start, int size) {
        if (index < 0) {
            throw new IllegalArgumentException("A parameter index cannot be negative: " + index);
        } else if (start < 0) {
            throw new IllegalArgumentException("An array index cannot be negative: " + start);
        } else if (size == 0) {
            return this;
        } else if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative: " + size);
        }
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(size);
        for (int position = 0; position < size; position++) {
            argumentLoaders.add(new ArgumentLoader.ForMethodParameterArrayElement.OfParameter(index, start + position));
        }
        return with(argumentLoaders);
    }

    /**
     * Assigns the {@code this} reference to the next parameter.
     *
     * @return This method call where the next parameter is a assigned a reference to the {@code this} reference
     * of the instance of the intercepted method.
     */
    public MethodCall withThis() {
        return with(ArgumentLoader.ForThisReference.Factory.INSTANCE);
    }

    /**
     * Assigns the {@link java.lang.Class} value of the instrumented type.
     *
     * @return This method call where the next parameter is a assigned a reference to the {@link java.lang.Class}
     * value of the instrumented type.
     */
    public MethodCall withOwnType() {
        return with(ArgumentLoader.ForInstrumentedType.Factory.INSTANCE);
    }

    /**
     * Defines a method call which fetches a value from a list of existing fields.
     *
     * @param name The names of the fields.
     * @return A method call which assigns the next parameters to the values of the given fields.
     */
    public MethodCall withField(String... name) {
        return withField(FieldLocator.ForClassHierarchy.Factory.INSTANCE, name);
    }

    /**
     * Defines a method call which fetches a value from a list of existing fields.
     *
     * @param fieldLocatorFactory The field locator factory to use.
     * @param name                The names of the fields.
     * @return A method call which assigns the next parameters to the values of the given fields.
     */
    public MethodCall withField(FieldLocator.Factory fieldLocatorFactory, String... name) {
        List<ArgumentLoader.Factory> argumentLoaders = new ArrayList<ArgumentLoader.Factory>(name.length);
        for (String aName : name) {
            argumentLoaders.add(new ArgumentLoader.ForField.Factory(aName, fieldLocatorFactory));
        }
        return with(argumentLoaders);
    }

    /**
     * Adds a stack manipulation as an assignment to the next parameter.
     *
     * @param stackManipulation The stack manipulation loading the value.
     * @param type              The type of the argument being loaded.
     * @return A method call that adds the stack manipulation as the next argument to the invoked method.
     */
    public MethodCall with(StackManipulation stackManipulation, Type type) {
        return with(stackManipulation, TypeDefinition.Sort.describe(type));
    }

    /**
     * Adds a stack manipulation as an assignment to the next parameter.
     *
     * @param stackManipulation The stack manipulation loading the value.
     * @param typeDefinition    The type of the argument being loaded.
     * @return A method call that adds the stack manipulation as the next argument to the invoked method.
     */
    public MethodCall with(StackManipulation stackManipulation, TypeDefinition typeDefinition) {
        return with(new ArgumentLoader.ForStackManipulation(stackManipulation, typeDefinition));
    }

    /**
     * Defines a method call that resolves arguments by the supplied argument loader factories.
     *
     * @param argumentLoader The argument loaders to apply to the subsequent arguments of the
     * @return A method call that adds the arguments of the supplied argument loaders to the invoked method.
     */
    public MethodCall with(ArgumentLoader.Factory... argumentLoader) {
        return with(Arrays.asList(argumentLoader));
    }

    /**
     * Defines a method call that resolves arguments by the supplied argument loader factories.
     *
     * @param argumentLoaders The argument loaders to apply to the subsequent arguments of the
     * @return A method call that adds the arguments of the supplied argument loaders to the invoked method.
     */
    public MethodCall with(List<? extends ArgumentLoader.Factory> argumentLoaders) {
        return new MethodCall(methodLocator,
                targetHandler,
                CompoundList.of(this.argumentLoaders, argumentLoaders),
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
    public Implementation.Composable withAssigner(Assigner assigner, Assigner.Typing typing) {
        return new MethodCall(methodLocator,
                targetHandler,
                argumentLoaders,
                methodInvoker,
                terminationHandler,
                assigner,
                typing);
    }

    @Override
    public Implementation andThen(Implementation implementation) {
        return new Implementation.Compound(new MethodCall(methodLocator,
                targetHandler,
                argumentLoaders,
                methodInvoker,
                TerminationHandler.DROPPING,
                assigner,
                typing), implementation);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        for (ArgumentLoader.Factory argumentLoader : argumentLoaders) {
            instrumentedType = argumentLoader.prepare(instrumentedType);
        }
        return targetHandler.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationTarget);
    }

    /**
     * A method locator is responsible for identifying the method that is to be invoked
     * by a {@link net.bytebuddy.implementation.MethodCall}.
     */
    public interface MethodLocator {

        /**
         * Resolves the method to be invoked.
         *
         * @param instrumentedType   The instrumented type.
         * @param instrumentedMethod The method being instrumented.
         * @return The method to invoke.
         */
        MethodDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod);

        /**
         * A method locator that simply returns the intercepted method.
         */
        enum ForInstrumentedMethod implements MethodLocator {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                return instrumentedMethod;
            }
        }

        /**
         * Invokes a given method.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
            protected ForExplicitMethod(MethodDescription methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public MethodDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                return methodDescription;
            }
        }

        /**
         * A method locator that identifies a unique virtual method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForElementMatcher implements MethodLocator {

            /**
             * The matcher to use.
             */
            private final ElementMatcher<? super MethodDescription> matcher;

            /**
             * The method graph compiler to use.
             */
            private final MethodGraph.Compiler methodGraphCompiler;

            /**
             * Creates a new method locator for an element matcher.
             *
             * @param matcher             The matcher to use.
             * @param methodGraphCompiler The method graph compiler to use.
             */
            protected ForElementMatcher(ElementMatcher<? super MethodDescription> matcher, MethodGraph.Compiler methodGraphCompiler) {
                this.matcher = matcher;
                this.methodGraphCompiler = methodGraphCompiler;
            }

            @Override
            public MethodDescription resolve(TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
                MethodList<?> candidates = methodGraphCompiler.compile(instrumentedType).listNodes().asMethodList().filter(matcher);
                if (candidates.size() == 1) {
                    return candidates.getOnly();
                } else {
                    throw new IllegalStateException(instrumentedType + " does not define exactly one virtual method for " + matcher);
                }
            }
        }
    }

    /**
     * An argument loader is responsible for loading an argument for an invoked method
     * onto the operand stack.
     */
    public interface ArgumentLoader {

        /**
         * Loads the argument that is represented by this instance onto the operand stack.
         *
         * @param target   The target parameter.
         * @param assigner The assigner to be used.
         * @param typing   Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return The stack manipulation that loads the represented argument onto the stack.
         */
        StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing);

        /**
         * A factory that produces {@link ArgumentLoader}s for a given instrumented method.
         */
        interface Factory {

            /**
             * Prepares the instrumented type in order to allow the loading of the represented argument.
             *
             * @param instrumentedType The instrumented type.
             * @return The prepared instrumented type.
             */
            InstrumentedType prepare(InstrumentedType instrumentedType);

            /**
             * Creates any number of argument loaders for an instrumentation.
             *
             * @param instrumentedType   The instrumented type.
             * @param instrumentedMethod The instrumented method.
             * @param invokedMethod      The invoked method.
             * @return Any number of argument loaders to supply for the method call.
             */
            List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod);
        }

        /**
         * An argument loader that loads the {@code null} value onto the operand stack.
         */
        enum ForNullConstant implements ArgumentLoader, Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                return Collections.<ArgumentLoader>singletonList(this);
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                if (target.getType().isPrimitive()) {
                    throw new IllegalStateException("Cannot assign null to " + target);
                }
                return NullConstant.INSTANCE;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }
        }

        /**
         * An argument loader that assigns the {@code this} reference to a parameter.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForThisReference implements ArgumentLoader {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates an argument loader that supplies the {@code this} instance as an argument.
             *
             * @param instrumentedType The instrumented type.
             */
            public ForThisReference(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.loadThis(),
                        assigner.assign(instrumentedType.asGenericType(), target.getType(), typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + instrumentedType + " to " + target);
                }
                return stackManipulation;
            }

            /**
             * A factory for an argument loader that supplies the {@code this} value as an argument.
             */
            public enum Factory implements ArgumentLoader.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException(instrumentedMethod + " is static and cannot supply an invoker instance");
                    }
                    return Collections.<ArgumentLoader>singletonList(new ForThisReference(instrumentedType));
                }
            }
        }

        /**
         * Loads the instrumented type onto the operand stack.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForInstrumentedType implements ArgumentLoader {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates an argument loader for supporting the instrumented type as a type constant as an argument.
             *
             * @param instrumentedType The instrumented type.
             */
            public ForInstrumentedType(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        ClassConstant.of(instrumentedType),
                        assigner.assign(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Class.class), target.getType(), typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign Class value to " + target);
                }
                return stackManipulation;
            }

            /**
             * A factory for an argument loader that supplies the instrumented type as an argument.
             */
            public enum Factory implements ArgumentLoader.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    return Collections.<ArgumentLoader>singletonList(new ForInstrumentedType(instrumentedType));
                }
            }
        }

        /**
         * Loads a parameter of the instrumented method onto the operand stack.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForMethodParameter implements ArgumentLoader {

            /**
             * The index of the parameter to be loaded onto the operand stack.
             */
            private final int index;

            /**
             * The instrumented method.
             */
            private final MethodDescription instrumentedMethod;

            /**
             * Creates an argument loader for a parameter of the instrumented method.
             *
             * @param index              The index of the parameter to be loaded onto the operand stack.
             * @param instrumentedMethod The instrumented method.
             */
            public ForMethodParameter(int index, MethodDescription instrumentedMethod) {
                this.index = index;
                this.instrumentedMethod = instrumentedMethod;
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                ParameterDescription parameterDescription = instrumentedMethod.getParameters().get(index);
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.load(parameterDescription),
                        assigner.assign(parameterDescription.getType(), target.getType(), typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + parameterDescription + " to " + target + " for " + instrumentedMethod);
                }
                return stackManipulation;
            }

            /**
             * A factory for argument loaders that supplies all arguments of the instrumented method as arguments.
             */
            protected enum OfInstrumentedMethod implements ArgumentLoader.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(instrumentedMethod.getParameters().size());
                    for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                        argumentLoaders.add(new ForMethodParameter(parameterDescription.getIndex(), instrumentedMethod));
                    }
                    return argumentLoaders;
                }
            }

            /**
             * A factory for an argument loader that supplies a method parameter as an argument.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements ArgumentLoader.Factory {

                /**
                 * The index of the parameter to be loaded onto the operand stack.
                 */
                private final int index;

                /**
                 * Creates a factory for an argument loader that supplies a method parameter as an argument.
                 *
                 * @param index The index of the parameter to supply.
                 */
                public Factory(int index) {
                    this.index = index;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    if (index >= instrumentedMethod.getParameters().size()) {
                        throw new IllegalStateException(instrumentedMethod + " does not have a parameter with index " + index);
                    }
                    return Collections.<ArgumentLoader>singletonList(new ForMethodParameter(index, instrumentedMethod));
                }
            }
        }

        /**
         * Loads an array containing all arguments of a method.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForMethodParameterArray implements ArgumentLoader {

            /**
             * The parameters to load.
             */
            private final ParameterList<?> parameters;

            /**
             * Creates an argument loader that loads the supplied parameters onto the operand stack.
             *
             * @param parameters The parameters to load.
             */
            public ForMethodParameterArray(ParameterList<?> parameters) {
                this.parameters = parameters;
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                TypeDescription.Generic componentType;
                if (target.getType().represents(Object.class)) {
                    componentType = TypeDescription.Generic.OBJECT;
                } else if (target.getType().isArray()) {
                    componentType = target.getType().getComponentType();
                } else {
                    throw new IllegalStateException();
                }
                List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(parameters.size());
                for (ParameterDescription parameter : parameters) {
                    StackManipulation stackManipulation = new StackManipulation.Compound(
                            MethodVariableAccess.load(parameter),
                            assigner.assign(parameter.getType(), componentType, typing)
                    );
                    if (stackManipulation.isValid()) {
                        stackManipulations.add(stackManipulation);
                    } else {
                        throw new IllegalStateException("Cannot assign " + parameter + " to " + componentType);
                    }
                }
                return new StackManipulation.Compound(ArrayFactory.forType(componentType).withValues(stackManipulations));
            }

            /**
             * A factory that creates an arguments loader that loads all parameters of the instrumented method contained in an array.
             */
            public enum ForInstrumentedMethod implements ArgumentLoader.Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    return Collections.<ArgumentLoader>singletonList(new ForMethodParameterArray(instrumentedMethod.getParameters()));
                }
            }
        }

        /**
         * An argument loader that loads an element of a parameter of an array type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForMethodParameterArrayElement implements ArgumentLoader {

            /**
             * The parameter to load the array from.
             */
            private final ParameterDescription parameterDescription;

            /**
             * The array index to load.
             */
            private final int index;

            /**
             * Creates an argument loader for a parameter of the instrumented method where an array element is assigned to the invoked method.
             *
             * @param parameterDescription The parameter from which to load an array element.
             * @param index                The array index to load.
             */
            public ForMethodParameterArrayElement(ParameterDescription parameterDescription, int index) {
                this.parameterDescription = parameterDescription;
                this.index = index;
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        MethodVariableAccess.load(parameterDescription),
                        IntegerConstant.forValue(index),
                        ArrayAccess.of(parameterDescription.getType().getComponentType()).load(),
                        assigner.assign(parameterDescription.getType().getComponentType(), target.getType(), typing)
                );
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + parameterDescription.getType().getComponentType() + " to " + target);
                }
                return stackManipulation;
            }

            /**
             * Creates an argument loader for an array element that of a specific parameter.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class OfParameter implements ArgumentLoader.Factory {

                /**
                 * The parameter index.
                 */
                private final int index;

                /**
                 * The array index to load.
                 */
                private final int arrayIndex;

                /**
                 * Creates a factory for an argument loader that loads a given parameter's array value.
                 *
                 * @param index      The index of the parameter.
                 * @param arrayIndex The array index to load.
                 */
                public OfParameter(int index, int arrayIndex) {
                    this.index = index;
                    this.arrayIndex = arrayIndex;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    if (instrumentedMethod.getParameters().size() <= index) {
                        throw new IllegalStateException(instrumentedMethod + " does not declare a parameter with index " + index);
                    } else if (!instrumentedMethod.getParameters().get(index).getType().isArray()) {
                        throw new IllegalStateException("Cannot access an item from non-array parameter " + instrumentedMethod.getParameters().get(index));
                    }
                    return Collections.<ArgumentLoader>singletonList(new ForMethodParameterArrayElement(instrumentedMethod.getParameters().get(index), arrayIndex));
                }
            }

            /**
             * An argument loader factory that loads an array element from a parameter for each argument of the invoked method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfInvokedMethod implements ArgumentLoader.Factory {

                /**
                 * The parameter index.
                 */
                private final int index;

                /**
                 * Creates an argument loader factory for an invoked method.
                 *
                 * @param index The parameter index.
                 */
                public OfInvokedMethod(int index) {
                    this.index = index;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    if (instrumentedMethod.getParameters().size() <= index) {
                        throw new IllegalStateException(instrumentedMethod + " does not declare a parameter with index " + index);
                    } else if (!instrumentedMethod.getParameters().get(index).getType().isArray()) {
                        throw new IllegalStateException("Cannot access an item from non-array parameter " + instrumentedMethod.getParameters().get(index));
                    }
                    List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(instrumentedMethod.getParameters().size());
                    for (int index = 0; index < invokedMethod.getParameters().size(); index++) {
                        argumentLoaders.add(new ForMethodParameterArrayElement(instrumentedMethod.getParameters().get(this.index), index++));
                    }
                    return argumentLoaders;
                }
            }
        }

        /**
         * Loads a value onto the operand stack that is stored in a static field.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForInstance implements ArgumentLoader {

            /**
             * The description of the field.
             */
            private final FieldDescription fieldDescription;

            /**
             * Creates an argument loader that supplies the value of a static field as an argument.
             *
             * @param fieldDescription The description of the field.
             */
            public ForInstance(FieldDescription fieldDescription) {
                this.fieldDescription = fieldDescription;
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        FieldAccess.forField(fieldDescription).read(),
                        assigner.assign(fieldDescription.getType(), target.getType(), typing));
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + fieldDescription.getType() + " to " + target);
                }
                return stackManipulation;
            }

            /**
             * A factory that supplies the value of a static field as an argument.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements ArgumentLoader.Factory {

                /**
                 * The name prefix of the field to store the argument.
                 */
                private static final String FIELD_PREFIX = "methodCall";

                /**
                 * The value to be stored in the field.
                 */
                private final Object value;

                /**
                 * The name of the field.
                 */
                @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
                private final String name;

                /**
                 * Creates a factory that loads the value of a static field as an argument.
                 *
                 * @param value The value to supply as an argument.
                 */
                public Factory(Object value) {
                    this.value = value;
                    name = FIELD_PREFIX + "$" + RandomString.make();
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType
                            .withField(new FieldDescription.Token(name,
                                    Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                    new TypeDescription.Generic.OfNonGenericType.ForLoadedType(value.getClass())))
                            .withInitializer(new LoadedTypeInitializer.ForStaticField(name, value));
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    return Collections.<ArgumentLoader>singletonList(new ForInstance(instrumentedType.getDeclaredFields().filter(named(name)).getOnly()));
                }
            }
        }

        /**
         * Loads the value of an existing field onto the operand stack.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForField implements ArgumentLoader {

            /**
             * The field containing the loaded value.
             */
            private final FieldDescription fieldDescription;

            /**
             * The instrumented method.
             */
            private final MethodDescription instrumentedMethod;

            /**
             * Creates a new argument loader for loading an existing field.
             *
             * @param fieldDescription   The field containing the loaded value.
             * @param instrumentedMethod The instrumented method.
             */
            public ForField(FieldDescription fieldDescription, MethodDescription instrumentedMethod) {
                this.fieldDescription = fieldDescription;
                this.instrumentedMethod = instrumentedMethod;
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                if (!fieldDescription.isStatic() && instrumentedMethod.isStatic()) {
                    throw new IllegalStateException("Cannot access non-static " + fieldDescription + " from " + instrumentedMethod);
                }
                StackManipulation stackManipulation = new StackManipulation.Compound(
                        fieldDescription.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.loadThis(),
                        FieldAccess.forField(fieldDescription).read(),
                        assigner.assign(fieldDescription.getType(), target.getType(), typing)
                );
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot assign " + fieldDescription + " to " + target);
                }
                return stackManipulation;
            }

            /**
             * A factory for an argument loaded that loads the value of an existing field as an argument.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Factory implements ArgumentLoader.Factory {

                /**
                 * The name of the field.
                 */
                private final String name;

                /**
                 * The field locator to use.
                 */
                private final FieldLocator.Factory fieldLocatorFactory;

                /**
                 * Creates a new argument loader for an existing field.
                 *
                 * @param name                The name of the field.
                 * @param fieldLocatorFactory The field locator to use.
                 */
                public Factory(String name, FieldLocator.Factory fieldLocatorFactory) {
                    this.name = name;
                    this.fieldLocatorFactory = fieldLocatorFactory;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                    FieldLocator.Resolution resolution = fieldLocatorFactory.make(instrumentedType).locate(name);
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Could not locate field '" + name + "' on " + instrumentedType);
                    }
                    return Collections.<ArgumentLoader>singletonList(new ForField(resolution.getField(), instrumentedMethod));
                }
            }
        }

        /**
         * Loads a stack manipulation resulting in a specific type as an argument.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForStackManipulation implements ArgumentLoader, Factory {

            /**
             * The stack manipulation to load.
             */
            private final StackManipulation stackManipulation;

            /**
             * The type of the resulting value.
             */
            private final TypeDefinition typeDefinition;

            /**
             * Creates an argument loader that loads a stack manipulation as an argument.
             *
             * @param stackManipulation The stack manipulation to load.
             * @param type              The type of the resulting value.
             */
            public ForStackManipulation(StackManipulation stackManipulation, Type type) {
                this(stackManipulation, TypeDescription.Generic.Sort.describe(type));
            }

            /**
             * Creates an argument loader that loads a stack manipulation as an argument.
             *
             * @param stackManipulation The stack manipulation to load.
             * @param typeDefinition    The type of the resulting value.
             */
            public ForStackManipulation(StackManipulation stackManipulation, TypeDefinition typeDefinition) {
                this.stackManipulation = stackManipulation;
                this.typeDefinition = typeDefinition;
            }

            /**
             * Creates an argument loader that loads the supplied value as a constant. If the value cannot be represented
             * in the constant pool, a field is created to store the value.
             *
             * @param value The value to load as an argument or {@code null}.
             * @return An appropriate argument loader.
             */
            public static ArgumentLoader.Factory of(Object value) {
                if (value == null) {
                    return ForNullConstant.INSTANCE;
                } else if (value instanceof String) {
                    return new ForStackManipulation(new TextConstant((String) value), String.class);
                } else if (value instanceof Boolean) {
                    return new ForStackManipulation(IntegerConstant.forValue((Boolean) value), boolean.class);
                } else if (value instanceof Byte) {
                    return new ForStackManipulation(IntegerConstant.forValue((Byte) value), byte.class);
                } else if (value instanceof Short) {
                    return new ForStackManipulation(IntegerConstant.forValue((Short) value), short.class);
                } else if (value instanceof Character) {
                    return new ForStackManipulation(IntegerConstant.forValue((Character) value), char.class);
                } else if (value instanceof Integer) {
                    return new ForStackManipulation(IntegerConstant.forValue((Integer) value), int.class);
                } else if (value instanceof Long) {
                    return new ForStackManipulation(LongConstant.forValue((Long) value), long.class);
                } else if (value instanceof Float) {
                    return new ForStackManipulation(FloatConstant.forValue((Float) value), float.class);
                } else if (value instanceof Double) {
                    return new ForStackManipulation(DoubleConstant.forValue((Double) value), double.class);
                } else if (value instanceof Class) {
                    return new ForStackManipulation(ClassConstant.of(new TypeDescription.ForLoadedType((Class<?>) value)), Class.class);
                } else if (JavaType.METHOD_HANDLE.getTypeStub().isInstance(value)) {
                    return new ForStackManipulation(JavaConstant.MethodHandle.ofLoaded(value).asStackManipulation(), JavaType.METHOD_HANDLE.getTypeStub());
                } else if (JavaType.METHOD_TYPE.getTypeStub().isInstance(value)) {
                    return new ForStackManipulation(JavaConstant.MethodType.ofLoaded(value).asStackManipulation(), JavaType.METHOD_TYPE.getTypeStub());
                } else if (value instanceof Enum<?>) {
                    EnumerationDescription enumerationDescription = new EnumerationDescription.ForLoadedEnumeration((Enum<?>) value);
                    return new ForStackManipulation(FieldAccess.forEnumeration(enumerationDescription), enumerationDescription.getEnumerationType());
                } else {
                    return new ForInstance.Factory(value);
                }
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public List<ArgumentLoader> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, MethodDescription invokedMethod) {
                return Collections.<ArgumentLoader>singletonList(this);
            }

            @Override
            public StackManipulation resolve(ParameterDescription target, Assigner assigner, Assigner.Typing typing) {
                StackManipulation assigment = assigner.assign(typeDefinition.asGenericType(), target.getType(), typing);
                if (!assigment.isValid()) {
                    throw new IllegalStateException("Cannot assign " + target + " to " + typeDefinition);
                }
                return new StackManipulation.Compound(stackManipulation, assigment);
            }
        }
    }

    /**
     * A target handler is responsible for invoking a method for a
     * {@link net.bytebuddy.implementation.MethodCall}.
     */
    protected interface TargetHandler extends InstrumentedType.Prepareable {

        /**
         * Creates a stack manipulation that represents the method's invocation.
         *
         * @param invokedMethod      The method to be invoked.
         * @param instrumentedMethod The instrumented method.
         * @param instrumentedType   The instrumented type.  @return A stack manipulation that invokes the method.
         * @param assigner           The assigner to use.
         * @param typing             The typing to apply.
         * @return A stack manipulation that loads the method target onto the operand stack.
         */
        StackManipulation resolve(MethodDescription invokedMethod,
                                  MethodDescription instrumentedMethod,
                                  TypeDescription instrumentedType,
                                  Assigner assigner,
                                  Assigner.Typing typing);

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
            public StackManipulation resolve(MethodDescription invokedMethod,
                                             MethodDescription instrumentedMethod,
                                             TypeDescription instrumentedType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                if (instrumentedMethod.isStatic() && !invokedMethod.isStatic() && !invokedMethod.isConstructor()) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " from " + instrumentedMethod);
                }
                return new StackManipulation.Compound(
                        invokedMethod.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.loadThis(),
                        invokedMethod.isConstructor()
                                ? Duplication.SINGLE
                                : StackManipulation.Trivial.INSTANCE
                );
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
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
            public StackManipulation resolve(MethodDescription invokedMethod,
                                             MethodDescription instrumentedMethod,
                                             TypeDescription instrumentedType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                return new StackManipulation.Compound(TypeCreation.of(invokedMethod.getDeclaringType().asErasure()), Duplication.SINGLE);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }
        }

        /**
         * A target handler that invokes a method on an instance that is stored in a static field.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForValue implements TargetHandler {

            /**
             * The name prefix of the field to store the instance.
             */
            private static final String FIELD_PREFIX = "invocationTarget";

            /**
             * The target on which the method is to be invoked.
             */
            private final Object target;

            /**
             * The type of the field.
             */
            private final TypeDescription.Generic fieldType;

            /**
             * The name of the field to store the target.
             */
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
            private final String name;

            /**
             * Creates a new target handler for a static field.
             *
             * @param target    The target on which the method is to be invoked.
             * @param fieldType The type of the field.
             */
            protected ForValue(Object target, TypeDescription.Generic fieldType) {
                this.target = target;
                this.fieldType = fieldType;
                name = FIELD_PREFIX + "$" + RandomString.make();
            }

            @Override
            public StackManipulation resolve(MethodDescription invokedMethod,
                                             MethodDescription instrumentedMethod,
                                             TypeDescription instrumentedType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                StackManipulation stackManipulation = assigner.assign(fieldType, invokedMethod.getDeclaringType().asGenericType(), typing);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " on " + fieldType);
                }
                return new StackManipulation.Compound(
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter(named(name)).getOnly()).read(),
                        stackManipulation
                );
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(name,
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE | Opcodes.ACC_SYNTHETIC,
                                fieldType))
                        .withInitializer(new LoadedTypeInitializer.ForStaticField(name, target));
            }
        }

        /**
         * Creates a target handler that stores the instance to invoke a method on in an instance field.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForField implements TargetHandler {

            /**
             * The name of the field.
             */
            private final String name;

            /**
             * The field locator factory to use.
             */
            private final FieldLocator.Factory fieldLocatorFactory;

            /**
             * Creates a new target handler for storing a method invocation target in an
             * instance field.
             *
             * @param name                The name of the field.
             * @param fieldLocatorFactory The field locator factory to use.
             */
            protected ForField(String name, FieldLocator.Factory fieldLocatorFactory) {
                this.name = name;
                this.fieldLocatorFactory = fieldLocatorFactory;
            }

            @Override
            public StackManipulation resolve(MethodDescription invokedMethod, MethodDescription instrumentedMethod, TypeDescription instrumentedType, Assigner assigner, Assigner.Typing typing) {
                FieldLocator.Resolution resolution = fieldLocatorFactory.make(instrumentedType).locate(name);
                if (!resolution.isResolved()) {
                    throw new IllegalStateException("Could not locate field name " + name + " on " + instrumentedType);
                } else if (!resolution.getField().isStatic() && !instrumentedType.isAssignableTo(resolution.getField().getDeclaringType().asErasure())) {
                    throw new IllegalStateException("Cannot access " + resolution.getField() + " from " + instrumentedType);
                } else if (!invokedMethod.isInvokableOn(resolution.getField().getType().asErasure())) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " on " + resolution.getField());
                } else if (!invokedMethod.isAccessibleTo(instrumentedType)) {
                    throw new IllegalStateException("Cannot access " + invokedMethod + " from " + instrumentedType);
                }
                StackManipulation stackManipulation = assigner.assign(resolution.getField().getType(), invokedMethod.getDeclaringType().asGenericType(), typing);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " on " + resolution.getField());
                }
                return new StackManipulation.Compound(invokedMethod.isStatic()
                        ? StackManipulation.Trivial.INSTANCE
                        : MethodVariableAccess.loadThis(), FieldAccess.forField(resolution.getField()).read(), stackManipulation);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }
        }

        /**
         * A target handler that loads the parameter of the given index as the target object.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForMethodParameter implements TargetHandler {

            /**
             * The index of the instrumented method's parameter that is the target of the method invocation.
             */
            private final int index;

            /**
             * Creates a new target handler for the instrumented method's argument.
             *
             * @param index The index of the instrumented method's parameter that is the target of the method invocation.
             */
            protected ForMethodParameter(int index) {
                this.index = index;
            }

            @Override
            public StackManipulation resolve(MethodDescription invokedMethod,
                                             MethodDescription instrumentedMethod,
                                             TypeDescription instrumentedType,
                                             Assigner assigner,
                                             Assigner.Typing typing) {
                if (instrumentedMethod.getParameters().size() < index) {
                    throw new IllegalArgumentException(instrumentedMethod + " does not have a parameter with index " + index);
                }
                ParameterDescription parameterDescription = instrumentedMethod.getParameters().get(index);
                StackManipulation stackManipulation = assigner.assign(parameterDescription.getType(), invokedMethod.getDeclaringType().asGenericType(), typing);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " on " + parameterDescription.getType());
                }
                return new StackManipulation.Compound(MethodVariableAccess.load(parameterDescription), stackManipulation);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
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
         * @param invokedMethod        The method to be invoked.
         * @param implementationTarget The implementation target of the instrumented instance.
         * @return A stack manipulation that represents the method invocation.
         */
        StackManipulation invoke(MethodDescription invokedMethod, Target implementationTarget);

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
            public StackManipulation invoke(MethodDescription invokedMethod, Target implementationTarget) {
                if (invokedMethod.isVirtual() && !invokedMethod.isInvokableOn(implementationTarget.getInstrumentedType())) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " on " + implementationTarget.getInstrumentedType());
                }
                return invokedMethod.isVirtual()
                        ? MethodInvocation.invoke(invokedMethod).virtual(implementationTarget.getInstrumentedType())
                        : MethodInvocation.invoke(invokedMethod);
            }
        }

        /**
         * Applies a virtual invocation on a given type.
         */
        @HashCodeAndEqualsPlugin.Enhance
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

            /**
             * Creates a new method invoking for a virtual method invocation.
             *
             * @param type The type to virtually invoke the method upon.
             */
            protected ForVirtualInvocation(Class<?> type) {
                this(new TypeDescription.ForLoadedType(type));
            }

            @Override
            public StackManipulation invoke(MethodDescription invokedMethod, Target implementationTarget) {
                if (!invokedMethod.isVirtual()) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " virtually");
                } else if (!invokedMethod.isInvokableOn(typeDescription.asErasure())) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " on " + typeDescription);
                } else if (!typeDescription.asErasure().isAccessibleTo(implementationTarget.getInstrumentedType())) {
                    throw new IllegalStateException(typeDescription + " is not accessible to " + implementationTarget.getInstrumentedType());
                }
                return MethodInvocation.invoke(invokedMethod).virtual(typeDescription.asErasure());
            }

            /**
             * A method invoker for a virtual method that uses an implicit target type.
             */
            public enum WithImplicitType implements MethodInvoker {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public StackManipulation invoke(MethodDescription invokedMethod, Target implementationTarget) {
                    if (!invokedMethod.isVirtual()) {
                        throw new IllegalStateException("Cannot invoke " + invokedMethod + " virtually");
                    }
                    return MethodInvocation.invoke(invokedMethod);
                }
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
            public StackManipulation invoke(MethodDescription invokedMethod, Target implementationTarget) {
                if (implementationTarget.getInstrumentedType().getSuperClass() == null) {
                    throw new IllegalStateException("Cannot invoke super method for " + implementationTarget.getInstrumentedType());
                } else if (!invokedMethod.isInvokableOn(implementationTarget.getOriginType().asErasure())) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " as super method of " + implementationTarget.getInstrumentedType());
                }
                StackManipulation stackManipulation = implementationTarget.invokeDominant(invokedMethod.asSignatureToken());
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " as a super method");
                }
                return stackManipulation;
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
            public StackManipulation invoke(MethodDescription invokedMethod, Target implementationTarget) {
                if (!invokedMethod.isInvokableOn(implementationTarget.getInstrumentedType())) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " as default method of " + implementationTarget.getInstrumentedType());
                }
                StackManipulation stackManipulation = implementationTarget.invokeDefault(invokedMethod.asSignatureToken(), invokedMethod.getDeclaringType().asErasure());
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot invoke " + invokedMethod + " on " + implementationTarget.getInstrumentedType());
                }
                return stackManipulation;
            }
        }
    }

    /**
     * A termination handler is responsible to handle the return value of a method that is invoked via a
     * {@link net.bytebuddy.implementation.MethodCall}.
     */
    protected enum TerminationHandler {

        /**
         * A termination handler that returns the invoked method's return value.
         */
        RETURNING {
            @Override
            public StackManipulation resolve(MethodDescription invokedMethod, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                StackManipulation stackManipulation = assigner.assign(invokedMethod.isConstructor()
                        ? invokedMethod.getDeclaringType().asGenericType()
                        : invokedMethod.getReturnType(), instrumentedMethod.getReturnType(), typing);
                if (!stackManipulation.isValid()) {
                    throw new IllegalStateException("Cannot return " + invokedMethod.getReturnType() + " from " + instrumentedMethod);
                }
                return new StackManipulation.Compound(stackManipulation, MethodReturn.of(instrumentedMethod.getReturnType()));
            }
        },

        /**
         * A termination handler that drops the invoked method's return value.
         */
        DROPPING {
            @Override
            protected StackManipulation resolve(MethodDescription invokedMethod, MethodDescription instrumentedMethod, Assigner assigner, Assigner.Typing typing) {
                return Removal.of(invokedMethod.isConstructor()
                        ? invokedMethod.getDeclaringType()
                        : invokedMethod.getReturnType());
            }
        };

        /**
         * Returns a stack manipulation that handles the method return.
         *
         * @param invokedMethod      The method that was invoked by the method call.
         * @param instrumentedMethod The method being intercepted.
         * @param assigner           The assigner to be used.
         * @param typing             Indicates if dynamic type castings should be attempted for incompatible assignments.
         * @return A stack manipulation that handles the method return.
         */
        protected abstract StackManipulation resolve(MethodDescription invokedMethod,
                                                     MethodDescription instrumentedMethod,
                                                     Assigner assigner,
                                                     Assigner.Typing typing);
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
                    Collections.<ArgumentLoader.Factory>emptyList(),
                    MethodInvoker.ForContextualInvocation.INSTANCE,
                    TerminationHandler.RETURNING,
                    Assigner.DEFAULT,
                    Assigner.Typing.STATIC);
        }

        /**
         * Invokes the specified method on the given instance.
         *
         * @param target The object on which the method is to be invoked upon.
         * @return A method call that invokes the provided method on the given object.
         */
        @SuppressWarnings("unchecked")
        public MethodCall on(Object target) {
            return on(target, (Class) target.getClass());
        }

        /**
         * Invokes the specified method on the given instance.
         *
         * @param target The object on which the method is to be invoked upon.
         * @param type   The object's type.
         * @param <T>    The type of the object.
         * @return A method call that invokes the provided method on the given object.
         */
        public <T> MethodCall on(T target, Class<? super T> type) {
            return new MethodCall(methodLocator,
                    new TargetHandler.ForValue(target, new TypeDescription.Generic.OfNonGenericType.ForLoadedType(type)),
                    argumentLoaders,
                    new MethodInvoker.ForVirtualInvocation(type),
                    terminationHandler,
                    assigner,
                    typing);
        }

        /**
         * Invokes the specified method on the instrumented method's argument of the given index.
         *
         * @param index The index of the method's argument on which the specified method should be invoked.
         * @return A method call that invokes the provided method on the given method argument.
         */
        public MethodCall onArgument(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("An argument index cannot be negative: " + index);
            }
            return new MethodCall(methodLocator,
                    new TargetHandler.ForMethodParameter(index),
                    argumentLoaders,
                    MethodInvoker.ForVirtualInvocation.WithImplicitType.INSTANCE,
                    terminationHandler,
                    assigner,
                    typing);
        }

        /**
         * Invokes a method on the object stored in the specified field.
         *
         * @param name The name of the field.
         * @return A method call that invokes the given method on an instance that is read from a field.
         */
        public MethodCall onField(String name) {
            return onField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
        }

        /**
         * Invokes a method on the object stored in the specified field.
         *
         * @param name                The name of the field.
         * @param fieldLocatorFactory The field locator factory to use for locating the field.
         * @return A method call that invokes the given method on an instance that is read from a field.
         */
        public MethodCall onField(String name, FieldLocator.Factory fieldLocatorFactory) {
            return new MethodCall(methodLocator,
                    new TargetHandler.ForField(name, fieldLocatorFactory),
                    argumentLoaders,
                    MethodInvoker.ForVirtualInvocation.WithImplicitType.INSTANCE,
                    terminationHandler,
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
                    terminationHandler,
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
                    terminationHandler,
                    assigner,
                    typing);
        }
    }

    /**
     * The appender being used to implement a {@link net.bytebuddy.implementation.MethodCall}.
     */
    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
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
            MethodDescription invokedMethod = methodLocator.resolve(implementationTarget.getInstrumentedType(), instrumentedMethod);
            if (!invokedMethod.isVisibleTo(implementationTarget.getInstrumentedType())) {
                throw new IllegalStateException("Cannot invoke " + invokedMethod + " from " + implementationContext.getInstrumentedType());
            }
            List<ArgumentLoader> argumentLoaders = new ArrayList<ArgumentLoader>(MethodCall.this.argumentLoaders.size());
            for (ArgumentLoader.Factory argumentLoader : MethodCall.this.argumentLoaders) {
                argumentLoaders.addAll(argumentLoader.make(implementationTarget.getInstrumentedType(), instrumentedMethod, invokedMethod));
            }
            ParameterList<?> parameters = invokedMethod.getParameters();
            if (parameters.size() != argumentLoaders.size()) {
                throw new IllegalStateException(invokedMethod + " does not take " + argumentLoaders.size() + " arguments");
            }
            Iterator<? extends ParameterDescription> parameterIterator = parameters.iterator();
            List<StackManipulation> argumentInstructions = new ArrayList<StackManipulation>(argumentLoaders.size());
            for (ArgumentLoader argumentLoader : argumentLoaders) {
                argumentInstructions.add(argumentLoader.resolve(parameterIterator.next(), assigner, typing));
            }
            StackManipulation.Size size = new StackManipulation.Compound(
                    targetHandler.resolve(invokedMethod, instrumentedMethod, implementationTarget.getInstrumentedType(), assigner, typing),
                    new StackManipulation.Compound(argumentInstructions),
                    methodInvoker.invoke(invokedMethod, implementationTarget),
                    terminationHandler.resolve(invokedMethod, instrumentedMethod, assigner, typing)
            ).apply(methodVisitor, implementationContext);
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }
}
