package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.MethodVisitor}.
 */
public interface MethodAttributeAppender {

    /**
     * Applies this attribute appender to a given method visitor.
     *
     * @param methodVisitor     The method visitor to which the attributes that are represented by this attribute
     *                          appender are written to.
     * @param methodDescription The description of the method for which the given method visitor creates an
     *                          instrumentation for.
     */
    void apply(MethodVisitor methodVisitor, MethodDescription methodDescription);

    /**
     * A method attribute appender that does not append any attributes.
     */
    static enum NoOp implements MethodAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
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

    /**
     * Implementation of a method attribute appender that writes all annotations of the instrumented method to the
     * method that is being created. This includes method and parameter annotations.
     */
    static enum ForInstrumentedMethod implements MethodAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            AnnotationAppender methodAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethod(methodVisitor));
            for (Annotation annotation : methodDescription.getAnnotations()) {
                methodAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
            int i = 0;
            for (Annotation[] annotations : methodDescription.getParameterAnnotations()) {
                AnnotationAppender parameterAppender =
                        new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethodParameter(methodVisitor, i++));
                for (Annotation annotation : annotations) {
                    parameterAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
                }
            }
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    /**
     * A factory that creates method attribute appenders for a given type.
     */
    static interface Factory {

        /**
         * Returns a method attribute appender that is applicable for a given type description.
         *
         * @param typeDescription The type for which a method attribute appender is to be applied for.
         * @return The method attribute appender which should be applied for the given type.
         */
        MethodAttributeAppender make(TypeDescription typeDescription);

        /**
         * A method attribute appender factory that combines several method attribute appender factories to be
         * represented as a single factory.
         */
        static class Compound implements Factory {

            private final Factory[] factory;

            /**
             * Creates a new compound method attribute appender factory.
             *
             * @param factory The factories that are to be combined by this compound factory in the order of their
             *                application.
             */
            public Compound(Factory... factory) {
                this.factory = factory;
            }

            @Override
            public MethodAttributeAppender make(TypeDescription typeDescription) {
                MethodAttributeAppender[] methodAttributeAppender = new MethodAttributeAppender[factory.length];
                int index = 0;
                for (Factory factory : this.factory) {
                    methodAttributeAppender[index++] = factory.make(typeDescription);
                }
                return new MethodAttributeAppender.Compound(methodAttributeAppender);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Arrays.equals(factory, ((Compound) other).factory);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(factory);
            }

            @Override
            public String toString() {
                return "MethodAttributeAppender.Factory.Compound{" + Arrays.toString(factory) + '}';
            }
        }
    }

    /**
     * Appends an annotation to a method or method parameter. The visibility of the annotation is determined by the
     * annotation type's {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    static class ForAnnotation implements MethodAttributeAppender, Factory {

        private final Annotation[] annotation;
        private final Target target;

        /**
         * Create a new annotation appender for a method.
         *
         * @param annotation The annotations to append to the target method.
         */
        public ForAnnotation(Annotation... annotation) {
            this.annotation = annotation;
            target = Target.OnMethod.INSTANCE;
        }

        /**
         * Create a new annotation appender for a method parameter.
         *
         * @param parameterIndex The index of the target parameter.
         * @param annotation     The annotations to append to the target method parameter.
         */
        public ForAnnotation(int parameterIndex, Annotation... annotation) {
            this.annotation = annotation;
            target = new Target.OnMethodParameter(parameterIndex);
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            AnnotationAppender appender =
                    new AnnotationAppender.Default(target.make(methodVisitor, methodDescription));
            for (Annotation annotation : this.annotation) {
                appender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(annotation, ((ForAnnotation) other).annotation)
                    && target.equals(((ForAnnotation) other).target);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(annotation) + target.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForAnnotation{" +
                    "annotation=" + Arrays.toString(annotation) +
                    ", target=" + target +
                    '}';
        }

        private static interface Target {

            AnnotationAppender.Target make(MethodVisitor methodVisitor, MethodDescription methodDescription);

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

                @Override
                public boolean equals(Object o) {
                    return this == o || !(o == null || getClass() != o.getClass())
                            && parameterIndex == ((OnMethodParameter) o).parameterIndex;
                }

                @Override
                public int hashCode() {
                    return parameterIndex;
                }

                @Override
                public String toString() {
                    return "Target.OnMethodParameter{parameterIndex=" + parameterIndex + '}';
                }
            }
        }
    }

    /**
     * Implementation of a method attribute appender that writes all annotations of a given loaded method to the
     * method that is being created. This includes method and parameter annotations. In order to being able to do so,
     * the target method and the given method must have compatible signatures, i.e. an identical number of method
     * parameters. Otherwise, an exception is thrown when this attribute appender is applied on a method.
     */
    static class ForLoadedMethod implements MethodAttributeAppender, Factory {

        private final Method method;

        /**
         * Creates a new attribute appender for a loaded method.
         *
         * @param method The method which is read for annotations to write to an instrumented method.
         */
        public ForLoadedMethod(Method method) {
            this.method = method;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            if (method.getParameterTypes().length > methodDescription.getParameterTypes().size()) {
                throw new IllegalArgumentException("The constructor " + method + " has more parameters than the " +
                        "instrumented method " + methodDescription);
            }
            ForInstrumentedMethod.INSTANCE.apply(methodVisitor, new MethodDescription.ForLoadedMethod(method));
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && method.equals(((ForLoadedMethod) other).method);
        }

        @Override
        public int hashCode() {
            return method.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForLoadedMethod{method=" + method + '}';
        }
    }

    /**
     * Implementation of a method attribute appender that writes all annotations of a given loaded constructor to the
     * method that is being created. This includes method and parameter annotations. In order to being able to do so,
     * the target method and the given constructor must have compatible signatures, i.e. an identical number of method
     * parameters. Otherwise, an exception is thrown when this attribute appender is applied on a method.
     */
    static class ForLoadedConstructor implements MethodAttributeAppender, Factory {

        private final Constructor<?> constructor;

        /**
         * Creates a new attribute appender for a loaded constructor.
         *
         * @param constructor The constructor which is read for annotations to write to an instrumented method.
         */
        public ForLoadedConstructor(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            if (constructor.getParameterTypes().length > methodDescription.getParameterTypes().size()) {
                throw new IllegalArgumentException("The constructor " + constructor + " has more parameters than the " +
                        "instrumented method " + methodDescription);
            }
            ForInstrumentedMethod.INSTANCE.apply(methodVisitor, new MethodDescription.ForLoadedConstructor(constructor));
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && constructor.equals(((ForLoadedConstructor) other).constructor);
        }

        @Override
        public int hashCode() {
            return constructor.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForLoadedConstructor{constructor=" + constructor + '}';
        }
    }

    /**
     * A method attribute appender that combines several method attribute appenders to be represented as a single
     * method attribute appender.
     */
    static class Compound implements MethodAttributeAppender {

        private final MethodAttributeAppender[] methodAttributeAppender;

        /**
         * Creates a new compound method attribute appender.
         *
         * @param methodAttributeAppender The method attribute appenders that are to be combined by this compound appender
         *                                in the order of their application.
         */
        public Compound(MethodAttributeAppender... methodAttributeAppender) {
            this.methodAttributeAppender = methodAttributeAppender;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            for (MethodAttributeAppender methodAttributeAppender : this.methodAttributeAppender) {
                methodAttributeAppender.apply(methodVisitor, methodDescription);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(methodAttributeAppender, ((Compound) other).methodAttributeAppender);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(methodAttributeAppender);
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.Compound{" + Arrays.toString(methodAttributeAppender) + '}';
        }
    }
}
