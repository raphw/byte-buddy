package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface MethodAnnotationAppender {

    static interface Factory {

        MethodAnnotationAppender make(TypeDescription typeDescription);
    }

    static class ForAnnotation implements MethodAnnotationAppender, Factory {

        private static interface Target {

            static enum OnMethod implements Target {
                INSTANCE;

                @Override
                public AnnotationAppender.Target make(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    return new AnnotationAppender.Target.OnMethod(methodVisitor);
                }
            }

            static class OnMethodParameter implements Target {

                private final int parameterIndex;

                public OnMethodParameter(int parameterIndex) {
                    this.parameterIndex = parameterIndex;
                }

                @Override
                public AnnotationAppender.Target make(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    if (parameterIndex >= methodDescription.getParameterTypes().size()) {
                        throw new IllegalArgumentException("Method " + methodDescription
                                + " has less then " + parameterIndex + " parameters");
                    }
                    return new AnnotationAppender.Target.OnMethodParameter(methodVisitor, parameterIndex);
                }
            }

            AnnotationAppender.Target make(MethodVisitor methodVisitor, MethodDescription methodDescription);
        }

        private final Annotation annotation;
        private final Target target;

        public ForAnnotation(Annotation annotation) {
            this.annotation = annotation;
            target = Target.OnMethod.INSTANCE;
        }

        public ForAnnotation(Annotation annotation, int parameterIndex) {
            this.annotation = annotation;
            target = new Target.OnMethodParameter(parameterIndex);
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            AnnotationAppender appender = new AnnotationAppender.Default(
                    target.make(methodVisitor, methodDescription), AnnotationAppender.Visibility.VISIBLE);
            appender.append(annotation);
        }

        @Override
        public MethodAnnotationAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static class ForLoadedMethod implements MethodAnnotationAppender, Factory {

        private final MethodDescription methodDescription;

        public ForLoadedMethod(Method method) {
            methodDescription = new MethodDescription.ForMethod(method);
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            ForInstrumentedMethod.INSTANCE.apply(methodVisitor, this.methodDescription);
        }

        @Override
        public MethodAnnotationAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static enum ForInstrumentedMethod implements MethodAnnotationAppender, Factory {
        INSTANCE;

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            AnnotationAppender methodAppender = new AnnotationAppender.Default(
                    new AnnotationAppender.Target.OnMethod(methodVisitor), AnnotationAppender.Visibility.VISIBLE);
            for (Annotation annotation : methodDescription.getAnnotations()) {
                methodAppender.append(annotation);
            }
            int i = 0;
            for (Annotation[] annotations : methodDescription.getParameterAnnotations()) {
                AnnotationAppender parameterAppender = new AnnotationAppender.Default(
                        new AnnotationAppender.Target.OnMethodParameter(methodVisitor, i++), AnnotationAppender.Visibility.VISIBLE);
                for (Annotation annotation : annotations) {
                    parameterAppender.append(annotation);
                }
            }
        }

        @Override
        public MethodAnnotationAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    void apply(MethodVisitor methodVisitor, MethodDescription methodDescription);
}
