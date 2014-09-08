package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.*;
import net.bytebuddy.instrumentation.type.TypeDescription;

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
 * <li>If the annotated parameter is of type {@link java.lang.String}, the parameter is assigned a string describing
 * a unique method signature of the method it intercepts. This string is a concatenation of:
 * <ul><li>The method's name</li>
 * <li>The <i>(</i> symbol</li>
 * <li>A list of the method's parameters'
 * <a href=http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3>descriptors</a></li>
 * <li>The <i>)</i> symbol</li>
 * <li>The <a href=http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3>descriptor</a> of the
 * method's return type</li></ul>
 * This unique signature allows the unambiguous identification of a particular class's methods while avoid the rather
 * expensive creation of a {@link java.lang.reflect.Method} instance.
 * </li>
 * <li>If the annotated type is {@link java.lang.invoke.MethodHandle}, a handle of the intercepted method is injected.
 * A {@link java.lang.invoke.MethodHandle} is stored in a class's constant pool and does therefore not face the same
 * runtime performance limitations as a {@link java.lang.reflect.Method} reference. Method handles are only supported
 * for byte code versions starting from Java 7.</li>
 * <li>If the annotated type is {@link java.lang.invoke.MethodType}, a description of the intercepted method's type
 * is injected. Method type descriptions are only supported for byte code versions starting from Java 7.</li>
 * </ol>
 * Any other parameter type will cause an {@link java.lang.IllegalStateException}.
 *
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Origin {

    /**
     * If this value is set to {@code true} and the annotated parameter is a {@link java.lang.reflect.Method} type,
     * the value that is assigned to this parameter is cached in a {@code static} field. Otherwise, the instance is
     * looked up from its defining {@link java.lang.Class} on every invocation of the intercepted method.
     * <p>&nbsp;</p>
     * {@link java.lang.reflect.Method} look-ups are normally cached by its defining {@link java.lang.Class} what
     * makes a repeated look-up of a method little expensive. However, because {@link java.lang.reflect.Method}
     * instances are mutable by their {@link java.lang.reflect.AccessibleObject} contact, any looked-up instance
     * needs to be copied by its defining {@link java.lang.Class} before exposing it. This can cause performance
     * deficits when a method is for example called repeatedly in a loop. By enabling the method cache, this
     * performance penalty can be avoided by caching a single {@link java.lang.reflect.Method} instance for
     * any intercepted method as a {@code static} field in the instrumented type.
     *
     * @return {@code true} if the annotated {@link java.lang.reflect.Method} parameter should be assigned a cached
     * instance. For any other parameter type, this value is ignored.
     */
    boolean cacheMethod() default false;

    /**
     * A binder for binding parameters that are annotated with
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin}.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Origin> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Class<Origin> getHandledType() {
            return Origin.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(Origin annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getParameterTypes().get(targetParameterIndex);
            if (parameterType.represents(Class.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(ClassConstant.of(instrumentationTarget.getOriginType()));
            } else if (parameterType.represents(Method.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(annotation.cacheMethod()
                        ? MethodConstant.forMethod(source).cached()
                        : MethodConstant.forMethod(source));
            } else if (parameterType.represents(String.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new TextConstant(source.getUniqueSignature()));
            } else if (MethodHandleConstant.isRepresentedBy(parameterType)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(MethodHandleConstant.of(source));
            } else if (MethodTypeConstant.isRepresentedBy(parameterType)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new MethodTypeConstant(source));
            } else {
                throw new IllegalStateException("The " + target + " method's " + targetParameterIndex +
                        " parameter is annotated with a Origin annotation with an argument not representing a Class" +
                        " Method, String, MethodType or MethodHandle type");
            }
        }
    }
}
