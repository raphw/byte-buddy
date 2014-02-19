package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface MethodAttributeAppender {

    static interface Factory {

        static class Compound implements Factory {

            private final Factory[] factory;

            public Compound(Factory... factory) {
                this.factory = factory;
            }

            @Override
            public MethodAttributeAppender make(TypeDescription typeDescription) {
                MethodAttributeAppender[] methodAttributeAppender = new MethodAttributeAppender[factory.length];
                int index = 0;
                for(Factory factory : this.factory) {
                    methodAttributeAppender[index++] = factory.make(typeDescription);
                }
                return new MethodAttributeAppender.Compound(methodAttributeAppender);
            }
        }

        MethodAttributeAppender make(TypeDescription typeDescription);
    }

    static enum NoOp implements MethodAttributeAppender, Factory {
        INSTANCE;

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            /* do nothing */
        }
    }

    static class ForAnnotation implements MethodAttributeAppender, Factory {

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
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static class ForLoadedMethod implements MethodAttributeAppender, Factory {

        private final MethodDescription methodDescription;

        public ForLoadedMethod(Method method) {
            methodDescription = new MethodDescription.ForMethod(method);
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            ForInstrumentedMethod.INSTANCE.apply(methodVisitor, this.methodDescription);
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static enum ForInstrumentedMethod implements MethodAttributeAppender, Factory {
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
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static class Compound implements MethodAttributeAppender {

        private final MethodAttributeAppender[] methodAttributeAppender;

        public Compound(MethodAttributeAppender... methodAttributeAppender) {
            this.methodAttributeAppender = methodAttributeAppender;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            for(MethodAttributeAppender methodAttributeAppender : this.methodAttributeAppender) {
                methodAttributeAppender.apply(methodVisitor, methodDescription);
            }
        }
    }

    void apply(MethodVisitor methodVisitor, MethodDescription methodDescription);
}
