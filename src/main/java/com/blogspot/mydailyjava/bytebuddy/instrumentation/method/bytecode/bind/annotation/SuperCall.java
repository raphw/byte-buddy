package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.auxiliary.MethodCallProxy;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface SuperCall {

    static enum Binder implements AnnotationDrivenBinder.ArgumentBinder<SuperCall> {
        INSTANCE;

        @Override
        public Class<SuperCall> getHandledType() {
            return SuperCall.class;
        }

        @Override
        public IdentifiedBinding<?> bind(SuperCall annotation,
                                         int targetParameterIndex,
                                         MethodDescription source,
                                         MethodDescription target,
                                         TypeDescription instrumentedType,
                                         Assigner assigner) {
            TypeDescription targetType = target.getParameterTypes().get(targetParameterIndex);
            if(targetType.represents(Runnable.class) || targetType.represents(Callable.class) || !targetType.represents(Object.class)) {
                throw new IllegalStateException("A method call proxy can only be assigned to Runnable or Callable types");
            } else if(target.isAbstract()) {
                return IdentifiedBinding.makeIllegal();
            } else {
                return IdentifiedBinding.makeAnonymous(new MethodCallProxy.AssignableSignatureCall(source));
            }
        }
    }
}
