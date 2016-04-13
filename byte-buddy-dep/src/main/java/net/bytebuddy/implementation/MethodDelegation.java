package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * This implementation delegates an method call to another method which can either be {@code static} by providing
 * a reference to a {@link java.lang.Class} or an instance method when another object is provided. The potential
 * targets of the method delegation can further be filtered by applying a filter. The method delegation can be
 * customized by invoking the {@code MethodDelegation}'s several builder methods.
 * <h3>Without any customization, the method delegation will work as follows:</h3>
 * <span style="text-decoration: underline">Binding an instrumented method to a given delegate method</span>
 * <p>&nbsp;</p>
 * A method will be bound parameter by parameter. Considering a method {@code Foo#bar} being bound to a method
 * {@code Qux#baz}, the method delegation will be decided on basis of the following annotations:
 * <ul>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Argument}:
 * This annotation will bind the {@code n}-th parameter of {@code Foo#bar} to that parameter of {@code Qux#baz}that
 * is annotated with this annotation where {@code n} is the obligatory argument of the {@code @Argument} annotation.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.AllArguments}:
 * This annotation will assign a collection of all parameters of {@code Foo#bar} to that parameter of {@code Qux#baz}
 * that is annotated with {@code AllArguments}.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.This}: A parameter
 * of {@code Qux#baz} that is annotated with {@code This} will be assigned the instance that is instrumented for
 * a non-static method.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Super}: A parameter that is annotated with
 * this annotation is assigned a proxy that allows calling an instrumented type's super methods.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Default}: A parameter that is annotated with
 * this annotation is assigned a proxy that allows calling an instrumented type's directly implemented interfaces'
 * default methods.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.SuperCall}: A parameter
 * of {@code Qux#baz} that is annotated with {@code SuperCall} will be assigned an instance of a type implementing both
 * {@link java.lang.Runnable} and {@link java.util.concurrent.Callable} which will invoke the instrumented method on the
 * invocation of either interface's method. The call is made using the original arguments of the method invocation.
 * The return value is only emitted for the {@link java.util.concurrent.Callable#call()} method which additionally
 * requires to catch any unchecked exceptions that might be thrown by the original method's implementation. If a
 * source method is abstract, using this annotation excludes the method with this parameter annotation from being bound
 * to this source method.
 * </li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.DefaultCall}:
 * This annotation is similar to the {@link net.bytebuddy.implementation.bind.annotation.SuperCall}
 * annotation but it invokes a default method that is compatible to this method. If a source method does not represent
 * a default method, using this annotation excludes the method with this parameter annotation from being bound to this
 * source method.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Origin}: A parameter of
 * {@code Qux#baz} that is annotated with {@code Origin} is assigned a reference to either a {@link java.lang.reflect.Method},
 * a {@link java.lang.reflect.Constructor}, a {@code java.lang.reflect.Executable} or a {@link java.lang.Class} instance.
 * A {@code Method}-typed, {@code Constructor} or {@code Executable} parameter is assigned a reference to the original
 * method that is instrumented. A {@code Class}-typed parameter is assigned the type of the caller. Furthermore, {@code MethodType}
 * and {@code MethodHandle} parameters are also supported. When using the annotation on a {@link java.lang.String} type,
 * the intercepted method's {@code toString} value is injected. The same holds for a parameter of type {@code int} that receives
 * the modifiers of the instrumented method.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.StubValue}: Assigns the (boxed) default value of the
 * intercepted method's return type to the parameter. If the return type is {@code void}, {@code null} is assigned.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Empty}: Assigns the parameter type's
 * default value, i.e. {@code null} for a reference type or zero for primitive types. This is an opportunity to
 * ignore a parameter.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Pipe}: A parameter that is annotated
 * with this annotation is assigned a proxy for forwarding the source method invocation to another instance of the
 * same type as the declaring type of the intercepted method. <b>This annotation needs to be installed and explicitly
 * registered before it can be used.</b> See the {@link net.bytebuddy.implementation.bind.annotation.Pipe}
 * annotation's documentation for further information on how this can be done.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Morph}: The morph annotation is similar to
 * the {@link net.bytebuddy.implementation.bind.annotation.SuperCall} annotation but allows to
 * explicitly define and therewith alter the arguments that are handed to the super method. <b>This annotation needs
 * to be installed and explicitly registered before it can be used.</b> See the documentation to the annotation for
 * further information.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.FieldValue}: Allows to access a field's value at the time
 * of the method invocation. The field's value is directly assigned to the annotated parameter.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.FieldProxy}: Allows to access fields via getter
 * and setter proxies. <b>This annotation needs to be installed and explicitly registered before it can be used.</b>
 * Note that any field access requires boxing such that a use of {@link net.bytebuddy.implementation.FieldAccessor} in
 * combination with {@link net.bytebuddy.implementation.MethodDelegation#andThen(Implementation)} might be a more
 * performant alternative for implementing field getters and setters.</li>
 * </ul>
 * If a method is not annotated with any of the above methods, it will be treated as if it was annotated
 * {@link net.bytebuddy.implementation.bind.annotation.Argument} using the next
 * unbound parameter index of the source method as its parameter. This means that a method
 * {@code Qux#baz(@Argument(2) Object p1, Object p2, @Argument(0) Object p3} would be treated as if {@code p2} was annotated
 * with {@code @Argument(1)}.
 * <p>&nbsp;</p>
 * In addition, the {@link net.bytebuddy.implementation.bind.annotation.RuntimeType}
 * annotation can instruct a parameter to be bound by a
 * {@link net.bytebuddy.implementation.bytecode.assign.Assigner} with considering the
 * runtime type of the parameter.
 * <p>&nbsp;</p>
 * <span style="text-decoration: underline">Selecting among different methods that can be used for binding a method
 * of the instrumented type</span>
 * <p>&nbsp;</p>
 * When deciding between two methods {@code Foo#bar} and {@code Foo#qux} that could both be used to delegating a
 * method call, the following consideration is applied in the given order:
 * <ol>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.BindingPriority}:
 * A method that is annotated with this annotation is given a specific priority where the default priority is set
 * to {@link net.bytebuddy.implementation.bind.annotation.BindingPriority#DEFAULT}
 * for non-annotated method. A method with a higher priority is considered a better target for delegation.</li>
 * <li>{@link net.bytebuddy.implementation.bind.DeclaringTypeResolver}:
 * If a target method is declared by a more specific type than another method, the method with the most specific
 * type is bound.</li>
 * <li>{@link net.bytebuddy.implementation.bind.MethodNameEqualityResolver}:
 * If a source method {@code Baz#qux} is the source method, it will rather be assigned to {@code Foo#qux} because
 * of their equal names. Similar names and case-insensitive equality are not considered.</li>
 * <li>{@link net.bytebuddy.implementation.bind.ArgumentTypeResolver}:
 * The most specific type resolver will consider all bindings that are using the
 * {@link net.bytebuddy.implementation.bind.annotation.Argument}
 * annotation for resolving a binding conflict. In this context, the resolution will equal the most-specific
 * type resolution that is performed by the Java compiler. This means that a source method {@code Bar#baz(String)}
 * will rather be bound to a method {@code Foo#bar(String)} than {@code Foo#qux(Object)} because the {@code String}
 * type is more specific than the {@code Object} type. If two methods are equally adequate by their parameter types,
 * then the method with the higher numbers of {@code @Argument} annotated parameters is considered as the better
 * delegation target.</li>
 * <li>{@link net.bytebuddy.implementation.bind.ParameterLengthResolver}:
 * If a target methods has a higher number of total parameters that were successfully bound, the method with
 * the higher number will be considered as the better delegation target.</li>
 * </ol>
 * <p>
 * Additionally, if a method is annotated by
 * {@link net.bytebuddy.implementation.bind.annotation.IgnoreForBinding},
 * it is never considered as a target for a method delegation.
 * </p>
 * <p>
 * <b>Important</b>: For invoking a method on another instance, use the {@link Forwarding} implementation. A method delegation
 * intends to bind a interceptor class and its resolution algorithm will not necessarily yield a delegation to the intercepted
 * method.
 * </p>
 *
 * @see Forwarding
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue
 */
public class MethodDelegation implements Implementation.Composable {

    /**
     * The implementation delegate for this method delegation.
     */
    private final ImplementationDelegate implementationDelegate;

    /**
     * A list of {@link net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder.ParameterBinder}s
     * to be used by this method delegation.
     */
    private final List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders;

    /**
     * The {@link net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder.DefaultsProvider}
     * to be used by this method delegation.
     */
    private final TargetMethodAnnotationDrivenBinder.DefaultsProvider defaultsProvider;

    /**
     * The termination handler to apply.
     */
    private final TargetMethodAnnotationDrivenBinder.TerminationHandler terminationHandler;

    /**
     * The {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}
     * to be used by this method delegation.
     */
    private final MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    /**
     * The {@link net.bytebuddy.implementation.bytecode.assign.Assigner} to be used by this method delegation.
     */
    private final Assigner assigner;

    /**
     * A method container to query for methods to be considered for delegation.
     */
    private final MethodContainer methodContainer;

    /**
     * Creates a new method delegation.
     *
     * @param implementationDelegate The implementation delegate to use by this method delegator.
     * @param parameterBinders       The parameter binders to use by this method delegator.
     * @param defaultsProvider       The defaults provider to use by this method delegator.
     * @param terminationHandler     The termination handler to apply.
     * @param ambiguityResolver      The ambiguity resolver to use by this method delegator.
     * @param assigner               The assigner to be supplied by this method delegator.
     * @param methodContainer        A method container to query for methods to be considered for delegation.
     */
    protected MethodDelegation(ImplementationDelegate implementationDelegate,
                               List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                               TargetMethodAnnotationDrivenBinder.DefaultsProvider defaultsProvider,
                               TargetMethodAnnotationDrivenBinder.TerminationHandler terminationHandler,
                               MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                               Assigner assigner,
                               MethodContainer methodContainer) {
        this.implementationDelegate = implementationDelegate;
        this.parameterBinders = parameterBinders;
        this.defaultsProvider = defaultsProvider;
        this.terminationHandler = terminationHandler;
        this.ambiguityResolver = ambiguityResolver;
        this.assigner = assigner;
        this.methodContainer = methodContainer;
    }

    /**
     * Creates an implementation where only {@code static} methods of the given type are considered as binding targets.
     *
     * @param type The type containing the {@code static} methods for binding.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Class<?> type) {
        return to(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Creates an implementation where only {@code static} methods of the given type are considered as binding targets.
     *
     * @param typeDescription The type containing the {@code static} methods for binding.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(TypeDescription typeDescription) {
        if (typeDescription.isArray()) {
            throw new IllegalArgumentException("Cannot delegate to array " + typeDescription);
        } else if (typeDescription.isPrimitive()) {
            throw new IllegalArgumentException("Cannot delegate to primitive " + typeDescription);
        }
        return new MethodDelegation(ImplementationDelegate.ForStaticMethod.INSTANCE,
                TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                Argument.NextUnboundAsDefaultsProvider.INSTANCE,
                TargetMethodAnnotationDrivenBinder.TerminationHandler.Returning.INSTANCE,
                MethodDelegationBinder.AmbiguityResolver.DEFAULT,
                Assigner.DEFAULT,
                MethodContainer.ForExplicitMethods.ofStatic(typeDescription));
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate A delegate instance which will be injected by a
     *                 {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                 then delegated to this instance.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object delegate) {
        return to(delegate, MethodGraph.Compiler.DEFAULT);
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate            A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object delegate, MethodGraph.Compiler methodGraphCompiler) {
        return to(delegate, delegate.getClass(), methodGraphCompiler);
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate  A delegate instance which will be injected by a
     *                  {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                  then delegated to this instance.
     * @param fieldName The name of the field for storing the delegate instance.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object delegate, String fieldName) {
        return to(delegate, fieldName, MethodGraph.Compiler.DEFAULT);
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate            A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param fieldName           The name of the field for storing the delegate instance.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object delegate, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        return to(delegate, delegate.getClass(), fieldName, methodGraphCompiler);
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate A delegate instance which will be injected by a
     *                 {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                 then delegated to this instance.
     * @param type     The type as which the delegate is treated for resolving its methods.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object delegate, Type type) {
        return to(delegate, type, MethodGraph.Compiler.DEFAULT);
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate            A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param type                The type as which the delegate is treated for resolving its methods.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object delegate, Type type, MethodGraph.Compiler methodGraphCompiler) {
        return to(delegate,
                type,
                String.format("%s$%d", ImplementationDelegate.ForStaticField.PREFIX, Math.abs(delegate.hashCode() % Integer.MAX_VALUE)),
                methodGraphCompiler);
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate  A delegate instance which will be injected by a
     *                  {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                  then delegated to this instance.
     * @param type      The type as which the delegate is treated for resolving its methods.
     * @param fieldName The name of the field for storing the delegate instance.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object delegate, Type type, String fieldName) {
        return to(delegate, type, fieldName, MethodGraph.Compiler.DEFAULT);
    }

    /**
     * Creates an implementation where only instance methods of the given object are considered as binding targets.
     * This method will never bind to constructors but will consider methods that are defined in super types. Note
     * that this includes methods that were defined by the {@link java.lang.Object} class. You can narrow this default
     * selection by explicitly selecting methods with calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     *
     * @param delegate            A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param type                The type as which the delegate is treated for resolving its methods.
     * @param fieldName           The name of the field for storing the delegate instance.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object delegate, Type type, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(type);
        if (!typeDescription.asErasure().isInstance(delegate)) {
            throw new IllegalArgumentException(delegate + " is not an instance of " + type);
        }
        return new MethodDelegation(new ImplementationDelegate.ForStaticField(delegate, fieldName),
                TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                Argument.NextUnboundAsDefaultsProvider.INSTANCE,
                TargetMethodAnnotationDrivenBinder.TerminationHandler.Returning.INSTANCE,
                MethodDelegationBinder.AmbiguityResolver.DEFAULT,
                Assigner.DEFAULT,
                new MethodContainer.ForVirtualMethods(methodGraphCompiler, typeDescription));
    }

    /**
     * Creates an implementation where method calls are delegated to an instance that is manually stored in a field
     * {@code fieldName} that is defined for the instrumented type. The field belongs to any instance of the instrumented
     * type and must be set manually by the user of the instrumented class. Note that this prevents interception of
     * method calls within the constructor of the instrumented class which will instead result in a
     * {@link java.lang.NullPointerException}. Note that this includes methods that were defined by the
     * {@link java.lang.Object} class. You can narrow this default selection by explicitly selecting methods with
     * calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
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
    public static MethodDelegation toInstanceField(Class<?> type, String fieldName) {
        return toInstanceField(new TypeDescription.ForLoadedType(type), fieldName);
    }

    /**
     * Creates an implementation where method calls are delegated to an instance that is manually stored in a field
     * {@code fieldName} that is defined for the instrumented type. The field belongs to any instance of the instrumented
     * type and must be set manually by the user of the instrumented class. Note that this prevents interception of
     * method calls within the constructor of the instrumented class which will instead result in a
     * {@link java.lang.NullPointerException}. Note that this includes methods that were defined by the
     * {@link java.lang.Object} class. You can narrow this default selection by explicitly selecting methods with
     * calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     * <p>&nbsp;</p>
     * The field is typically accessed by reflection or by defining an accessor on the instrumented type.
     *
     * @param typeDescription The type of the delegate and the field.
     * @param fieldName       The name of the field.
     * @return A method delegation that intercepts method calls by delegating to method calls on the given instance.
     */
    public static MethodDelegation toInstanceField(TypeDescription typeDescription, String fieldName) {
        return toInstanceField(typeDescription, fieldName, MethodGraph.Compiler.DEFAULT);
    }

    /**
     * Creates an implementation where method calls are delegated to an instance that is manually stored in a field
     * {@code fieldName} that is defined for the instrumented type. The field belongs to any instance of the instrumented
     * type and must be set manually by the user of the instrumented class. Note that this prevents interception of
     * method calls within the constructor of the instrumented class which will instead result in a
     * {@link java.lang.NullPointerException}. Note that this includes methods that were defined by the
     * {@link java.lang.Object} class. You can narrow this default selection by explicitly selecting methods with
     * calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to <code>Foo</code> where no methods of {@link java.lang.Object} are considered
     * for delegation.
     * <p>&nbsp;</p>
     * The field is typically accessed by reflection or by defining an accessor on the instrumented type.
     *
     * @param type                The type of the delegate and the field.
     * @param fieldName           The name of the field.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation that intercepts method calls by delegating to method calls on the given instance.
     */
    public static MethodDelegation toInstanceField(Class<?> type, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        return toInstanceField(new TypeDescription.ForLoadedType(type), fieldName, methodGraphCompiler);
    }

    /**
     * Creates an implementation where method calls are delegated to an instance that is manually stored in a field
     * {@code fieldName} that is defined for the instrumented type. The field belongs to any instance of the instrumented
     * type and must be set manually by the user of the instrumented class. Note that this prevents interception of
     * method calls within the constructor of the instrumented class which will instead result in a
     * {@link java.lang.NullPointerException}. Note that this includes methods that were defined by the
     * {@link java.lang.Object} class. You can narrow this default selection by explicitly selecting methods with
     * calling the
     * {@link net.bytebuddy.implementation.MethodDelegation#filter(net.bytebuddy.matcher.ElementMatcher)}
     * method on the returned method delegation as for example:
     * <pre>MethodDelegation.to(new Foo()).filter(MethodMatchers.not(isDeclaredBy(Object.class)));</pre>
     * which will result in a delegation to {@code Foo} where no methods of {@link java.lang.Object} are considered
     * for delegation.
     * <p>&nbsp;</p>
     * The field is typically accessed by reflection or by defining an accessor on the instrumented type.
     *
     * @param typeDefinition      The type of the delegate and the field.
     * @param fieldName           The name of the field.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation that intercepts method calls by delegating to method calls on the given instance.
     */
    public static MethodDelegation toInstanceField(TypeDefinition typeDefinition, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        return new MethodDelegation(new ImplementationDelegate.ForInstanceField(typeDefinition.asGenericType(), fieldName),
                TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                Argument.NextUnboundAsDefaultsProvider.INSTANCE,
                TargetMethodAnnotationDrivenBinder.TerminationHandler.Returning.INSTANCE,
                MethodDelegationBinder.AmbiguityResolver.DEFAULT,
                Assigner.DEFAULT,
                new MethodContainer.ForVirtualMethods(methodGraphCompiler, typeDefinition.asGenericType()));
    }

    /**
     * Creates an implementation where method calls are delegated to constructor calls on the given type. As a result,
     * the return values of all instrumented methods must be assignable to
     *
     * @param type The type that should be constructed by the instrumented methods.
     * @return An implementation that creates instances of the given type as its result.
     */
    public static MethodDelegation toConstructor(Class<?> type) {
        return toConstructor(new TypeDescription.ForLoadedType(type));
    }

    /**
     * Creates an implementation where method calls are delegated to constructor calls on the given type. As a result,
     * the return values of all instrumented methods must be assignable to
     *
     * @param typeDescription The type that should be constructed by the instrumented methods.
     * @return An implementation that creates instances of the given type as its result.
     */
    public static MethodDelegation toConstructor(TypeDescription typeDescription) {
        return new MethodDelegation(new ImplementationDelegate.ForConstruction(typeDescription),
                TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                Argument.NextUnboundAsDefaultsProvider.INSTANCE,
                TargetMethodAnnotationDrivenBinder.TerminationHandler.Returning.INSTANCE,
                MethodDelegationBinder.AmbiguityResolver.DEFAULT,
                Assigner.DEFAULT,
                MethodContainer.ForExplicitMethods.ofConstructors(typeDescription));
    }

    /**
     * Defines an parameter binder to be appended to the already defined parameter binders.
     *
     * @param parameterBinder The parameter binder to append to the already defined parameter binders.
     * @return A method delegation implementation that makes use of the given parameter binder.
     */
    public MethodDelegation appendParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder<?> parameterBinder) {
        return new MethodDelegation(implementationDelegate,
                CompoundList.of(parameterBinders, parameterBinder),
                defaultsProvider,
                terminationHandler,
                ambiguityResolver,
                assigner,
                methodContainer);
    }

    /**
     * Defines a number of parameter binders to be appended to be used by this method delegation.
     *
     * @param parameterBinder The parameter binders to use by this parameter binders.
     * @return A method delegation implementation that makes use of the given parameter binders.
     */
    public MethodDelegation defineParameterBinder(TargetMethodAnnotationDrivenBinder.ParameterBinder<?>... parameterBinder) {
        return new MethodDelegation(implementationDelegate,
                Arrays.asList(parameterBinder),
                defaultsProvider,
                terminationHandler,
                ambiguityResolver,
                assigner,
                methodContainer);
    }

    /**
     * A provider for annotation instances on values that are not explicitly annotated.
     *
     * @param defaultsProvider The defaults provider to use.
     * @return A method delegation implementation that makes use of the given defaults provider.
     */
    public MethodDelegation withDefaultsProvider(TargetMethodAnnotationDrivenBinder.DefaultsProvider defaultsProvider) {
        return new MethodDelegation(implementationDelegate,
                parameterBinders,
                defaultsProvider,
                terminationHandler,
                ambiguityResolver,
                assigner,
                methodContainer);
    }

    /**
     * Defines an ambiguity resolver to be appended to the already defined ambiguity resolver for resolving binding conflicts.
     *
     * @param ambiguityResolver The ambiguity resolver to append to the already defined ambiguity resolvers.
     * @return A method delegation implementation that makes use of the given ambiguity resolver.
     */
    public MethodDelegation appendAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver ambiguityResolver) {
        return defineAmbiguityResolver(new MethodDelegationBinder.AmbiguityResolver.Chain(this.ambiguityResolver, ambiguityResolver));
    }

    /**
     * Defines an ambiguity resolver to be used for resolving binding conflicts.
     *
     * @param ambiguityResolver The ambiguity resolver to use exclusively.
     * @return A method delegation implementation that makes use of the given ambiguity resolver.
     */
    public MethodDelegation defineAmbiguityResolver(MethodDelegationBinder.AmbiguityResolver... ambiguityResolver) {
        return new MethodDelegation(implementationDelegate,
                parameterBinders,
                defaultsProvider,
                terminationHandler,
                new MethodDelegationBinder.AmbiguityResolver.Chain(ambiguityResolver),
                assigner,
                methodContainer);
    }

    /**
     * Applies an assigner to the method delegation that is used for assigning method return and parameter types.
     *
     * @param assigner The assigner to apply.
     * @return A method delegation implementation that makes use of the given designer.
     */
    public MethodDelegation withAssigner(Assigner assigner) {
        return new MethodDelegation(implementationDelegate,
                parameterBinders,
                defaultsProvider,
                terminationHandler,
                ambiguityResolver,
                assigner,
                methodContainer);
    }

    /**
     * Applies a filter to target methods that are eligible for delegation.
     *
     * @param methodMatcher A filter where only methods that match the filter are considered for delegation.
     * @return A method delegation with the filter applied.
     */
    public MethodDelegation filter(ElementMatcher<? super MethodDescription> methodMatcher) {
        return new MethodDelegation(implementationDelegate,
                parameterBinders,
                defaultsProvider,
                terminationHandler,
                ambiguityResolver,
                assigner,
                methodContainer.filter(methodMatcher));
    }

    @Override
    public Implementation andThen(Implementation implementation) {
        return new Compound(new MethodDelegation(implementationDelegate,
                parameterBinders,
                defaultsProvider,
                TargetMethodAnnotationDrivenBinder.TerminationHandler.Dropping.INSTANCE,
                ambiguityResolver,
                assigner,
                methodContainer), implementation);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return implementationDelegate.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationDelegate.getPreparingStackAssignment(implementationTarget.getInstrumentedType()),
                implementationTarget,
                methodContainer.resolve(implementationTarget.getInstrumentedType()),
                new MethodDelegationBinder.Processor(new TargetMethodAnnotationDrivenBinder(
                        parameterBinders,
                        defaultsProvider,
                        terminationHandler,
                        assigner,
                        implementationDelegate.getMethodInvoker(implementationTarget.getInstrumentedType())
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
                && terminationHandler.equals(that.terminationHandler)
                && implementationDelegate.equals(that.implementationDelegate)
                && methodContainer.equals(that.methodContainer)
                && parameterBinders.equals(that.parameterBinders);
    }

    @Override
    public int hashCode() {
        int result = implementationDelegate.hashCode();
        result = 31 * result + parameterBinders.hashCode();
        result = 31 * result + defaultsProvider.hashCode();
        result = 31 * result + terminationHandler.hashCode();
        result = 31 * result + ambiguityResolver.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + methodContainer.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MethodDelegation{" +
                "implementationDelegate=" + implementationDelegate +
                ", parameterBinders=" + parameterBinders +
                ", defaultsProvider=" + defaultsProvider +
                ", terminationHandler=" + terminationHandler +
                ", ambiguityResolver=" + ambiguityResolver +
                ", assigner=" + assigner +
                ", methodContainer=" + methodContainer +
                '}';
    }

    /**
     * An implementation delegate is responsible for executing the actual method delegation.
     */
    protected interface ImplementationDelegate {

        /**
         * Prepares the instrumented type.
         *
         * @param instrumentedType The instrumented type to be prepared.
         * @return The instrumented type after it was prepared.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * Returns the stack manipulation responsible for preparing the instance representing the implementation.
         *
         * @param instrumentedType A description of the instrumented type to which the implementation is applied.
         * @return A stack manipulation representing the preparation.
         */
        StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType);

        /**
         * Returns the method invoker responsible for invoking the delegation method.
         *
         * @param instrumentedType The instrumented type to which the implementation is applied.
         * @return A method invoker responsible for invoking the delegation method.
         */
        MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType);

        /**
         * An implementation applied to a static method.
         */
        enum ForStaticMethod implements ImplementationDelegate {

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
                return StackManipulation.Trivial.INSTANCE;
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return MethodDelegationBinder.MethodInvoker.Simple.INSTANCE;
            }

            @Override
            public String toString() {
                return "MethodDelegation.ImplementationDelegate.ForStaticMethod." + name();
            }
        }

        /**
         * An implementation applied on a static field.
         */
        class ForStaticField implements ImplementationDelegate {

            /**
             * The name prefix for the {@code static} field that is containing the delegation target.
             */
            protected static final String PREFIX = "delegate";

            /**
             * The name of the field that is containing the delegation target.
             */
            private final String fieldName;

            /**
             * The delegation target.
             */
            private final Object delegate;

            /**
             * Creates a new implementation for delegating to an instance that is stored in a {@code static} field.
             *
             * @param delegate  The actual delegation target.
             * @param fieldName The name of the field for storing the delegate instance.
             */
            public ForStaticField(Object delegate, String fieldName) {
                this.delegate = delegate;
                this.fieldName = fieldName;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(fieldName,
                                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                                new TypeDescription.Generic.OfNonGenericType.ForLoadedType(delegate.getClass())))
                        .withInitializer(new LoadedTypeInitializer.ForStaticField(fieldName, delegate));
            }

            @Override
            public StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType) {
                return FieldAccess.forField(instrumentedType.getDeclaredFields().filter((named(fieldName))).getOnly()).getter();
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return new MethodDelegationBinder.MethodInvoker.Virtual(new TypeDescription.ForLoadedType(delegate.getClass()));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && delegate.equals(((ForStaticField) other).delegate)
                        && fieldName.equals(((ForStaticField) other).fieldName);
            }

            @Override
            public int hashCode() {
                return 31 * fieldName.hashCode() + delegate.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegation.ImplementationDelegate.ForStaticField{" +
                        "fieldName='" + fieldName + '\'' +
                        ", delegate=" + delegate +
                        '}';
            }
        }

        /**
         * An implementation applied on an instance field.
         */
        class ForInstanceField implements ImplementationDelegate {

            /**
             * The name of the instance field that is containing the target of the method delegation.
             */
            private final String fieldName;

            /**
             * The type of the method delegation target.
             */
            private final TypeDescription.Generic fieldType;

            /**
             * Creates a new instance field implementation delegate.
             *
             * @param fieldType A description of the type that is the target of the implementation and thus also the
             *                  field type.
             * @param fieldName The name of the field.
             */
            public ForInstanceField(TypeDescription.Generic fieldType, String fieldName) {
                this.fieldType = fieldType;
                this.fieldName = fieldName;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(new FieldDescription.Token(fieldName, Opcodes.ACC_PUBLIC, fieldType));
            }

            @Override
            public StackManipulation getPreparingStackAssignment(TypeDescription instrumentedType) {
                return new StackManipulation.Compound(MethodVariableAccess.REFERENCE.loadOffset(0),
                        FieldAccess.forField(instrumentedType.getDeclaredFields().filter((named(fieldName))).getOnly()).getter());
            }

            @Override
            public MethodDelegationBinder.MethodInvoker getMethodInvoker(TypeDescription instrumentedType) {
                return new MethodDelegationBinder.MethodInvoker.Virtual(fieldType.asErasure());
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
                return "MethodDelegation.ImplementationDelegate.ForInstanceField{" +
                        "fieldName='" + fieldName + '\'' +
                        ", fieldType=" + fieldType +
                        '}';
            }
        }

        /**
         * An implementation that creates new instances of a given type.
         */
        class ForConstruction implements ImplementationDelegate {

            /**
             * The type that is to be constructed.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a new constructor implementation.
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
                        TypeCreation.of(typeDescription),
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
                return "MethodDelegation.ImplementationDelegate.ForConstruction{" +
                        "typeDescription=" + typeDescription +
                        '}';
            }
        }
    }

    /**
     * A method container collects methods that are considered as a target for delegation.
     */
    protected interface MethodContainer {

        /**
         * Appends a filter that is applied to the methods that this container represents.
         *
         * @param matcher The matcher that is to be applied for filtering methods.
         * @return A method container with the supplied filter applied.
         */
        MethodContainer filter(ElementMatcher<? super MethodDescription> matcher);

        /**
         * Resolves this method container to extract a list of methods to be considered for interception.
         *
         * @param instrumentedType The instrumented type.
         * @return A list of methods to be considered as delegation target.
         */
        MethodList<?> resolve(TypeDescription instrumentedType);

        /**
         * A method container for an explicit list of methods.
         */
        class ForExplicitMethods implements MethodContainer {

            /**
             * The methods to be considered.
             */
            private final MethodList<?> methodList;

            /**
             * Creates a new explicit method container.
             *
             * @param methodList The methods to be considered.
             */
            protected ForExplicitMethods(MethodList<?> methodList) {
                this.methodList = methodList;
            }

            /**
             * Creates a container for all static methods of the given type description.
             *
             * @param typeDescription The type description of which all static methods should be considered.
             * @return An appropriate method container.
             */
            protected static MethodContainer ofStatic(TypeDescription typeDescription) {
                return new ForExplicitMethods(typeDescription.getDeclaredMethods().filter(isStatic()));
            }

            /**
             * Creates a container for all constructors of the given type description.
             *
             * @param typeDescription The type description of which all constructors should be considered.
             * @return An appropriate method container.
             */
            protected static MethodContainer ofConstructors(TypeDescription typeDescription) {
                return new ForExplicitMethods(typeDescription.getDeclaredMethods().filter(isConstructor()));
            }

            @Override
            public MethodContainer filter(ElementMatcher<? super MethodDescription> matcher) {
                return new ForExplicitMethods(methodList.filter(matcher));
            }

            @Override
            public MethodList<?> resolve(TypeDescription instrumentedType) {
                return methodList.filter(isVisibleTo(instrumentedType));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodList.equals(((ForExplicitMethods) other).methodList);
            }

            @Override
            public int hashCode() {
                return methodList.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegation.MethodContainer.ForExplicitMethods{" +
                        "methodList=" + methodList +
                        '}';
            }
        }

        /**
         * A method container for which all virtual methods of a given type should be considered.
         */
        class ForVirtualMethods implements MethodContainer {

            /**
             * The method graph compiler to use.
             */
            private final MethodGraph.Compiler methodGraphCompiler;

            /**
             * The target type for which the virtual methods should be extracted.
             */
            private final TypeDescription.Generic targetType;

            /**
             * A matcher representing a filter to be applied to the extracted methods.
             */
            private final ElementMatcher<? super MethodDescription> matcher;

            /**
             * Creates a new method container for virtual method extraction.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param targetType          The target type for which the virtual methods should be extracted.
             */
            protected ForVirtualMethods(MethodGraph.Compiler methodGraphCompiler, TypeDescription.Generic targetType) {
                this(methodGraphCompiler, targetType, any());
            }

            /**
             * Creates a new method container for virtual method extraction.
             *
             * @param methodGraphCompiler The method graph compiler to use.
             * @param targetType          The target type for which the virtual methods should be extracted.
             * @param matcher             A matcher representing a filter to be applied to the extracted methods.
             */
            private ForVirtualMethods(MethodGraph.Compiler methodGraphCompiler,
                                      TypeDescription.Generic targetType,
                                      ElementMatcher<? super MethodDescription> matcher) {
                this.methodGraphCompiler = methodGraphCompiler;
                this.targetType = targetType;
                this.matcher = matcher;
            }

            @Override
            public MethodContainer filter(ElementMatcher<? super MethodDescription> matcher) {
                return new ForVirtualMethods(methodGraphCompiler,
                        targetType,
                        new ElementMatcher.Junction.Conjunction<MethodDescription>(this.matcher, matcher));
            }

            @Override
            public MethodList<?> resolve(TypeDescription instrumentedType) {
                if (!targetType.asErasure().isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException(instrumentedType + " cannot access " + targetType);
                }
                return methodGraphCompiler.compile(targetType, instrumentedType).listNodes().asMethodList().filter(matcher);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ForVirtualMethods that = (ForVirtualMethods) other;
                return methodGraphCompiler.equals(that.methodGraphCompiler)
                        && targetType.equals(that.targetType)
                        && matcher.equals(that.matcher);
            }

            @Override
            public int hashCode() {
                int result = methodGraphCompiler.hashCode();
                result = 31 * result + targetType.hashCode();
                result = 31 * result + matcher.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodDelegation.MethodContainer.ForVirtualMethods{" +
                        "methodGraphCompiler=" + methodGraphCompiler +
                        ", targetType=" + targetType +
                        ", matcher=" + matcher +
                        '}';
            }
        }
    }

    /**
     * The appender for implementing a {@link net.bytebuddy.implementation.MethodDelegation}.
     */
    protected static class Appender implements ByteCodeAppender {

        /**
         * The stack manipulation that is responsible for loading a potential target instance onto the stack
         * on which the target method is invoked.
         */
        private final StackManipulation preparingStackAssignment;

        /**
         * The implementation target of this implementation.
         */
        private final Target implementationTarget;

        /**
         * The method candidates to consider for delegating the invocation to.
         */
        private final MethodList targetCandidates;

        /**
         * The method delegation binder processor which is responsible for implementing the method delegation.
         */
        private final MethodDelegationBinder.Processor processor;

        /**
         * Creates a new appender.
         *
         * @param preparingStackAssignment The stack manipulation that is responsible for loading a potential target
         *                                 instance onto the stack on which the target method is invoked.
         * @param implementationTarget     The implementation target of this implementation.
         * @param targetCandidates         The method candidates to consider for delegating the invocation to.
         * @param processor                The method delegation binder processor which is responsible for implementing
         *                                 the method delegation.
         */
        protected Appender(StackManipulation preparingStackAssignment,
                           Target implementationTarget,
                           MethodList targetCandidates,
                           MethodDelegationBinder.Processor processor) {
            this.preparingStackAssignment = preparingStackAssignment;
            this.implementationTarget = implementationTarget;
            this.targetCandidates = targetCandidates;
            this.processor = processor;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    preparingStackAssignment,
                    processor.process(implementationTarget, instrumentedMethod, targetCandidates)
            ).apply(methodVisitor, implementationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Appender that = (Appender) other;
            return implementationTarget.equals(that.implementationTarget)
                    && preparingStackAssignment.equals(that.preparingStackAssignment)
                    && processor.equals(that.processor)
                    && targetCandidates.equals(that.targetCandidates);
        }

        @Override
        public int hashCode() {
            int result = preparingStackAssignment.hashCode();
            result = 31 * result + implementationTarget.hashCode();
            result = 31 * result + targetCandidates.hashCode();
            result = 31 * result + processor.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodDelegation.Appender{" +
                    "preparingStackAssignment=" + preparingStackAssignment +
                    ", implementationTarget=" + implementationTarget +
                    ", targetCandidates=" + targetCandidates +
                    ", processor=" + processor +
                    '}';
        }
    }
}
