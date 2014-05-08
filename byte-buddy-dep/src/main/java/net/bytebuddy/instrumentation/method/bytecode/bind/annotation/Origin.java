package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.ClassConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant;
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
 * </ol>
 * Any other parameter type will cause an {@link java.lang.IllegalStateException}.
 *
 * @see net.bytebuddy.instrumentation.MethodDelegation
 * @see TargetMethodAnnotationDrivenBinder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface Origin {

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
                                                               TypeDescription instrumentedType,
                                                               Assigner assigner) {
            TypeDescription parameterType = target.getParameterTypes().get(targetParameterIndex);
            if (parameterType.represents(Class.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new ClassConstant(instrumentedType));
            } else if (parameterType.represents(Method.class)) {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(MethodConstant.forMethod(source));
            } else {
                throw new IllegalStateException("The " + target + " method's " + targetParameterIndex +
                        " is annotated with a Origin annotation with an argument not representing a Class" +
                        " or Method type");
            }
        }
    }
}
