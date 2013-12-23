package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.MethodArgument;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Argument {

    static class Handler implements AnnotationCallBinder.Handler<Argument> {

        private final Assigner assigner;

        public Handler(Assigner assigner) {
            this.assigner = assigner;
        }

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public Assignment assign(Class<?> assignmentTarget, Argument argument, ClassContext classContext, MethodContext methodContext) {
            if (methodContext.getArgumentTypes().size() > argument.value()) {
                return IllegalAssignment.INSTANCE;
            }
            String assignedType = methodContext.getArgumentTypes().get(argument.value());
            return MethodArgument.forType(assignedType).assignAt(
                    methodContext.getAggregateArgumentSize().get(argument.value()),
                    assigner.assign(assignmentTarget, assignedType, true)); // TODO: Check for @RuntimeType annotation.
        }
    }

    int value();
}
