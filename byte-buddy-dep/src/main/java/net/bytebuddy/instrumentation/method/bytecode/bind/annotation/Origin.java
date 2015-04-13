package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.*;
import net.bytebuddy.instrumentation.type.TypeDescription;
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
 * <li>If the annotated type is {@code java.lang.invoke.MethodHandle}, a handle of the intercepted method is injected.
 * A {@code java.lang.invoke.MethodHandle} is stored in a class's constant pool and does therefore not face the same
 * runtime performance limitations as a {@link java.lang.reflect.Method} reference. Method handles are only supported
 * for byte code versions starting from Java 7.</li>
 * <li>If the annotated type is {@code java.lang.invoke.MethodType}, a description of the intercepted method's type
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

    boolean cache() default true;

    /**
     * A binder for binding parameters that are annotated with
     * {@link net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin}.
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
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getTypeDescription();
            if (parameterType.represents(Class.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(ClassConstant.of(instrumentationTarget.getOriginType()));
            } else if (parameterType.represents(Method.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(annotation.loadSilent().cache()
                        ? MethodConstant.forMethod(source).cached()
                        : MethodConstant.forMethod(source));
            } else if (parameterType.represents(String.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new TextConstant(source.toString()));
            } else if (parameterType.equals(JavaType.METHOD_HANDLE.getTypeStub())) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(MethodHandleConstant.of(source));
            } else if (parameterType.equals(JavaType.METHOD_TYPE.getTypeStub())) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(MethodTypeConstant.of(source));
            } else {
                throw new IllegalStateException("The " + target + " method's " + target.getIndex() +
                        " parameter is annotated with a Origin annotation with an argument not representing a Class" +
                        " Method, String, MethodType or MethodHandle type");
            }
        }

        @Override
        public String toString() {
            return "Origin.Binder." + name();
        }
    }
}
