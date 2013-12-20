package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.MethodArgumentAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.AssignmentExaminer;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;

public @interface Argument {

    static class Handler implements AnnotationCallBinder.Handler<Argument> {

        private final AssignmentExaminer assignmentExaminer;

        public Handler(AssignmentExaminer assignmentExaminer) {
            this.assignmentExaminer = assignmentExaminer;
        }

        @Override
        public Class<Argument> getHandledType() {
            return Argument.class;
        }

        @Override
        public Assignment assign(Class<?> assignmentTarget, Argument argument, ClassContext classContext, MethodContext methodContext) {
            if (methodContext.getArgumentType().size() > argument.value()) {
                return IllegalAssignment.INSTANCE;
            }
            String assignedType = methodContext.getArgumentType().get(argument.value());
            return MethodArgumentAssignment.of(assignedType).applyTo(argument.value(), assignmentExaminer.assign(assignmentTarget, assignedType));
        }
    }

    int value();
}
