package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.utility.JavaType;

import java.lang.annotation.*;
import java.lang.reflect.Method;

/**
 * The origin annotation provides some meta information about the source method that is bound to this method where
 * the binding is dependant of the parameter's type:
 * <ol>
 * <li>If the annotated parameter is of type {@link java.lang.reflect.Method}, the parameter is assigned a reference
 * to the method it intercepts.</li>
 * <li>If the annotated parameter is of type {@link java.lang.Class}, the parameter is assigned a reference of the
 * type of the instrumented type.</li>
 * <li>If the annotated parameter is of type {@link java.lang.String}, the parameter is assigned a string with
 * the value that would be returned by the {@link Method#toString()} method.
 * </li>
 * <li>If the annotated parameter is a {@code int} type, it is assigned the intercepted method's modifiers.</li>
 * <li>If the annotated type is {@code java.lang.invoke.MethodHandle}, a handle of the intercepted method is injected.
 * A {@code java.lang.invoke.MethodHandle} is stored in a class's constant pool and does therefore not face the same
 * runtime performance limitations as a (non-cached) {@link java.lang.reflect.Method} reference. Method handles are
 * only supported for byte code versions starting from Java 7.</li>
 * <li>If the annotated type is {@code java.lang.invoke.MethodType}, a description of the intercepted method's type
 * is injected. Method type descriptions are only supported for byte code versions starting from Java 7.</li>
 * </ol>
 * Any other parameter type will cause an {@link java.lang.IllegalStateException}.
 *
 * @see net.bytebuddy.implementation.MethodDelegation
 * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Origin {

    /**
     * Determines if the value that is assigned by this annotation is cached. For values that can be stored in the constant pool,
     * this value is ignored as such values are cached implicitly. As a result, this value currently only affects caching of
     * {@link Method} instances.
     *
     * @return {@code true} if the value for this parameter should be cached in a {@code static} field inside the instrumented class.
     */
    boolean cache() default true;

    /**
     * A binder for binding parameters that are annotated with
     * {@link net.bytebuddy.implementation.bind.annotation.Origin}.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Origin> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<Origin> getHandledType() {
            return Origin.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Origin> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getType().asErasure();
            if (parameterType.represents(Class.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(ClassConstant.of(implementationTarget.getOriginType()));
            } else if (parameterType.represents(Method.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(annotation.loadSilent().cache()
                        ? MethodConstant.forMethod(source.asDefined()).cached()
                        : MethodConstant.forMethod(source.asDefined()));
            } else if (parameterType.represents(String.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new TextConstant(source.toString()));
            } else if (parameterType.represents(int.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(IntegerConstant.forValue(source.getModifiers()));
            } else if (parameterType.equals(JavaType.METHOD_HANDLE.getTypeStub())) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(MethodHandleConstant.of(source.asDefined()));
            } else if (parameterType.equals(JavaType.METHOD_TYPE.getTypeStub())) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(MethodTypeConstant.of(source.asDefined()));
            } else {
                throw new IllegalStateException("The " + target + " method's " + target.getIndex() +
                        " parameter is annotated with a Origin annotation with an argument not representing a Class," +
                        " Method, String, int, MethodType or MethodHandle type");
            }
        }

        @Override
        public String toString() {
            return "Origin.Binder." + name();
        }
    }
}
