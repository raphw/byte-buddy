package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;

import java.lang.annotation.*;
import java.lang.reflect.Method;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Argument {

    static class Handler implements AnnotationCallBinder.ArgumentHandler<Argument> {

        private final Assigner assigner;

        public Handler(Assigner assigner) {
            this.assigner = assigner;
        }

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public Assignment assign(int parameterIndex, Argument argument, Method sourceMethod, Method targetMethod) {
            if(sourceMethod.getParameterTypes().length > argument.value()) {
                return IllegalAssignment.INSTANCE;
            }
//            return MethodArgument.loading(targetMethod.getParameterTypes()[parameterIndex]).loadFromIndex(sourceMethod.getParameterTypes()[argument.value()])
            return null;
        }
    }

    int value();
}
