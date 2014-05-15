package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodNameEqualityResolver;
import net.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import net.bytebuddy.instrumentation.method.bytecode.bind.ParameterLengthResolver;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.*;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.VoidAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.FieldAccess;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.*;

/**
 * This instrumentation delegates an method call to another method which can either be {@code static} by providing
 * a reference to a {@link java.lang.Class} or an instance method when another object is provided. The potential
 * targets of the method delegation can further be filtered by applying a filter. The method delegation can be
 * customized by invoking the {@code MethodDelegation}'s several builder methods.
 * <h3>Without any customization, the method delegation will work as follows:</h3>
 * <span style="text-decoration: underline">Binding an instrumented method to a given delegate method</span>
 * <p>&nbsp;</p>
 * A method will be bound parameter by parameter. Considering a method {@code Foo#bar} being bound to a method
 * {@code Qux#baz}, the method delegation will be decided on basis of the following annotations:
 * <ul>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument}:
 * This annotation will bind the {@code n}-th parameter of {@code Foo#bar} to that parameter of {@code Qux#baz}that
 * is annotated with this annotation where {@code n} is the obligatory argument of the {@code @Argument} annotation.</li>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.AllArguments}:
 * This annotation will assign a collection of all parameter of {@code Foo#bar} to that parameter of {@code Qux#baz}
 * that is annotated with {@code AllArguments}.</li>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.This}: A parameter
 * of {@code Qux#baz} that is annotated with {@code This} will be assigned the instance that is instrumented for
 * a non-static method.</li>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall}: A parameter
 * of {@code Qux#baz} that is annotated with {@code SuperCall} will be assigned an instance of a type implementing both
 * {@link java.lang.Runnable} and {@link java.util.concurrent.Callable} which will return the instrumented method on the
 * invocation of either interface's method. The call is made using the original arguments of the method invocation.
 * The return value is only returned for the {@link java.util.concurrent.Callable#call()} method which additionally
 * requires to catch any unchecked exceptions that might be thrown by the original method's implementation.</li>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin}: A parameter of
 * {@code Qux#baz} that is annotated with {@code Origin} is assigned a reference to either a {@link java.lang.reflect.Method}
 * or a {@link java.lang.Class} instance. A {@code Method}-typed parameter is assigned a reference to the original method that
 * is overriden. A {@code Class}-typed parameter is assigned the type of the caller.</li>
 * </ul>
 * If a method is not annotated with any of the above methods, it will be treated as if it was annotated
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument} using the next
 * unbound parameter index of the source method as its parameter. This means that a method
 * {@code Qux#baz(@Argument(2) Object p1, Object p2, @Argument(0) Object p3} would be treated as if {@code p2} was annotated
 * with {@code @Argument(1)}.
 * <p>&nbsp;</p>
 * In addition, the {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType}
 * annotation can instruct a parameter to be bound by a
 * {@link net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner} with considering the
 * runtime type of the parameter.
 * <p>&nbsp;</p>
 * <span style="text-decoration: underline">Selecting among different methods that can be used for binding a method
 * of the instrumented type</span>
 * <p>&nbsp;</p>
 * When deciding between two methods {@code Foo#bar} and {@code Foo#qux} that could both be used to delegating a
 * method call, the following consideration is applied in the given order:
 * <ol>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.BindingPriority}:
 * A method that is annotated with this annotation is given a specific priority where the default priority is set
 * to {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.BindingPriority#DEFAULT}
 * for non-annotated method. A method with a higher priority is considered a better target for delegation.</li>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.MethodNameEqualityResolver}:
 * If a source method {@code Baz#qux} is the source method, it will rather be assigned to {@code Foo#qux} because
 * of their equal names. Similar names and case-insensitive equality are not considered.</li>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver}:
 * The most specific type resolver will consider all bindings that are using the
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument}
 * annotation for resolving a binding conflict. In this context, the resolution will equal the most-specific
 * type resolution that is performed by the Java compiler. This means that a source method {@code Bar#baz(String)}
 * will rather be bound to a method {@code Foo#bar(String)} than {@code Foo#qux(Object)} because the {@code String}
 * type is more specific than the {@code Object} type. If two methods are equally adequate by their parameter types,
 * then the method with the higher numbers of {@code @Argument} annotated parameters is considered as the better
 * delegation target.</li>
 * <li>{@link net.bytebuddy.instrumentation.method.bytecode.bind.ParameterLengthResolver}:
 * If a target methods has a higher number of total parameters that were successfully bound, the method with
 * the higher number will be considered as the better delegation target.</li>
 * </ol>
 * Additionally, if a method is annotated by
 * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.IgnoreForBinding},
 * it is never considered as a target for a method delegation.
 */
public class MethodDelegation implements Instrumentation {

    private static final String NO_METHODS_ERROR_MESSAGE = "The target type does not define any methods for delegation";
    private final InstrumentationDelegate instrumentationDelegate;
    private final List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders;
    private final TargetMethodAnnotationDrivenBinder.DefaultsProvider<?> defaultsProvider;
    private final MethodDelegationBinder.AmbiguityResolver ambiguityResolver;
    private final Assigner assigner;
    private final MethodList methodList;

    /**
     * Creates a new method delegation.
     *
     * @param instrumentationDelegate The instrumentation delegate to use by this method delegator.
     * @param parameterBinders        The parameter binders to use by this method delegator.
     * @param defaultsProvider        The defaults provider to use by this method delegator.
     * @param ambiguityResolver       The ambiguity resolver to use by this method delegator.
     * @param assigner                The assigner to be supplied by this method delegator.
     * @param methodList              A list of methods that should be considered as possible binding targets by
     *                                this method delegator.
     */
    protected MethodDelegation(InstrumentationDelegate instrumentationDelegate,
                               List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                               TargetMethodAnnotationDrivenBinder.DefaultsProvider<?> defaultsProvider,
                               MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                               Assigner assigner,
                               MethodList methodList) {
        this.instrumentationDelegate = instrumentationDelegate;
        this.parameterBinders = parameterBinders;
        this.defaultsProvider = defaultsProvider;
        this.ambiguityResolver = ambiguityResolver;
        this.assigner = assigner;
        this.methodList = isNotEmpty(methodList, NO_METHODS_ERROR_MESSAGE);
    }

    /**
     * Creates an instrumentation where only {@code static} methods of the given type are considered as binding targets.
     *
     * @param type The type containing the {@code static} methods for binding.
     * @return A method delegation instrumentation to the given {@code static} methods.
     */
    public static MethodDelegation to(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("Type must not be null");
        } else if (type.isInterface()) {
            throw new IllegalArgumentException("Cannot delegate to interface " + type);
        } else if (type.isArray()) {
            throw new IllegalArgumentException("Cannot delegate to array " + type);
        } else if (type.isPrimitive()) {
            throw new IllegalArgumentException("Cannot delegate to primitive " + type);
        }
        return new MethodDelegation(InstrumentationDelegate.ForStaticMethod.INSTANCE,
                defaultParameterBinders(),
                defaultDefaultsProvider(),
                defaultAmbiguityResolver(),
                defaultAssigner(),
                new TypeDescription.ForLoadedType(type).getDeclaredMethods().filter(isStatic().and(not(isPrivate()))));
    }

    /**
     * Creates an instrumentation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.instrumentation.MethodDelegation#filter(net.bytebuddy.instrumentation.method.matcher.MethodMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate A delegate instance which will be injected by a type initializer and to which all intercepted
     *                 method calls are delegated to.
     * @return A method delegation instrumentation to the given instance methods.
     */
    public static MethodDelegation to(Object delegate) {
        return to(delegate, defaultMethodLookupEngine());
    }

    /**
     * Identical to {@link net.bytebuddy.instrumentation.MethodDelegation#to(Object)} but uses an explicit
     * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
     *
     * @param delegate           A delegate instance which will be injected by a type initializer and to which all
     *                           intercepted method calls are delegated to.
     * @param methodLookupEngine The method lookup engine to use.
     * @return A method delegation instrumentation to the given instance methods.
     */
    public static MethodDelegation to(Object delegate, MethodLookupEngine methodLookupEngine) {
        return new MethodDelegation(new InstrumentationDelegate.ForStaticFieldInstance(nonNull(delegate)),
                defaultParameterBinders(),
                defaultDefaultsProvider(),
                defaultAmbiguityResolver(),
                defaultAssigner(),
                methodLookupEngine.getReachableMethods(new TypeDescription.ForLoadedType(delegate.getClass()))
                        .filter(not(isStatic().or(isPrivate()).or(isConstructor())))
        );
    }

    /**
     * Creates an instrumentation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.instrumentation.MethodDelegation#filter(net.bytebuddy.instrumentation.method.matcher.MethodMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate  A delegate instance which will be injected by a type initializer and to which all intercepted
     *                  method calls are delegated to.
     * @param fieldName The name of the field for storing the delegate instance.
     * @return A method delegation instrumentation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object delegate, String fieldName) {
        return to(delegate, fieldName, defaultMethodLookupEngine());
    }

    /**
     * Identical to {@link net.bytebuddy.instrumentation.MethodDelegation#to(Object, java.lang.String)} but uses an
     * explicit {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
     *
     * @param delegate           A delegate instance which will be injected by a type initializer and to which all intercepted
     *                           method calls are delegated to.
     * @param fieldName          The name of the field for storing the delegate instance.
     * @param methodLookupEngine The method lookup engine to use.
     * @return A method delegation instrumentation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object delegate, String fieldName, MethodLookupEngine methodLookupEngine) {
        return new MethodDelegation(
                new InstrumentationDelegate.ForStaticFieldInstance(nonNull(delegate), isValidIdentifier(fieldName)),
                defaultParameterBinders(),
                defaultDefaultsProvider(),
                defaultAmbiguityResolver(),
                defaultAssigner(),
                methodLookupEngine.getReachableMethods(new TypeDescription.ForLoadedType(delegate.getClass()))
                        .filter(not(isStatic().or(isPrivate()).or(isConstructor())))
        );
    }

    /**
     * Creates an instrumentation where method calls are delegated to an instance that is manually stored in a field
     * {@code fieldName} that is defined for the instrumented type. The field belongs to any instance of the instrumented
     * type and must be set manually by the user of the instrumented class. Note that this prevents interception of
     * method calls within the constructor of the instrumented class which will instead result in a
     * {@link java.lang.NullPointerException}. Note that this includes methods that were defined by the
     * {@link java.lang.Object} class. You can narrow this default selection by explicitly selecting methods with
     * calling the
     * {@link net.bytebuddy.instrumentation.MethodDelegation#filter(net.bytebuddy.instrumentation.method.matcher.MethodMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     * <p>&nbsp;</p>
     * The field is typically accessed by reflection or by defining an accessor on the instrumented type.
     *
     * @param type      The type of the delegate and the field.
     * @param fieldName The name of the field.
     * @return A method delegation that intercepts method calls by delegating to method calls on the given instance.
     */
    public static MethodDelegation instanceField(Class<?> type, String fieldName) {
        return instanceField(type, fieldName, defaultMethodLookupEngine());
    }

    /**
     * Identical to {@link net.bytebuddy.instrumentation.MethodDelegation#instanceField(Class, String)} but uses an
     * explicit {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
     *
     * @param type               The type of the delegate and the field.
     * @param fieldName          The name of the field.
     * @param methodLookupEngine The method lookup engine to use.
     * @return A method delegation that intercepts method calls by delegating to method calls on the given instance.
     */
    public static MethodDelegation instanceField(Class<?> type, String fieldName, MethodLookupEngine methodLookupEngine) {
        return new MethodDelegation(
                new InstrumentationDelegate.ForInstanceField(new TypeDescription.ForLoadedType(nonNull(type)), isValidIdentifier(fieldName)),
                defaultParameterBinders(),
                defaultDefaultsProvider(),
                defaultAmbiguityResolver(),
                defaultAssigner(),
                methodLookupEngine.getReachableMethods(new TypeDescription.ForLoadedType(type))
                        .filter(not(isStatic().or(isPrivate()).or(isConstructor())))
        );
    }

    /**
     * Creates an instrumentation where method calls are delegated to constructor calls on the given type. As a result,
     * the return values of all instrumented methods must be assignable to
     *
     * @param type The type that should be constructed by the instrumented methods.
     * @return An instrumentation that creates instances of the given type as its result.
     */
    public static MethodDelegation construct(Class<?> type) {
        return new MethodDelegation(new InstrumentationDelegate.ForConstruction(new TypeDescription.ForLoadedType(type)),
                defaultParameterBinders(),
                defaultDefaultsProvider(),
                defaultAmbiguityResolver(),
                defaultAssigner(),
                new TypeDescription.ForLoadedType(type)
                        .getDeclaredMethods()
                        .filter(isConstructor())
        );
    }

    private static List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> defaultParameterBinders() {
        return Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(Argument.Binder.INSTANCE,
                AllArguments.Binder.INSTANCE,
                Origin.Binder.INSTANCE,
                This.Binder.INSTANCE,
                Super.Binder.INSTANCE,
                SuperCall.Binder.INSTANCE);
    }

    private static TargetMethodAnnotationDrivenBinder.DefaultsProvider<?> defaultDefaultsProvider() {
        return Argument.NextUnboundAsDefaultsProvider.INSTANCE;
    }

    private static MethodDelegationBinder.AmbiguityResolver defaultAmbiguityResolver() {
        return MethodDelegationBinder.AmbiguityResolver.Chain.of(
                BindingPriority.Resolver.INSTANCE,
                MethodNameEqualityResolver.INSTANCE,
                MostSpecificTypeResolver.INSTANCE,
                ParameterLengthResolver.INSTANCE
        );
    }

    private static Assigner defaultAssigner() {
        return new VoidAwareAssigner(new PrimitiveTypeAwareAssigner(ReferenceTypeAwareAssigner.INSTANCE), false);
    }

    private static MethodLookupEngine defaultMethodLookupEngine() {
        return new MethodLookupEngine.Default();
    }

    /**
     * Defines an parameter binder to be appended to the already defined parameter binders.
     *
     * @param parameterBinder The parameter binder to append to the already defined parameter binders.
     * @return A method delegation instrumentation that makes use of the given parameter binder.
     */
    public MethodDelegation appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder<?> parameterBinder) {
        return new MethodDelegation(instrumentationDelegate,
                join(parameterBinders, nonNull(parameterBinder)),
                defaultsProvider,
                ambiguityResolver,
                assigner,
                methodList);
    }

    /**
     * Defines a number of parameter binders to be appended to be used by this method delegation.
     *
     * @param parameterBinder The parameter binders to use by this parameter binders.
     * @return A method delegation instrumentation that makes use of the given parameter binders.
     */
    public MethodDelegation defineParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder<?>... parameterBinder) {
        return new MethodDelegation(instrumentationDelegate,
                Arrays.asList(nonNull(parameterBinder)),
                defaultsProvider,
                ambiguityResolver,
                assigner,
                methodList);
    }

    /**
     * A provider for annotation instances on values that are not explicitly annotated.
     *
     * @param defaultsProvider The defaults provider to use.
     * @return A method delegation instrumentation that makes use of the given defaults provider.
     */
    public MethodDelegation defaultsProvider(TargetMethodAnnotationDrivenBinder.DefaultsProvider defaultsProvider) {
        return new MethodDelegation(instrumentationDelegate,
                parameterBinders,
                nonNull(defaultsProvider),
                ambiguityResolver,
                assigner,
                methodList);
    }

    /**
     * Defines an ambiguity resolver to be appended to the already defined ambiguity resolver for resolving binding conflicts.
     *
     * @param ambiguityResolver The ambiguity resolver to append to the already defined ambiguity resolvers.
     * @return A method delegation instrumentation that makes use of the given ambiguity resolver.
     */
    public MethodDelegation appendAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver ambiguityResolver) {
        return defineAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver.Chain
                .of(this.ambiguityResolver, nonNull(ambiguityResolver)));
    }

    /**
     * Defines an ambiguity resolver to be used for resolving binding conflicts.
     *
     * @param ambiguityResolver The ambiguity resolver to use exclusively.
     * @return A method delegation instrumentation that makes use of the given ambiguity resolver.
     */
    public MethodDelegation defineAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver... ambiguityResolver) {
        return new MethodDelegation(instrumentationDelegate,
                parameterBinders,
                defaultsProvider,
                MethodDelegationBinder.AmbiguityResolver.Chain.of(nonNull(ambiguityResolver)),
                assigner,
                methodList);
    }

    /**
     * Applies an assigner to the method delegation that is used for assigning method return and parameter types.
     *
     * @param assigner The assigner to apply.
     * @return A method delegation instrumentation that makes use of the given designer.
     */
    public MethodDelegation assigner(Assigner assigner) {
        return new MethodDelegation(instrumentationDelegate,
                parameterBinders,
                defaultsProvider,
                ambiguityResolver,
                nonNull(assigner),
                methodList);
    }

    /**
     * Applies a filter to target methods that are eligible for delegation.
     *
     * @param methodMatcher A filter where only methods that match the filter are considered for delegation.
     * @return A method delegation with the filter applied.
     */
    public MethodDelegation filter(MethodMatcher methodMatcher) {
        return new MethodDelegation(instrumentationDelegate,
                parameterBinders,
                defaultsProvider,
                ambiguityResolver,
                assigner,
                isNotEmpty(methodList.filter(nonNull(methodMatcher)), NO_METHODS_ERROR_MESSAGE));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentationDelegate.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(TypeDescription instrumentedType) {
        MethodList methodList = this.methodList.filter(isVisibleTo(instrumentedType));
        if (methodList.size() == 0) {
            throw new IllegalStateException("No bindable method is visible to " + instrumentedType);
        }
        return new MethodDelegationByteCodeAppender(instrumentationDelegate.getPreparingStackAssignment(instrumentedType),
                instrumentedType,
                methodList,
                new MethodDelegationBinder.Processor(new TargetMethodAnnotationDrivenBinder(
                        parameterBinders,
                        defaultsProvider,
                        assigner,
                        instrumentationDelegate.getMethodInvoker(instrumentedType)
                ), ambiguityResolver)
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodDelegation that = (MethodDelegation) other;
        return ambiguityResolver.equals(that.ambiguityResolver)
                && assigner.equals(that.assigner)
                && defaultsProvider.equals(that.defaultsProvider)
                && instrumentationDelegate.equals(that.instrumentationDelegate)
                && methodList.equals(that.methodList)
                && parameterBinders.equals(that.parameterBinders);
    }

    @Override
    public int hashCode() {
        int result = instrumentationDelegate.hashCode();
        result = 31 * result + parameterBinders.hashCode();
        result = 31 * result + defaultsProvider.hashCode();
        result = 31 * result + ambiguityResolver.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + methodList.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MethodDelegation{" +
                "instrumentationDelegate=" + instrumentationDelegate +
                ", parameterBinders=" + parameterBinders +
                ", defaultsProvider=" + defaultsProvider +
                ", ambiguityResolver=" + ambiguityResolver +
                ", assigner=" + assigner +
                ", methodList=" + methodList +
                '}';
    }

    /**
     * An instrumentation delegate is responsible for executing the actual method delegation.
     */
    protected static interface InstrumentationDelegate {

        /**
         * Prepares the instrumented type.
         *
         * @param instrumentedType The instrumented type to be prepared.
         * @return The instrumented type after it was prepared.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * Returns the stack manipulation responsible for preparing the instance representing the instrumentation.
         *
         * @param instrumentedType A description of the instrumented type to which the instrumentation is applied.
         * @return A stack manipulation representing the preparation.
         */
        StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType);

        /**
         * Returns the method invoker responsible for invoking the delegation method.
         *
         * @param instrumentedType The instrumented type to which the instrumentation is applied.
         * @return A method invoker responsible for invoking the delegation method.
         */
        MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType);

        /**
         * An instrumentation applied to a static method.
         */
        static enum ForStaticMethod implements InstrumentationDelegate {

            /**
             * The singleton instance.
             */
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

        /**
         * An instrumentation applied on a static field.
         */
        static class ForStaticFieldInstance implements InstrumentationDelegate {

            private static final String PREFIX = "methodDelegate";

            private final String fieldName;
            private final Object delegate;

            /**
             * Creates a new instrumentation to an instance that is stored in a {@code static} field.
             * The field name will be created randomly.
             *
             * @param delegate The actual delegation target.
             */
            public ForStaticFieldInstance(Object delegate) {
                this(delegate, String.format("%s$%d", PREFIX, delegate.hashCode()));
            }

            /**
             * Creates a new instrumentation to an instance that is stored in a {@code static} field.
             *
             * @param delegate  The actual delegation target.
             * @param fieldName The name of the field for storing the delegate instance.
             */
            public ForStaticFieldInstance(Object delegate, String fieldName) {
                this.delegate = delegate;
                this.fieldName = fieldName;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(fieldName,
                        new TypeDescription.ForLoadedType(delegate.getClass()),
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC)
                        .withInitializer(TypeInitializer.ForStaticField.nonAccessible(fieldName, delegate));
            }

            @Override
            public StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType) {
                return FieldAccess.forField(instrumentedType.getDeclaredFields().named(fieldName)).getter();
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return new MethodDelegationBinder.MethodInvoker.Virtual(new TypeDescription.ForLoadedType(delegate.getClass()));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && delegate.equals(((ForStaticFieldInstance) other).delegate)
                        && fieldName.equals(((ForStaticFieldInstance) other).fieldName);
            }

            @Override
            public int hashCode() {
                return 31 * fieldName.hashCode() + delegate.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegation.InstrumentationDelegate.ForStaticFieldInstance{" +
                        "fieldName='" + fieldName + '\'' +
                        ", delegate=" + delegate +
                        '}';
            }
        }

        /**
         * An instrumentation applied on an instance field.
         */
        static class ForInstanceField implements InstrumentationDelegate {

            private final String fieldName;
            private final TypeDescription fieldType;

            /**
             * Creates a new instance field instrumentation delegate.
             *
             * @param fieldType A description of the type that is the target of the instrumentation and thus also the
             *                  field type.
             * @param fieldName The name of the field.
             */
            public ForInstanceField(TypeDescription fieldType, String fieldName) {
                this.fieldType = fieldType;
                this.fieldName = fieldName;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(fieldName, fieldType, Opcodes.ACC_PUBLIC);
            }

            @Override
            public StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(MethodVariableAccess.forType(instrumentedType).loadFromIndex(0),
                        FieldAccess.forField(instrumentedType.getDeclaredFields().named(fieldName)).getter());
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return new MethodDelegationBinder.MethodInvoker.Virtual(fieldType);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && fieldName.equals(((ForInstanceField) other).fieldName)
                        && fieldType.equals(((ForInstanceField) other).fieldType);
            }

            @Override
            public int hashCode() {
                return 31 * fieldName.hashCode() + fieldType.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegation.InstrumentationDelegate.ForInstanceField{" +
                        "fieldName='" + fieldName + '\'' +
                        ", fieldType=" + fieldType +
                        '}';
            }
        }

        /**
         * An instrumentation that creates new instances of a given type.
         */
        static class ForConstruction implements InstrumentationDelegate {

            private final TypeDescription typeDescription;

            /**
             * Creates a new constructor instrumentation.
             *
             * @param typeDescription The type to be constructed.
             */
            public ForConstruction(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(
                        TypeCreation.forType(typeDescription),
                        Duplication.SINGLE);
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return MethodDelegationBinder.MethodInvoker.Simple.INSTANCE;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && typeDescription.equals(((ForConstruction) other).typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegation.InstrumentationDelegate.ForConstruction{" +
                        "typeDescription=" + typeDescription +
                        '}';
            }
        }
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

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            MethodDelegationByteCodeAppender that = (MethodDelegationByteCodeAppender) other;
            return instrumentedType.equals(that.instrumentedType)
                    && preparingStackAssignment.equals(that.preparingStackAssignment)
                    && processor.equals(that.processor)
                    && targetMethods.equals(that.targetMethods);
        }

        @Override
        public int hashCode() {
            int result = preparingStackAssignment.hashCode();
            result = 31 * result + instrumentedType.hashCode();
            result = 31 * result + targetMethods.hashCode();
            result = 31 * result + processor.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodDelegationByteCodeAppender{" +
                    "preparingStackAssignment=" + preparingStackAssignment +
                    ", instrumentedType=" + instrumentedType +
                    ", targetMethods=" + targetMethods +
                    ", processor=" + processor +
                    '}';
        }
    }
}
