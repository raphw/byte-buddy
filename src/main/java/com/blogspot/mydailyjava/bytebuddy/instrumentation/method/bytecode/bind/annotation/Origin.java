package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.ClassConstant;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.*;
import java.lang.reflect.Method;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Origin {

    static enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Origin> {
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
                return new MethodDelegationBinder.ParameterBinding.Anonymous(new MethodConstant(source));
            } else {
                throw new IllegalStateException("The " + target + " method's " + targetParameterIndex +
                        " is annotated with a Origin annotation with an argument not representing a Class" +
                        " or Method type");
            }
        }
    }
}
