package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
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
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <b>Important</b>: For invoking a method on another instance, use the {@link MethodCall} implementation. A method delegation
 * intends to bind a interceptor class and its resolution algorithm will not necessarily yield a delegation to the intercepted
 * method.
 * </p>
 *
 * @see MethodCall
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
     * The {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}
     * to be used by this method delegation.
     */
    private final MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    /**
     * The termination handler to apply.
     */
    private final TargetMethodAnnotationDrivenBinder.TerminationHandler terminationHandler;

    /**
     * The {@link net.bytebuddy.implementation.bytecode.assign.Assigner} to be used by this method delegation.
     */
    private final Assigner assigner;

    /**
     * Creates a new method delegation.
     *
     * @param implementationDelegate The implementation delegate to use by this method delegator.
     * @param parameterBinders       The parameter binders to use by this method delegator.
     * @param ambiguityResolver      The ambiguity resolver to use by this method delegator.
     */
    protected MethodDelegation(ImplementationDelegate implementationDelegate,
                               List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                               MethodDelegationBinder.AmbiguityResolver ambiguityResolver) {
        this(implementationDelegate, parameterBinders, ambiguityResolver, MethodDelegationBinder.TerminationHandler.Default.RETURNING, Assigner.DEFAULT);
    }

    /**
     * Creates a new method delegation.
     *
     * @param implementationDelegate The implementation delegate to use by this method delegator.
     * @param parameterBinders       The parameter binders to use by this method delegator.
     * @param ambiguityResolver      The ambiguity resolver to use by this method delegator.
     * @param terminationHandler     The termination handler to apply.
     * @param assigner               The assigner to be supplied by this method delegator.
     */
    private MethodDelegation(ImplementationDelegate implementationDelegate,
                             List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                             MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                             TargetMethodAnnotationDrivenBinder.TerminationHandler terminationHandler,
                             Assigner assigner) {
        this.implementationDelegate = implementationDelegate;
        this.parameterBinders = parameterBinders;
        this.terminationHandler = terminationHandler;
        this.ambiguityResolver = ambiguityResolver;
        this.assigner = assigner;
    }

    /**
     * Creates an implementation where only {@code static} methods of the given type are considered as binding targets.
     *
     * @param type The type containing the {@code static} methods for binding.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Class<?> type) {
        return withDefaultConfiguration().to(type);
    }

    /**
     * Creates an implementation where only {@code static} methods of the given type are considered as binding targets.
     *
     * @param typeDescription The type containing the {@code static} methods for binding.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(TypeDescription typeDescription) {
        return withDefaultConfiguration().to(typeDescription);
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
     * @param target A delegate instance which will be injected by a
     *               {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *               then delegated to this instance.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object target) {
        return withDefaultConfiguration().to(target);
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
     * @param target              A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object target, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, methodGraphCompiler);
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
     * @param target    A delegate instance which will be injected by a
     *                  {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                  then delegated to this instance.
     * @param fieldName The name of the field for storing the delegate instance.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object target, String fieldName) {
        return withDefaultConfiguration().to(target, fieldName);
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
     * @param target              A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param fieldName           The name of the field for storing the delegate instance.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object target, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, fieldName, methodGraphCompiler);
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
     * @param target A delegate instance which will be injected by a
     *               {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *               then delegated to this instance.
     * @param type   The type as which the delegate is treated for resolving its methods.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object target, Type type) {
        return withDefaultConfiguration().to(target, type);
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
     * @param target              A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param type                The type as which the delegate is treated for resolving its methods.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given instance methods.
     */
    public static MethodDelegation to(Object target, Type type, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, type, methodGraphCompiler);
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
     * @param target    A delegate instance which will be injected by a
     *                  {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                  then delegated to this instance.
     * @param type      The type as which the delegate is treated for resolving its methods.
     * @param fieldName The name of the field for storing the delegate instance.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object target, Type type, String fieldName) {
        return withDefaultConfiguration().to(target, type, fieldName);
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
     * @param target              A delegate instance which will be injected by a
     *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
     *                            then delegated to this instance.
     * @param type                The type as which the delegate is treated for resolving its methods.
     * @param fieldName           The name of the field for storing the delegate instance.
     * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
     * @return A method delegation implementation to the given {@code static} methods.
     */
    public static MethodDelegation to(Object target, Type type, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, type, fieldName, methodGraphCompiler);
    }

    /**
     * Creates an implementation where method calls are delegated to constructor calls on the given type. As a result,
     * the return values of all instrumented methods must be assignable to
     *
     * @param type The type that should be constructed by the instrumented methods.
     * @return An implementation that creates instances of the given type as its result.
     */
    public static MethodDelegation toConstructor(Class<?> type) {
        return withDefaultConfiguration().toConstructor(type);
    }

    /**
     * Creates an implementation where method calls are delegated to constructor calls on the given type. As a result,
     * the return values of all instrumented methods must be assignable to
     *
     * @param typeDescription The type that should be constructed by the instrumented methods.
     * @return An implementation that creates instances of the given type as its result.
     */
    public static MethodDelegation toConstructor(TypeDescription typeDescription) {
        return withDefaultConfiguration().toConstructor(typeDescription);
    }

    /**
     * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
     * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
     *
     * @param name The name of the field.
     * @return A method delegation that invokes method's on a field's value.
     */
    public static MethodDelegation toField(String name) {
        return withDefaultConfiguration().toField(name);
    }

    /**
     * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
     * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
     *
     * @param name                The name of the field.
     * @param fieldLocatorFactory A field locator factory to use for locating the field.
     * @return A method delegation that invokes method's on a field's value.
     */
    public static MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory) {
        return withDefaultConfiguration().toField(name, fieldLocatorFactory);
    }

    /**
     * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
     * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
     *
     * @param name                The name of the field.
     * @param methodGraphCompiler The method graph compiler to use for locating methods of the field's value.
     * @return A method delegation that invokes method's on a field's value.
     */
    public static MethodDelegation toField(String name, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().toField(name, methodGraphCompiler);
    }

    /**
     * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
     * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
     *
     * @param name                The name of the field.
     * @param fieldLocatorFactory A field locator factory to use for locating the field.
     * @param methodGraphCompiler The method graph compiler to use for locating methods of the field's value.
     * @return A method delegation that invokes method's on a field's value.
     */
    public static MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().toField(name, fieldLocatorFactory, methodGraphCompiler);
    }

    public static WithCustomProperties withDefaultConfiguration() {
        return new WithCustomProperties(MethodDelegationBinder.AmbiguityResolver.DEFAULT,
                TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                any());
    }

    public static WithCustomProperties withEmptyConfiguration() {
        return new WithCustomProperties(MethodDelegationBinder.AmbiguityResolver.NoOp.INSTANCE,
                Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList(),
                any());
    }

    /**
     * Applies an assigner to the method delegation that is used for assigning method return and parameter types.
     *
     * @param assigner The assigner to apply.
     * @return A method delegation implementation that makes use of the given designer.
     */
    public Implementation.Composable withAssigner(Assigner assigner) {
        return new MethodDelegation(implementationDelegate,
                parameterBinders,
                ambiguityResolver,
                terminationHandler,
                assigner);
    }

    @Override
    public Implementation andThen(Implementation implementation) {
        return new Compound(new MethodDelegation(implementationDelegate,
                parameterBinders,
                ambiguityResolver,
                MethodDelegationBinder.TerminationHandler.Default.DROPPING,
                assigner), implementation);
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return implementationDelegate.prepare(instrumentedType);
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        ImplementationDelegate.Compiled compiled = implementationDelegate.compile(implementationTarget.getInstrumentedType());
        return new Appender(implementationTarget,
                new MethodDelegationBinder.Processor(compiled.getRecords(), ambiguityResolver),
                terminationHandler,
                assigner,
                compiled);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodDelegation that = (MethodDelegation) other;
        return ambiguityResolver.equals(that.ambiguityResolver)
                && assigner.equals(that.assigner)
                && terminationHandler.equals(that.terminationHandler)
                && implementationDelegate.equals(that.implementationDelegate)
                && parameterBinders.equals(that.parameterBinders);
    }

    @Override
    public int hashCode() {
        int result = implementationDelegate.hashCode();
        result = 31 * result + parameterBinders.hashCode();
        result = 31 * result + terminationHandler.hashCode();
        result = 31 * result + ambiguityResolver.hashCode();
        result = 31 * result + assigner.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MethodDelegation{" +
                "implementationDelegate=" + implementationDelegate +
                ", parameterBinders=" + parameterBinders +
                ", terminationHandler=" + terminationHandler +
                ", ambiguityResolver=" + ambiguityResolver +
                ", assigner=" + assigner +
                '}';
    }

    /**
     * An implementation delegate is responsible for executing the actual method delegation.
     */
    protected interface ImplementationDelegate extends InstrumentedType.Prepareable {

        /**
         * A name prefix for fields.
         */
        String FIELD_NAME_PREFIX = "delegate";

        Compiled compile(TypeDescription instrumentedType);

        interface Compiled {

            StackManipulation prepare(MethodDescription instrumentedMethod);

            MethodDelegationBinder.MethodInvoker invoke();

            List<MethodDelegationBinder.Record> getRecords();

            class ForStaticCall implements Compiled {

                private final List<MethodDelegationBinder.Record> records;

                protected ForStaticCall(List<MethodDelegationBinder.Record> records) {
                    this.records = records;
                }

                @Override
                public StackManipulation prepare(MethodDescription instrumentedMethod) {
                    return StackManipulation.Trivial.INSTANCE;
                }

                @Override
                public MethodDelegationBinder.MethodInvoker invoke() {
                    return MethodDelegationBinder.MethodInvoker.Simple.INSTANCE;
                }

                @Override
                public List<MethodDelegationBinder.Record> getRecords() {
                    return records;
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForStaticCall that = (ForStaticCall) object;
                    return records.equals(that.records);
                }

                @Override
                public int hashCode() {
                    return records.hashCode();
                }

                @Override
                public String toString() {
                    return "MethodDelegation.ImplementationDelegate.Compiled.ForStaticCall{" +
                            "records=" + records +
                            '}';
                }
            }

            class ForField implements Compiled {

                private final FieldDescription fieldDescription;

                private final List<MethodDelegationBinder.Record> records;

                protected ForField(FieldDescription fieldDescription, List<MethodDelegationBinder.Record> records) {
                    this.fieldDescription = fieldDescription;
                    this.records = records;
                }

                @Override
                public StackManipulation prepare(MethodDescription instrumentedMethod) {
                    if (instrumentedMethod.isStatic() && !fieldDescription.isStatic()) {
                        throw new IllegalStateException();
                    }
                    return new StackManipulation.Compound(fieldDescription.isStatic()
                            ? StackManipulation.Trivial.INSTANCE
                            : MethodVariableAccess.loadThis(), FieldAccess.forField(fieldDescription).read());
                }

                @Override
                public MethodDelegationBinder.MethodInvoker invoke() {
                    return new MethodDelegationBinder.MethodInvoker.Virtual(fieldDescription.getType().asErasure());
                }

                @Override
                public List<MethodDelegationBinder.Record> getRecords() {
                    return records;
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForField forField = (ForField) object;
                    return fieldDescription.equals(forField.fieldDescription) && records.equals(forField.records);
                }

                @Override
                public int hashCode() {
                    int result = fieldDescription.hashCode();
                    result = 31 * result + records.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodDelegation.ImplementationDelegate.Compiled.ForField{" +
                            "fieldDescription=" + fieldDescription +
                            ", records=" + records +
                            '}';
                }
            }

            class ForConstruction implements Compiled {

                private final TypeDescription typeDescription;

                private final List<MethodDelegationBinder.Record> records;

                protected ForConstruction(TypeDescription typeDescription, List<MethodDelegationBinder.Record> records) {
                    this.typeDescription = typeDescription;
                    this.records = records;
                }

                @Override
                public StackManipulation prepare(MethodDescription instrumentedMethod) {
                    return new StackManipulation.Compound(TypeCreation.of(typeDescription), Duplication.SINGLE);
                }

                @Override
                public MethodDelegationBinder.MethodInvoker invoke() {
                    return MethodDelegationBinder.MethodInvoker.Simple.INSTANCE;
                }

                @Override
                public List<MethodDelegationBinder.Record> getRecords() {
                    return records;
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForConstruction that = (ForConstruction) object;
                    return typeDescription.equals(that.typeDescription) && records.equals(that.records);
                }

                @Override
                public int hashCode() {
                    int result = typeDescription.hashCode();
                    result = 31 * result + records.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodDelegation.ImplementationDelegate.Compiled.ForConstruction{" +
                            "typeDescription=" + typeDescription +
                            ", records=" + records +
                            '}';
                }
            }
        }

        class ForStaticMethod implements ImplementationDelegate {

            private final List<MethodDelegationBinder.Record> records;

            protected ForStaticMethod(List<MethodDelegationBinder.Record> records) {
                this.records = records;
            }

            protected static ImplementationDelegate of(MethodList<?> methods, MethodDelegationBinder methodDelegationBinder) {
                List<MethodDelegationBinder.Record> records = new ArrayList<MethodDelegationBinder.Record>(methods.size());
                for (MethodDescription methodDescription : methods) {
                    records.add(methodDelegationBinder.compile(methodDescription));
                }
                return new ForStaticMethod(records);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public ImplementationDelegate.Compiled compile(TypeDescription instrumentedType) {
                return new Compiled.ForStaticCall(records);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForStaticMethod that = (ForStaticMethod) object;
                return records.equals(that.records);
            }

            @Override
            public int hashCode() {
                return records.hashCode();
            }

            @Override
            public String toString() {
                return "MethodDelegation.ImplementationDelegate.ForStaticMethod{" +
                        "records=" + records +
                        '}';
            }
        }

        class ForField implements ImplementationDelegate {

            private final String fieldName;

            private final FieldLocator.Factory fieldLocatorFactory;

            private final MethodGraph.Compiler methodGraphCompiler;

            private final List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders;

            private final ElementMatcher<? super MethodDescription> matcher;

            protected ForField(String fieldName,
                               FieldLocator.Factory fieldLocatorFactory,
                               MethodGraph.Compiler methodGraphCompiler,
                               List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                               ElementMatcher<? super MethodDescription> matcher) {
                this.fieldName = fieldName;
                this.fieldLocatorFactory = fieldLocatorFactory;
                this.methodGraphCompiler = methodGraphCompiler;
                this.parameterBinders = parameterBinders;
                this.matcher = matcher;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public Compiled compile(TypeDescription instrumentedType) {
                FieldLocator.Resolution resolution = fieldLocatorFactory.make(instrumentedType).locate(fieldName);
                if (!resolution.isResolved()) {
                    throw new IllegalStateException();
                } else if (!resolution.getField().getType().asErasure().isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException();
                } else {
                    MethodList<?> candidates = methodGraphCompiler.compile(resolution.getField().getType(), instrumentedType)
                            .listNodes()
                            .asMethodList()
                            .filter(matcher);
                    List<MethodDelegationBinder.Record> records = new ArrayList<MethodDelegationBinder.Record>(candidates.size());
                    MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(parameterBinders);
                    for (MethodDescription candidate : candidates) {
                        records.add(methodDelegationBinder.compile(candidate));
                    }
                    return new Compiled.ForField(resolution.getField(), records);
                }
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForField forField = (ForField) object;
                return fieldName.equals(forField.fieldName)
                        && fieldLocatorFactory.equals(forField.fieldLocatorFactory)
                        && methodGraphCompiler.equals(forField.methodGraphCompiler)
                        && parameterBinders.equals(forField.parameterBinders)
                        && matcher.equals(forField.matcher);
            }

            @Override
            public int hashCode() {
                int result = fieldName.hashCode();
                result = 31 * result + fieldLocatorFactory.hashCode();
                result = 31 * result + methodGraphCompiler.hashCode();
                result = 31 * result + parameterBinders.hashCode();
                result = 31 * result + matcher.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodDelegation.ImplementationDelegate.ForField{" +
                        "fieldName='" + fieldName + '\'' +
                        ", fieldLocatorFactory=" + fieldLocatorFactory +
                        ", methodGraphCompiler=" + methodGraphCompiler +
                        ", parameterBinders=" + parameterBinders +
                        ", matcher=" + matcher +
                        '}';
            }
        }

        class ForInstance implements ImplementationDelegate {

            private final Object target;

            private final String fieldName;

            private final TypeDescription.Generic fieldType;

            private final MethodGraph.Compiler methodGraphCompiler;

            private final List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders;

            private final ElementMatcher<? super MethodDescription> matcher;

            protected ForInstance(Object target,
                                  String fieldName,
                                  TypeDescription.Generic fieldType,
                                  MethodGraph.Compiler methodGraphCompiler,
                                  List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                                  ElementMatcher<? super MethodDescription> matcher) {
                this.target = target;
                this.fieldName = fieldName;
                this.fieldType = fieldType;
                this.methodGraphCompiler = methodGraphCompiler;
                this.parameterBinders = parameterBinders;
                this.matcher = matcher;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType
                        .withField(new FieldDescription.Token(fieldName, Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, fieldType))
                        .withInitializer(new LoadedTypeInitializer.ForStaticField(fieldName, target));
            }

            @Override
            public Compiled compile(TypeDescription instrumentedType) {
                if (!fieldType.asErasure().isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException();
                } else {
                    FieldDescription fieldDescription = instrumentedType.getDeclaredFields()
                            .filter(named(fieldName).and(fieldType(fieldType.asErasure())))
                            .getOnly();
                    MethodList<?> candidates = methodGraphCompiler.compile(fieldDescription.getType(), instrumentedType)
                            .listNodes()
                            .asMethodList()
                            .filter(matcher);
                    List<MethodDelegationBinder.Record> records = new ArrayList<MethodDelegationBinder.Record>(candidates.size());
                    MethodDelegationBinder methodDelegationBinder = TargetMethodAnnotationDrivenBinder.of(parameterBinders);
                    for (MethodDescription candidate : candidates) {
                        records.add(methodDelegationBinder.compile(candidate));
                    }
                    return new Compiled.ForField(fieldDescription, records);
                }
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForInstance that = (ForInstance) object;
                return target.equals(that.target)
                        && fieldName.equals(that.fieldName)
                        && fieldType.equals(that.fieldType)
                        && methodGraphCompiler.equals(that.methodGraphCompiler)
                        && parameterBinders.equals(that.parameterBinders)
                        && matcher.equals(that.matcher);
            }

            @Override
            public int hashCode() {
                int result = target.hashCode();
                result = 31 * result + fieldName.hashCode();
                result = 31 * result + fieldType.hashCode();
                result = 31 * result + methodGraphCompiler.hashCode();
                result = 31 * result + parameterBinders.hashCode();
                result = 31 * result + matcher.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodDelegation.ImplementationDelegate.ForInstance{" +
                        "target=" + target +
                        ", fieldName='" + fieldName + '\'' +
                        ", fieldType=" + fieldType +
                        ", methodGraphCompiler=" + methodGraphCompiler +
                        ", parameterBinders=" + parameterBinders +
                        ", matcher=" + matcher +
                        '}';
            }
        }

        class ForConstruction implements ImplementationDelegate {

            private final TypeDescription typeDescription;

            private final List<MethodDelegationBinder.Record> records;

            protected ForConstruction(TypeDescription typeDescription, List<MethodDelegationBinder.Record> records) {
                this.typeDescription = typeDescription;
                this.records = records;
            }

            protected static ImplementationDelegate of(TypeDescription typeDescription,
                                                       MethodList<?> methods,
                                                       MethodDelegationBinder methodDelegationBinder) {
                List<MethodDelegationBinder.Record> records = new ArrayList<MethodDelegationBinder.Record>(methods.size());
                for (MethodDescription methodDescription : methods) {
                    records.add(methodDelegationBinder.compile(methodDescription));
                }
                return new ForConstruction(typeDescription, records);
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public Compiled compile(TypeDescription instrumentedType) {
                return new Compiled.ForConstruction(typeDescription, records);
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForConstruction that = (ForConstruction) object;
                return typeDescription.equals(that.typeDescription) && records.equals(that.records);
            }

            @Override
            public int hashCode() {
                int result = typeDescription.hashCode();
                result = 31 * result + records.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodDelegation.ImplementationDelegate.ForConstruction{" +
                        "typeDescription=" + typeDescription +
                        ", records=" + records +
                        '}';
            }
        }
    }

    /**
     * The appender for implementing a {@link net.bytebuddy.implementation.MethodDelegation}.
     */
    protected static class Appender implements ByteCodeAppender {

        /**
         * The implementation target of this implementation.
         */
        private final Target implementationTarget;

        /**
         * The method delegation binder processor which is responsible for implementing the method delegation.
         */
        private final MethodDelegationBinder.Processor processor;

        /**
         * A termination handler for a method delegation binder.
         */
        private final MethodDelegationBinder.TerminationHandler terminationHandler;

        /**
         * The assigner to use.
         */
        private final Assigner assigner;

        private final ImplementationDelegate.Compiled compiled;

        protected Appender(Target implementationTarget,
                           MethodDelegationBinder.Processor processor,
                           MethodDelegationBinder.TerminationHandler terminationHandler,
                           Assigner assigner,
                           ImplementationDelegate.Compiled compiled) {
            this.implementationTarget = implementationTarget;
            this.processor = processor;
            this.terminationHandler = terminationHandler;
            this.assigner = assigner;
            this.compiled = compiled;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            StackManipulation.Size stackSize = new StackManipulation.Compound(
                    compiled.prepare(instrumentedMethod),
                    processor.bind(implementationTarget, instrumentedMethod, terminationHandler, compiled.invoke(), assigner)
            ).apply(methodVisitor, implementationContext);
            return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Appender appender = (Appender) object;
            return implementationTarget.equals(appender.implementationTarget)
                    && processor.equals(appender.processor)
                    && terminationHandler.equals(appender.terminationHandler)
                    && assigner.equals(appender.assigner)
                    && compiled.equals(appender.compiled);
        }

        @Override
        public int hashCode() {
            int result = implementationTarget.hashCode();
            result = 31 * result + processor.hashCode();
            result = 31 * result + terminationHandler.hashCode();
            result = 31 * result + assigner.hashCode();
            result = 31 * result + compiled.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodDelegation.Appender{" +
                    "implementationTarget=" + implementationTarget +
                    ", processor=" + processor +
                    ", terminationHandler=" + terminationHandler +
                    ", assigner=" + assigner +
                    ", compiled=" + compiled +
                    '}';
        }
    }

    /**
     * A {@link MethodDelegation} with custom configuration.
     */
    public static class WithCustomProperties {

        /**
         * The ambiguity resolver to use.
         */
        private final MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

        /**
         * The parameter binders to use.
         */
        private final List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders;

        /**
         * The matcher to use for filtering relevant methods.
         */
        private final ElementMatcher<? super MethodDescription> matcher;

        /**
         * Creates a new method delegation with custom properties.
         *
         * @param ambiguityResolver The ambiguity resolver to use.
         * @param parameterBinders  The parameter binders to use.
         * @param matcher           The matcher to use for filtering relevant methods.
         */
        protected WithCustomProperties(MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                                       List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                                       ElementMatcher<? super MethodDescription> matcher) {
            this.ambiguityResolver = ambiguityResolver;
            this.parameterBinders = parameterBinders;
            this.matcher = matcher;
        }

        public WithCustomProperties withResolvers(MethodDelegationBinder.AmbiguityResolver... ambiguityResolver) {
            return withResolvers(Arrays.asList(ambiguityResolver));
        }

        public WithCustomProperties withResolvers(List<? extends MethodDelegationBinder.AmbiguityResolver> ambiguityResolvers) {
            return new WithCustomProperties(new MethodDelegationBinder.AmbiguityResolver.Compound(CompoundList.of(this.ambiguityResolver, ambiguityResolvers)), parameterBinders, matcher);
        }

        public WithCustomProperties withBinders(TargetMethodAnnotationDrivenBinder.ParameterBinder<?>... parameterBinder) {
            return withBinders(Arrays.asList(parameterBinder));
        }

        public WithCustomProperties withBinders(List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinder) {
            return new WithCustomProperties(ambiguityResolver, CompoundList.of(this.parameterBinders, parameterBinder), matcher);
        }

        public WithCustomProperties filter(ElementMatcher<? super MethodDescription> matcher) {
            return new WithCustomProperties(ambiguityResolver, parameterBinders, new ElementMatcher.Junction.Conjunction<MethodDescription>(this.matcher, matcher));
        }

        /**
         * Creates an implementation where only {@code static} methods of the given type are considered as binding targets.
         *
         * @param type The type containing the {@code static} methods for binding.
         * @return A method delegation implementation to the given {@code static} methods.
         */
        public MethodDelegation to(Class<?> type) {
            return to(new TypeDescription.ForLoadedType(type));
        }

        /**
         * Creates an implementation where only {@code static} methods of the given type are considered as binding targets.
         *
         * @param typeDescription The type containing the {@code static} methods for binding.
         * @return A method delegation implementation to the given {@code static} methods.
         */
        public MethodDelegation to(TypeDescription typeDescription) {
            if (typeDescription.isArray()) {
                throw new IllegalArgumentException("Cannot delegate to array " + typeDescription);
            } else if (typeDescription.isPrimitive()) {
                throw new IllegalArgumentException("Cannot delegate to primitive " + typeDescription);
            }
            return new MethodDelegation(ImplementationDelegate.ForStaticMethod.of(typeDescription.getDeclaredMethods().filter(isStatic().and(matcher)), TargetMethodAnnotationDrivenBinder.of(parameterBinders)),
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                    MethodDelegationBinder.AmbiguityResolver.DEFAULT);
        }

        /**
         * Creates an implementation where only instance methods of the given object are considered as binding targets.
         * This method will never bind to constructors but will consider methods that are defined in super types. Note
         * that this includes methods that were defined by the {@link java.lang.Object} class.
         *
         * @param target A delegate instance which will be injected by a
         *               {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *               then delegated to this instance.
         * @return A method delegation implementation to the given instance methods.
         */
        public MethodDelegation to(Object target) {
            return to(target, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Creates an implementation where only instance methods of the given object are considered as binding targets.
         * This method will never bind to constructors but will consider methods that are defined in super types. Note
         * that this includes methods that were defined by the {@link java.lang.Object} class.
         *
         * @param target              A delegate instance which will be injected by a
         *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *                            then delegated to this instance.
         * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
         * @return A method delegation implementation to the given instance methods.
         */
        public MethodDelegation to(Object target, MethodGraph.Compiler methodGraphCompiler) {
            return to(target, target.getClass(), methodGraphCompiler);
        }

        /**
         * Creates an implementation where only instance methods of the given object are considered as binding targets.
         * This method will never bind to constructors but will consider methods that are defined in super types. Note
         * that this includes methods that were defined by the {@link java.lang.Object} class.
         *
         * @param target    A delegate instance which will be injected by a
         *                  {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *                  then delegated to this instance.
         * @param fieldName The name of the field for storing the delegate instance.
         * @return A method delegation implementation to the given {@code static} methods.
         */
        public MethodDelegation to(Object target, String fieldName) {
            return to(target, fieldName, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Creates an implementation where only instance methods of the given object are considered as binding targets.
         * This method will never bind to constructors but will consider methods that are defined in super types. Note
         * that this includes methods that were defined by the {@link java.lang.Object} class.
         *
         * @param target              A delegate instance which will be injected by a
         *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *                            then delegated to this instance.
         * @param fieldName           The name of the field for storing the delegate instance.
         * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
         * @return A method delegation implementation to the given {@code static} methods.
         */
        public MethodDelegation to(Object target, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
            return to(target, target.getClass(), fieldName, methodGraphCompiler);
        }

        /**
         * Creates an implementation where only instance methods of the given object are considered as binding targets.
         * This method will never bind to constructors but will consider methods that are defined in super types. Note
         * that this includes methods that were defined by the {@link java.lang.Object} class unless the specified type is
         * an interface type where those methods are not considered.
         *
         * @param target A delegate instance which will be injected by a
         *               {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *               then delegated to this instance.
         * @param type   The type as which the delegate is treated for resolving its methods.
         * @return A method delegation implementation to the given instance methods.
         */
        public MethodDelegation to(Object target, Type type) {
            return to(target, type, MethodGraph.Compiler.DEFAULT);
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
         * @param target              A delegate instance which will be injected by a
         *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *                            then delegated to this instance.
         * @param type                The type as which the delegate is treated for resolving its methods.
         * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
         * @return A method delegation implementation to the given instance methods.
         */
        public MethodDelegation to(Object target, Type type, MethodGraph.Compiler methodGraphCompiler) {
            return to(target,
                    type,
                    String.format("%s$%s", ImplementationDelegate.FIELD_NAME_PREFIX, RandomString.hashOf(target.hashCode())),
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
         * @param target    A delegate instance which will be injected by a
         *                  {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *                  then delegated to this instance.
         * @param type      The type as which the delegate is treated for resolving its methods.
         * @param fieldName The name of the field for storing the delegate instance.
         * @return A method delegation implementation to the given {@code static} methods.
         */
        public MethodDelegation to(Object target, Type type, String fieldName) {
            return to(target, type, fieldName, MethodGraph.Compiler.DEFAULT);
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
         * @param target              A delegate instance which will be injected by a
         *                            {@link net.bytebuddy.implementation.LoadedTypeInitializer}. All intercepted method calls are
         *                            then delegated to this instance.
         * @param type                The type as which the delegate is treated for resolving its methods.
         * @param fieldName           The name of the field for storing the delegate instance.
         * @param methodGraphCompiler The method graph compiler to be used for locating methods to delegate to.
         * @return A method delegation implementation to the given {@code static} methods.
         */
        public MethodDelegation to(Object target, Type type, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
            TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(type);
            if (!typeDescription.asErasure().isInstance(target)) {
                throw new IllegalArgumentException(target + " is not an instance of " + type);
            }
            return new MethodDelegation(new ImplementationDelegate.ForInstance(target, fieldName, typeDescription, methodGraphCompiler, parameterBinders, matcher),
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                    MethodDelegationBinder.AmbiguityResolver.DEFAULT);
        }

        /**
         * Creates an implementation where method calls are delegated to constructor calls on the given type. As a result,
         * the return values of all instrumented methods must be assignable to
         *
         * @param type The type that should be constructed by the instrumented methods.
         * @return An implementation that creates instances of the given type as its result.
         */
        public MethodDelegation toConstructor(Class<?> type) {
            return toConstructor(new TypeDescription.ForLoadedType(type));
        }

        /**
         * Creates an implementation where method calls are delegated to constructor calls on the given type. As a result,
         * the return values of all instrumented methods must be assignable to
         *
         * @param typeDescription The type that should be constructed by the instrumented methods.
         * @return An implementation that creates instances of the given type as its result.
         */
        public MethodDelegation toConstructor(TypeDescription typeDescription) {
            return new MethodDelegation(ImplementationDelegate.ForConstruction.of(typeDescription, typeDescription.getDeclaredMethods().filter(isConstructor().and(matcher)), TargetMethodAnnotationDrivenBinder.of(parameterBinders)),
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                    MethodDelegationBinder.AmbiguityResolver.DEFAULT);
        }

        /**
         * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
         * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
         *
         * @param name The name of the field.
         * @return A method delegation that invokes method's on a field's value.
         */
        public MethodDelegation toField(String name) {
            return toField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
        }

        /**
         * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
         * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
         *
         * @param name                The name of the field.
         * @param fieldLocatorFactory A field locator factory to use for locating the field.
         * @return A method delegation that invokes method's on a field's value.
         */
        public MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory) {
            return toField(name, fieldLocatorFactory, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
         * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
         *
         * @param name                The name of the field.
         * @param methodGraphCompiler The method graph compiler to use for locating methods of the field's value.
         * @return A method delegation that invokes method's on a field's value.
         */
        public MethodDelegation toField(String name, MethodGraph.Compiler methodGraphCompiler) {
            return toField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE, methodGraphCompiler);
        }

        /**
         * Delegates invocations of any instrumented method to a field's value. The field must be defined in the instrumented type or in any super type.
         * If the field is not static, the instrumentation cannot be used for instrumenting static methods.
         *
         * @param name                The name of the field.
         * @param fieldLocatorFactory A field locator factory to use for locating the field.
         * @param methodGraphCompiler The method graph compiler to use for locating methods of the field's value.
         * @return A method delegation that invokes method's on a field's value.
         */
        public MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory, MethodGraph.Compiler methodGraphCompiler) {
            return new MethodDelegation(new ImplementationDelegate.ForField(name, fieldLocatorFactory, methodGraphCompiler, parameterBinders, matcher),
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                    MethodDelegationBinder.AmbiguityResolver.DEFAULT);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            WithCustomProperties that = (WithCustomProperties) object;
            return ambiguityResolver.equals(that.ambiguityResolver)
                    && parameterBinders.equals(that.parameterBinders)
                    && matcher.equals(that.matcher);
        }

        @Override
        public int hashCode() {
            int result = ambiguityResolver.hashCode();
            result = 31 * result + parameterBinders.hashCode();
            result = 31 * result + matcher.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MethodDelegation.WithCustomProperties{" +
                    "ambiguityResolver=" + ambiguityResolver +
                    ", parameterBinders=" + parameterBinders +
                    ", matcher=" + matcher +
                    '}';
        }
    }
}
