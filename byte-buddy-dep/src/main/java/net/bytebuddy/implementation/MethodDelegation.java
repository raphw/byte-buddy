package net.bytebuddy.implementation;

import lombok.EqualsAndHashCode;
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
 * <li>The {@link net.bytebuddy.implementation.bind.annotation.SuperMethod} or
 * {@link net.bytebuddy.implementation.bind.annotation.DefaultMethod} annotations can be used on any parameter type
 * that is assignable from the {@link java.lang.reflect.Method} type. the parameter is bound a method instance that
 * allows for the reflective invocation of a super or default method. Note that this method is not equal to the intercepted
 * method but represents a synthetic accessor method. Using this annotation also causes this accessor to be {@code public}
 * which allows its outside invocation without any access checks by a security manager.</li>
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
@EqualsAndHashCode
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
     * Delegates any intercepted method to invoke a {@code static} method that is declared by the supplied type. To be considered
     * a valid delegation target, the target method must be visible and accessible to the instrumented type. This is the case if
     * the target type is either public or in the same package as the instrumented type and if the target method is either public
     * or non-private and in the same package as the instrumented type. Private methods can only be used as a delegation target if
     * the interception is targeting the instrumented type.
     *
     * @param type The target type for the delegation.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Class<?> type) {
        return withDefaultConfiguration().to(type);
    }

    /**
     * Delegates any intercepted method to invoke a {@code static} method that is declared by the supplied type. To be considered
     * a valid delegation target, the target method must be visible and accessible to the instrumented type. This is the case if
     * the target type is either public or in the same package as the instrumented type and if the target method is either public
     * or non-private and in the same package as the instrumented type. Private methods can only be used as a delegation target if
     * the interception is targeting the instrumented type.
     *
     * @param typeDescription The target type for the delegation.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(TypeDescription typeDescription) {
        return withDefaultConfiguration().to(typeDescription);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target The target instance for the delegation.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target) {
        return withDefaultConfiguration().to(target);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target              The target instance for the delegation.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, methodGraphCompiler);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target    The target instance for the delegation.
     * @param fieldName The name of the field that is holding the {@code target} instance.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target, String fieldName) {
        return withDefaultConfiguration().to(target, fieldName);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target              The target instance for the delegation.
     * @param fieldName           The name of the field that is holding the {@code target} instance.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, fieldName, methodGraphCompiler);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target The target instance for the delegation.
     * @param type   The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target, Type type) {
        return withDefaultConfiguration().to(target, type);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target              The target instance for the delegation.
     * @param type                The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target, Type type, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, type, methodGraphCompiler);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target    The target instance for the delegation.
     * @param type      The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
     * @param fieldName The name of the field that is holding the {@code target} instance.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target, Type type, String fieldName) {
        return withDefaultConfiguration().to(target, type, fieldName);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
     * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
     * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the delegation is targeting the instrumented type.
     *
     * @param target              The target instance for the delegation.
     * @param type                The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
     * @param fieldName           The name of the field that is holding the {@code target} instance.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A method delegation that redirects method calls to a static method of the supplied type.
     */
    public static MethodDelegation to(Object target, Type type, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().to(target, type, fieldName, methodGraphCompiler);
    }

    /**
     * Delegates any intercepted method to invoke a constructor of the supplied type. To be considered a valid delegation target,
     * a constructor must be visible and accessible to the instrumented type. This is the case if the constructor's declaring type is
     * either public or in the same package as the instrumented type and if the constructor is either public or non-private and in
     * the same package as the instrumented type. Private constructors can only be used as a delegation target if the delegation is
     * targeting the instrumented type.
     *
     * @param type The type to construct.
     * @return A delegation that redirects method calls to a constructor of the supplied type.
     */
    public static MethodDelegation toConstructor(Class<?> type) {
        return withDefaultConfiguration().toConstructor(type);
    }

    /**
     * Delegates any intercepted method to invoke a constructor of the supplied type. To be considered a valid delegation target,
     * a constructor must be visible and accessible to the instrumented type. This is the case if the constructor's declaring type is
     * either public or in the same package as the instrumented type and if the constructor is either public or non-private and in
     * the same package as the instrumented type. Private constructors can only be used as a delegation target if the delegation is
     * targeting the instrumented type.
     *
     * @param typeDescription The type to construct.
     * @return A delegation that redirects method calls to a constructor of the supplied type.
     */
    public static MethodDelegation toConstructor(TypeDescription typeDescription) {
        return withDefaultConfiguration().toConstructor(typeDescription);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
     * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
     * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the ddelegation is targeting the instrumented type.
     *
     * @param name The field's name.
     * @return A delegation that redirects invocations to a method of the specified field's instance.
     */
    public static MethodDelegation toField(String name) {
        return withDefaultConfiguration().toField(name);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
     * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
     * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the ddelegation is targeting the instrumented type.
     *
     * @param name                The field's name.
     * @param fieldLocatorFactory The field locator factory to use.
     * @return A delegation that redirects invocations to a method of the specified field's instance.
     */
    public static MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory) {
        return withDefaultConfiguration().toField(name, fieldLocatorFactory);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
     * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
     * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the ddelegation is targeting the instrumented type.
     *
     * @param name                The field's name.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A delegation that redirects invocations to a method of the specified field's instance.
     */
    public static MethodDelegation toField(String name, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().toField(name, methodGraphCompiler);
    }

    /**
     * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
     * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
     * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
     * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
     * a delegation target if the ddelegation is targeting the instrumented type.
     *
     * @param name                The field's name.
     * @param fieldLocatorFactory The field locator factory to use.
     * @param methodGraphCompiler The method graph compiler to use.
     * @return A delegation that redirects invocations to a method of the specified field's instance.
     */
    public static MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory, MethodGraph.Compiler methodGraphCompiler) {
        return withDefaultConfiguration().toField(name, fieldLocatorFactory, methodGraphCompiler);
    }

    /**
     * Creates a configuration builder for a method delegation that is pre-configured with the ambiguity resolvers defined by
     * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver#DEFAULT} and the parameter binders
     * defined by {@link net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder.ParameterBinder#DEFAULTS}.
     *
     * @return A method delegation configuration with pre-configuration.
     */
    public static WithCustomProperties withDefaultConfiguration() {
        return new WithCustomProperties(MethodDelegationBinder.AmbiguityResolver.DEFAULT, TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS);
    }

    /**
     * Creates a configuration builder for a method delegation that does not apply any pre-configured
     * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}s or
     * {@link net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder.ParameterBinder}s.
     *
     * @return A method delegation configuration without any pre-configuration.
     */
    public static WithCustomProperties withEmptyConfiguration() {
        return new WithCustomProperties(MethodDelegationBinder.AmbiguityResolver.NoOp.INSTANCE, Collections.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>emptyList());
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

    /**
     * An implementation delegate is responsible for executing the actual method delegation and for resolving the target methods.
     */
    protected interface ImplementationDelegate extends InstrumentedType.Prepareable {

        /**
         * A name prefix for fields.
         */
        String FIELD_NAME_PREFIX = "delegate";

        /**
         * Compiles this implementation delegate.
         *
         * @param instrumentedType The instrumented type.
         * @return A compiled implementation delegate.
         */
        Compiled compile(TypeDescription instrumentedType);

        /**
         * A compiled implementation delegate.
         */
        interface Compiled {

            /**
             * Resolves a stack manipulation that prepares the delegation invocation.
             *
             * @param instrumentedMethod The instrumented method.
             * @return A stack manipulation that is applied prior to loading arguments and executing the method call.
             */
            StackManipulation prepare(MethodDescription instrumentedMethod);

            /**
             * Resolves an invoker to use for invoking the delegation target.
             *
             * @return The method invoker to use.
             */
            MethodDelegationBinder.MethodInvoker invoke();

            /**
             * Returns a list of binding records to consider for delegation.
             *
             * @return A list of delegation binder records to consider.
             */
            List<MethodDelegationBinder.Record> getRecords();

            /**
             * A compiled implementation delegate for invoking a static method.
             */
            @EqualsAndHashCode
            class ForStaticCall implements Compiled {

                /**
                 * The list of records to consider.
                 */
                private final List<MethodDelegationBinder.Record> records;

                /**
                 * Creates a new compiled implementation delegate for a static method call.
                 *
                 * @param records The list of records to consider.
                 */
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
            }

            /**
             * A compiled implementation delegate that invokes methods on a field.
             */
            @EqualsAndHashCode
            class ForField implements Compiled {

                /**
                 * The field to delegate to.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * The records to consider for delegation.
                 */
                private final List<MethodDelegationBinder.Record> records;

                /**
                 * Creates a new compiled implementation delegate for a field delegation.
                 *
                 * @param fieldDescription The field to delegate to.
                 * @param records          The records to consider for delegation.
                 */
                protected ForField(FieldDescription fieldDescription, List<MethodDelegationBinder.Record> records) {
                    this.fieldDescription = fieldDescription;
                    this.records = records;
                }

                @Override
                public StackManipulation prepare(MethodDescription instrumentedMethod) {
                    if (instrumentedMethod.isStatic() && !fieldDescription.isStatic()) {
                        throw new IllegalStateException("Cannot read " + fieldDescription + " from " + instrumentedMethod);
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
            }

            /**
             * A compiled implementation delegate for a constructor delegation.
             */
            @EqualsAndHashCode
            class ForConstruction implements Compiled {

                /**
                 * The type to be constructed.
                 */
                private final TypeDescription typeDescription;

                /**
                 * The records to consider for delegation.
                 */
                private final List<MethodDelegationBinder.Record> records;

                /**
                 * Creates a new compiled implementation delegate for a constructor delegation.
                 *
                 * @param typeDescription The type to be constructed.
                 * @param records         The records to consider for delegation.
                 */
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
            }
        }

        /**
         * An implementation delegate for a static method delegation.
         */
        @EqualsAndHashCode
        class ForStaticMethod implements ImplementationDelegate {

            /**
             * The precompiled records.
             */
            private final List<MethodDelegationBinder.Record> records;

            /**
             * Creates a new implementation delegate for a static method delegation.
             *
             * @param records The precompiled record.
             */
            protected ForStaticMethod(List<MethodDelegationBinder.Record> records) {
                this.records = records;
            }

            /**
             * Precompiles a static method delegation for a given list of methods.
             *
             * @param methods                The methods to consider.
             * @param methodDelegationBinder The method delegation binder to use.
             * @return An appropriate implementation delegate.
             */
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
        }

        /**
         * An implementation delegate for invoking methods on a field that is declared by the instrumented type or a super type.
         */
        @EqualsAndHashCode
        abstract class ForField implements ImplementationDelegate {

            /**
             * The name of the field that is target of the delegation.
             */
            protected final String fieldName;

            /**
             * The method graph compiler to use.
             */
            protected final MethodGraph.Compiler methodGraphCompiler;

            /**
             * The parameter binders to use.
             */
            protected final List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders;

            /**
             * The matcher to use for filtering methods.
             */
            protected final ElementMatcher<? super MethodDescription> matcher;

            /**
             * Creates a new implementation delegate for a field delegation.
             *
             * @param fieldName           The name of the field that is target of the delegation.
             * @param methodGraphCompiler The method graph compiler to use.
             * @param parameterBinders    The parameter binders to use.
             * @param matcher             The matcher to use for filtering methods.
             */
            protected ForField(String fieldName,
                               MethodGraph.Compiler methodGraphCompiler,
                               List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                               ElementMatcher<? super MethodDescription> matcher) {
                this.fieldName = fieldName;
                this.methodGraphCompiler = methodGraphCompiler;
                this.parameterBinders = parameterBinders;
                this.matcher = matcher;
            }

            @Override
            public Compiled compile(TypeDescription instrumentedType) {
                FieldDescription fieldDescription = resolve(instrumentedType);
                if (!fieldDescription.getType().asErasure().isVisibleTo(instrumentedType)) {
                    throw new IllegalStateException(fieldDescription + " is not visible to " + instrumentedType);
                } else {
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

            /**
             * Resolves the field to which is delegated.
             *
             * @param instrumentedType The instrumented type.
             * @return The field that is the delegation target.
             */
            protected abstract FieldDescription resolve(TypeDescription instrumentedType);

            /**
             * An implementation target for a static field that is declared by the instrumented type and that is assigned an instance.
             */
            @EqualsAndHashCode(callSuper = true)
            protected static class WithInstance extends ForField {

                /**
                 * The target instance.
                 */
                private final Object target;

                /**
                 * The field's type.
                 */
                private final TypeDescription.Generic fieldType;

                /**
                 * Creates a new implementation delegate for invoking methods on a supplied instance.
                 *
                 * @param fieldName           The name of the field that is target of the delegation.
                 * @param methodGraphCompiler The method graph compiler to use.
                 * @param parameterBinders    The parameter binders to use.
                 * @param matcher             The matcher to use for filtering methods.
                 * @param target              The target instance.
                 * @param fieldType           The field's type.
                 */
                protected WithInstance(String fieldName,
                                       MethodGraph.Compiler methodGraphCompiler,
                                       List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                                       ElementMatcher<? super MethodDescription> matcher,
                                       Object target,
                                       TypeDescription.Generic fieldType) {
                    super(fieldName, methodGraphCompiler, parameterBinders, matcher);
                    this.target = target;
                    this.fieldType = fieldType;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType
                            .withField(new FieldDescription.Token(fieldName, Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, fieldType))
                            .withInitializer(new LoadedTypeInitializer.ForStaticField(fieldName, target));
                }

                @Override
                protected FieldDescription resolve(TypeDescription instrumentedType) {
                    if (!fieldType.asErasure().isVisibleTo(instrumentedType)) {
                        throw new IllegalStateException(fieldType + " is not visible to " + instrumentedType);
                    } else {
                        return instrumentedType.getDeclaredFields()
                                .filter(named(fieldName).and(fieldType(fieldType.asErasure())))
                                .getOnly();
                    }
                }
            }


            /**
             * An implementation target for a field that is declared by the instrumented type or a super type.
             */
            @EqualsAndHashCode(callSuper = true)
            protected static class WithLookup extends ForField {

                /**
                 * The field locator factory to use for locating the field to delegate to.
                 */
                private final FieldLocator.Factory fieldLocatorFactory;

                /**
                 * Creates a new implementation delegate for a field that is declared by the instrumented type or any super type.
                 *
                 * @param fieldName           The name of the field that is target of the delegation.
                 * @param methodGraphCompiler The method graph compiler to use.
                 * @param parameterBinders    The parameter binders to use.
                 * @param matcher             The matcher to use for filtering methods.
                 * @param fieldLocatorFactory The field locator factory to use for locating the field to delegate to.
                 */
                protected WithLookup(String fieldName,
                                     MethodGraph.Compiler methodGraphCompiler,
                                     List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                                     ElementMatcher<? super MethodDescription> matcher,
                                     FieldLocator.Factory fieldLocatorFactory) {
                    super(fieldName, methodGraphCompiler, parameterBinders, matcher);
                    this.fieldLocatorFactory = fieldLocatorFactory;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                protected FieldDescription resolve(TypeDescription instrumentedType) {
                    FieldLocator.Resolution resolution = fieldLocatorFactory.make(instrumentedType).locate(fieldName);
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Could not locate " + fieldName + " on " + instrumentedType);
                    } else {
                        return resolution.getField();
                    }
                }
            }
        }

        /**
         * An implementation delegate for constructing an instance.
         */
        @EqualsAndHashCode
        class ForConstruction implements ImplementationDelegate {

            /**
             * The type being constructed.
             */
            private final TypeDescription typeDescription;

            /**
             * The precompiled delegation records.
             */
            private final List<MethodDelegationBinder.Record> records;

            /**
             * Creates an implementation delegate for constructing a new instance.
             *
             * @param typeDescription The type being constructed.
             * @param records         The precompiled delegation records.
             */
            protected ForConstruction(TypeDescription typeDescription, List<MethodDelegationBinder.Record> records) {
                this.typeDescription = typeDescription;
                this.records = records;
            }

            /**
             * Creates an implementation delegate for constructing a new instance.
             *
             * @param typeDescription        The type being constructed.
             * @param methods                The constructors to consider.
             * @param methodDelegationBinder The method delegation binder to use.
             * @return An appropriate implementation delegate.
             */
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
        }
    }

    /**
     * The appender for implementing a {@link net.bytebuddy.implementation.MethodDelegation}.
     */
    @EqualsAndHashCode
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

        /**
         * The compiled implementation delegate.
         */
        private final ImplementationDelegate.Compiled compiled;

        /**
         * Creates a new appender for a method delegation.
         *
         * @param implementationTarget The implementation target of this implementation.
         * @param processor            The method delegation binder processor which is responsible for implementing the method delegation.
         * @param terminationHandler   A termination handler for a method delegation binder.
         * @param assigner             The assigner to use.
         * @param compiled             The compiled implementation delegate.
         */
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
    }

    /**
     * A {@link MethodDelegation} with custom configuration.
     */
    @EqualsAndHashCode
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
         * Creates a new method delegation with custom properties that does not filter any methods.
         *
         * @param ambiguityResolver The ambiguity resolver to use.
         * @param parameterBinders  The parameter binders to use.
         */
        protected WithCustomProperties(MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                                       List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders) {
            this(ambiguityResolver, parameterBinders, any());
        }

        /**
         * Creates a new method delegation with custom properties.
         *
         * @param ambiguityResolver The ambiguity resolver to use.
         * @param parameterBinders  The parameter binders to use.
         * @param matcher           The matcher to use for filtering relevant methods.
         */
        private WithCustomProperties(MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                                     List<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders,
                                     ElementMatcher<? super MethodDescription> matcher) {
            this.ambiguityResolver = ambiguityResolver;
            this.parameterBinders = parameterBinders;
            this.matcher = matcher;
        }

        /**
         * Configures this method delegation to use the supplied ambiguity resolvers when deciding which out of two ore
         * more legal delegation targets should be considered.
         *
         * @param ambiguityResolver The ambiguity resolvers to use in their application order.
         * @return A new delegation configuration which also applies the supplied ambiguity resolvers.
         */
        public WithCustomProperties withResolvers(MethodDelegationBinder.AmbiguityResolver... ambiguityResolver) {
            return withResolvers(Arrays.asList(ambiguityResolver));
        }

        /**
         * Configures this method delegation to use the supplied ambiguity resolvers when deciding which out of two ore
         * more legal delegation targets should be considered.
         *
         * @param ambiguityResolvers The ambiguity resolvers to use in their application order.
         * @return A new delegation configuration which also applies the supplied ambiguity resolvers.
         */
        public WithCustomProperties withResolvers(List<? extends MethodDelegationBinder.AmbiguityResolver> ambiguityResolvers) {
            return new WithCustomProperties(new MethodDelegationBinder.AmbiguityResolver.Compound(CompoundList.of(this.ambiguityResolver, ambiguityResolvers)), parameterBinders, matcher);
        }

        /**
         * Configures this method delegation to use the supplied parameter binders when deciding what value to assign to
         * a parameter of a delegation target.
         *
         * @param parameterBinder The parameter binders to use.
         * @return A new delegation configuration which also applies the supplied parameter binders.
         */
        public WithCustomProperties withBinders(TargetMethodAnnotationDrivenBinder.ParameterBinder<?>... parameterBinder) {
            return withBinders(Arrays.asList(parameterBinder));
        }

        /**
         * Configures this method delegation to use the supplied parameter binders when deciding what value to assign to
         * a parameter of a delegation target.
         *
         * @param parameterBinders The parameter binders to use.
         * @return A new delegation configuration which also applies the supplied parameter binders.
         */
        public WithCustomProperties withBinders(List<? extends TargetMethodAnnotationDrivenBinder.ParameterBinder<?>> parameterBinders) {
            return new WithCustomProperties(ambiguityResolver, CompoundList.of(this.parameterBinders, parameterBinders), matcher);
        }

        /**
         * Configures this method delegation to only consider methods or constructors as a delegation target if they match the supplied matcher.
         *
         * @param matcher The matcher any delegation target needs to match in order to be considered a for delegation.
         * @return A new delegation configuration which only considers methods for delegation if they match the supplied matcher.
         */
        public WithCustomProperties filter(ElementMatcher<? super MethodDescription> matcher) {
            return new WithCustomProperties(ambiguityResolver, parameterBinders, new ElementMatcher.Junction.Conjunction<MethodDescription>(this.matcher, matcher));
        }

        /**
         * Delegates any intercepted method to invoke a {@code static} method that is declared by the supplied type. To be considered
         * a valid delegation target, the target method must be visible and accessible to the instrumented type. This is the case if
         * the target type is either public or in the same package as the instrumented type and if the target method is either public
         * or non-private and in the same package as the instrumented type. Private methods can only be used as a delegation target if
         * the interception is targeting the instrumented type.
         *
         * @param type The target type for the delegation.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Class<?> type) {
            return to(new TypeDescription.ForLoadedType(type));
        }

        /**
         * Delegates any intercepted method to invoke a {@code static} method that is declared by the supplied type. To be considered
         * a valid delegation target, the target method must be visible and accessible to the instrumented type. This is the case if
         * the target type is either public or in the same package as the instrumented type and if the target method is either public
         * or non-private and in the same package as the instrumented type. Private methods can only be used as a delegation target if
         * the delegation is targeting the instrumented type.
         *
         * @param typeDescription The target type for the delegation.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
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
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target The target instance for the delegation.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target) {
            return to(target, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target              The target instance for the delegation.
         * @param methodGraphCompiler The method graph compiler to use.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target, MethodGraph.Compiler methodGraphCompiler) {
            return to(target, target.getClass(), methodGraphCompiler);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target    The target instance for the delegation.
         * @param fieldName The name of the field that is holding the {@code target} instance.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target, String fieldName) {
            return to(target, fieldName, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target              The target instance for the delegation.
         * @param fieldName           The name of the field that is holding the {@code target} instance.
         * @param methodGraphCompiler The method graph compiler to use.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
            return to(target, target.getClass(), fieldName, methodGraphCompiler);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target The target instance for the delegation.
         * @param type   The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target, Type type) {
            return to(target, type, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target              The target instance for the delegation.
         * @param type                The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
         * @param methodGraphCompiler The method graph compiler to use.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target, Type type, MethodGraph.Compiler methodGraphCompiler) {
            return to(target,
                    type,
                    String.format("%s$%s", ImplementationDelegate.FIELD_NAME_PREFIX, RandomString.hashOf(target.hashCode())),
                    methodGraphCompiler);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target    The target instance for the delegation.
         * @param type      The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
         * @param fieldName The name of the field that is holding the {@code target} instance.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target, Type type, String fieldName) {
            return to(target, type, fieldName, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method that is declared by the supplied type's instance or any
         * of its super types. To be considered a valid delegation target, a method must be visible and accessible to the instrumented type.
         * This is the case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param target              The target instance for the delegation.
         * @param type                The most specific type of which {@code target} should be cosnidered. Must be a super type of the target's actual type.
         * @param fieldName           The name of the field that is holding the {@code target} instance.
         * @param methodGraphCompiler The method graph compiler to use.
         * @return A method delegation that redirects method calls to a static method of the supplied type.
         */
        public MethodDelegation to(Object target, Type type, String fieldName, MethodGraph.Compiler methodGraphCompiler) {
            TypeDescription.Generic typeDescription = TypeDefinition.Sort.describe(type);
            if (!typeDescription.asErasure().isInstance(target)) {
                throw new IllegalArgumentException(target + " is not an instance of " + type);
            }
            return new MethodDelegation(new ImplementationDelegate.ForField.WithInstance(fieldName, methodGraphCompiler, parameterBinders, matcher, target, typeDescription),
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                    MethodDelegationBinder.AmbiguityResolver.DEFAULT);
        }

        /**
         * Delegates any intercepted method to invoke a constructor of the supplied type. To be considered a valid delegation target,
         * a constructor must be visible and accessible to the instrumented type. This is the case if the constructor's declaring type is
         * either public or in the same package as the instrumented type and if the constructor is either public or non-private and in
         * the same package as the instrumented type. Private constructors can only be used as a delegation target if the delegation is
         * targeting the instrumented type.
         *
         * @param type The type to construct.
         * @return A delegation that redirects method calls to a constructor of the supplied type.
         */
        public MethodDelegation toConstructor(Class<?> type) {
            return toConstructor(new TypeDescription.ForLoadedType(type));
        }

        /**
         * Delegates any intercepted method to invoke a constructor of the supplied type. To be considered a valid delegation target,
         * a constructor must be visible and accessible to the instrumented type. This is the case if the constructor's declaring type is
         * either public or in the same package as the instrumented type and if the constructor is either public or non-private and in
         * the same package as the instrumented type. Private constructors can only be used as a delegation target if the delegation is
         * targeting the instrumented type.
         *
         * @param typeDescription The type to construct.
         * @return A delegation that redirects method calls to a constructor of the supplied type.
         */
        public MethodDelegation toConstructor(TypeDescription typeDescription) {
            return new MethodDelegation(ImplementationDelegate.ForConstruction.of(typeDescription, typeDescription.getDeclaredMethods().filter(isConstructor().and(matcher)), TargetMethodAnnotationDrivenBinder.of(parameterBinders)),
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                    MethodDelegationBinder.AmbiguityResolver.DEFAULT);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
         * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
         * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param name The field's name.
         * @return A delegation that redirects invocations to a method of the specified field's instance.
         */
        public MethodDelegation toField(String name) {
            return toField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
         * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
         * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the delegation is targeting the instrumented type.
         *
         * @param name                The field's name.
         * @param fieldLocatorFactory The field locator factory to use.
         * @return A delegation that redirects invocations to a method of the specified field's instance.
         */
        public MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory) {
            return toField(name, fieldLocatorFactory, MethodGraph.Compiler.DEFAULT);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
         * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
         * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the ddelegation is targeting the instrumented type.
         *
         * @param name                The field's name.
         * @param methodGraphCompiler The method graph compiler to use.
         * @return A delegation that redirects invocations to a method of the specified field's instance.
         */
        public MethodDelegation toField(String name, MethodGraph.Compiler methodGraphCompiler) {
            return toField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE, methodGraphCompiler);
        }

        /**
         * Delegates any intercepted method to invoke a non-{@code static} method on the instance of the supplied field. To be
         * considered a valid delegation target, a method must be visible and accessible to the instrumented type. This is the
         * case if the method's declaring type is either public or in the same package as the instrumented type and if the method
         * is either public or non-private and in the same package as the instrumented type. Private methods can only be used as
         * a delegation target if the ddelegation is targeting the instrumented type.
         *
         * @param name                The field's name.
         * @param fieldLocatorFactory The field locator factory to use.
         * @param methodGraphCompiler The method graph compiler to use.
         * @return A delegation that redirects invocations to a method of the specified field's instance.
         */
        public MethodDelegation toField(String name, FieldLocator.Factory fieldLocatorFactory, MethodGraph.Compiler methodGraphCompiler) {
            return new MethodDelegation(new ImplementationDelegate.ForField.WithLookup(name, methodGraphCompiler, parameterBinders, matcher, fieldLocatorFactory),
                    TargetMethodAnnotationDrivenBinder.ParameterBinder.DEFAULTS,
                    MethodDelegationBinder.AmbiguityResolver.DEFAULT);
        }
    }
}
