package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

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
    enum NoOp implements MethodAttributeAppender, Factory {

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

        @Override
        public String toString() {
            return "MethodAttributeAppender.NoOp." + name();
        }
    }

    /**
     * A factory that creates method attribute appenders for a given type.
     */
    interface Factory {

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
        class Compound implements Factory {

            /**
             * The factories this compound factory represents in their application order.
             */
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
                return "MethodAttributeAppender.Factory.Compound{factory=" + Arrays.toString(factory) + '}';
            }
        }
    }

    /**
     * Implementation of a method attribute appender that writes all annotations of the instrumented method to the
     * method that is being created. This includes method and parameter annotations.
     */
    class ForInstrumentedMethod implements Factory {

        /**
         * The value filter to apply for discovering which values of an annotation should be written.
         */
        private final AnnotationAppender.ValueFilter valueFilter;

        /**
         * Creates a new appender for appending the instrumented method's annotation to the created method.
         *
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForInstrumentedMethod(AnnotationAppender.ValueFilter valueFilter) {
            this.valueFilter = valueFilter;
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return new Appender(typeDescription, valueFilter);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && valueFilter.equals(((ForInstrumentedMethod) other).valueFilter);
        }

        @Override
        public int hashCode() {
            return valueFilter.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForInstrumentedMethod{" +
                    "valueFilter=" + valueFilter +
                    '}';
        }

        /**
         * An appender for an instrumented method that only appends the intercepted method's annotations if it is not already declared by the
         * instrumented type, i.e. it is defined explicitly.
         */
        protected static class Appender implements MethodAttributeAppender {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The value filter to apply for discovering which values of an annotation should be written.
             */
            private final AnnotationAppender.ValueFilter valueFilter;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The instrumented type.
             * @param valueFilter      The value filter to apply for discovering which values of an annotation should be written.
             */
            protected Appender(TypeDescription instrumentedType, AnnotationAppender.ValueFilter valueFilter) {
                this.instrumentedType = instrumentedType;
                this.valueFilter = valueFilter;
            }

            @Override
            public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                if (methodDescription.getDeclaringType().equals(instrumentedType)) {
                    return;
                }
                AnnotationAppender methodAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethod(methodVisitor), valueFilter);
                for (AnnotationDescription annotation : methodDescription.getDeclaredAnnotations()) {
                    methodAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
                }
                int index = 0;
                for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
                    AnnotationAppender parameterAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethodParameter(methodVisitor, index++), valueFilter);
                    for (AnnotationDescription annotation : parameterDescription.getDeclaredAnnotations()) {
                        parameterAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
                    }
                }
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Appender appender = (Appender) other;
                return instrumentedType.equals(appender.instrumentedType)
                        && valueFilter.equals(appender.valueFilter);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + valueFilter.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodAttributeAppender.ForInstrumentedMethod.Appender{" +
                        "instrumentedType=" + instrumentedType +
                        ", valueFilter=" + valueFilter +
                        '}';
            }
        }
    }

    /**
     * Appends an annotation to a method or method parameter. The visibility of the annotation is determined by the
     * annotation type's {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    class ForAnnotation implements MethodAttributeAppender, Factory {

        /**
         * the annotations this method attribute appender is writing to its target.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * The target to which the annotations are written to.
         */
        private final Target target;

        /**
         * The value filter to apply for discovering which values of an annotation should be written.
         */
        private final AnnotationAppender.ValueFilter valueFilter;

        /**
         * Creates a new appender for appending an annotation to a method parameter.
         *
         * @param parameterIndex The index of the parameter to which the annotations should be written.
         * @param valueFilter    The value filter to apply for discovering which values of an annotation should be written.
         * @param annotation     The annotations that should be written.
         */
        public ForAnnotation(int parameterIndex, AnnotationAppender.ValueFilter valueFilter, Annotation... annotation) {
            this(parameterIndex, new AnnotationList.ForLoadedAnnotation(annotation), valueFilter);
        }

        /**
         * Creates a new appender for appending an annotation to a method parameter.
         *
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         * @param annotation  The annotations that should be written.
         */
        public ForAnnotation(AnnotationAppender.ValueFilter valueFilter, Annotation... annotation) {
            this(new AnnotationList.ForLoadedAnnotation(annotation), valueFilter);
        }

        /**
         * Creates a new appender for appending an annotation to a method.
         *
         * @param parameterIndex The index of the parameter to which the annotations should be written.
         * @param annotations    The annotations that should be written.
         * @param valueFilter    The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForAnnotation(int parameterIndex, List<? extends AnnotationDescription> annotations, AnnotationAppender.ValueFilter valueFilter) {
            this.annotations = annotations;
            target = new Target.OnMethodParameter(parameterIndex);
            this.valueFilter = valueFilter;
        }

        /**
         * Creates a new appender for appending an annotation to a method.
         *
         * @param annotations The annotations that should be written.
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForAnnotation(List<? extends AnnotationDescription> annotations, AnnotationAppender.ValueFilter valueFilter) {
            this.annotations = annotations;
            target = Target.OnMethod.INSTANCE;
            this.valueFilter = valueFilter;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            AnnotationAppender appender = new AnnotationAppender.Default(target.make(methodVisitor, methodDescription), valueFilter);
            for (AnnotationDescription annotation : this.annotations) {
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
                    && annotations.equals(((ForAnnotation) other).annotations)
                    && valueFilter.equals(((ForAnnotation) other).valueFilter)
                    && target.equals(((ForAnnotation) other).target);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * annotations.hashCode() + valueFilter.hashCode()) + target.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForAnnotation{" +
                    "annotations=" + annotations +
                    ", valueFilter=" + valueFilter +
                    ", target=" + target +
                    '}';
        }

        /**
         * Represents the target on which this method attribute appender should write its annotations to.
         */
        protected interface Target {

            /**
             * Materializes the target for a given creation process.
             *
             * @param methodVisitor     The method visitor to which the attributes that are represented by this
             *                          attribute appender are written to.
             * @param methodDescription The description of the method for which the given method visitor creates an
             *                          instrumentation for.
             * @return The target of the annotation appender this target represents.
             */
            AnnotationAppender.Target make(MethodVisitor methodVisitor, MethodDescription methodDescription);

            /**
             * A method attribute appender target for writing annotations directly onto the method.
             */
            enum OnMethod implements Target {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public AnnotationAppender.Target make(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    return new AnnotationAppender.Target.OnMethod(methodVisitor);
                }

                @Override
                public String toString() {
                    return "MethodAttributeAppender.ForAnnotation.Target.OnMethod." + name();
                }
            }

            /**
             * A method attribute appender target for writing annotations onto a given method parameter.
             */
            class OnMethodParameter implements Target {

                /**
                 * The index of the parameter to write the annotation to.
                 */
                private final int parameterIndex;

                /**
                 * Creates a target for a method attribute appender for a method parameter of the given index.
                 *
                 * @param parameterIndex The index of the target parameter.
                 */
                protected OnMethodParameter(int parameterIndex) {
                    this.parameterIndex = parameterIndex;
                }

                @Override
                public AnnotationAppender.Target make(MethodVisitor methodVisitor, MethodDescription methodDescription) {
                    if (parameterIndex >= methodDescription.getParameters().size()) {
                        throw new IllegalArgumentException("Method " + methodDescription + " has less then " + parameterIndex + " parameters");
                    }
                    return new AnnotationAppender.Target.OnMethodParameter(methodVisitor, parameterIndex);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && parameterIndex == ((OnMethodParameter) other).parameterIndex;
                }

                @Override
                public int hashCode() {
                    return parameterIndex;
                }

                @Override
                public String toString() {
                    return "MethodAttributeAppender.ForAnnotation.Target.OnMethodParameter{parameterIndex=" + parameterIndex + '}';
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
    class ForMethod implements MethodAttributeAppender, Factory {

        /**
         * The method of which the annotations are to be copied.
         */
        private final MethodDescription methodDescription;

        /**
         * The value filter to apply for discovering which values of an annotation should be written.
         */
        private final AnnotationAppender.ValueFilter valueFilter;

        /**
         * Creates an that copies the annotations of a given constructor to its target.
         *
         * @param constructor The constructor of which the annotations should be copied.
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForMethod(Constructor<?> constructor, AnnotationAppender.ValueFilter valueFilter) {
            this(new MethodDescription.ForLoadedConstructor(constructor), valueFilter);
        }

        /**
         * Creates an that copies the annotations of a given method to its target.
         *
         * @param method      The method of which the annotations should be copied.
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForMethod(Method method, AnnotationAppender.ValueFilter valueFilter) {
            this(new MethodDescription.ForLoadedMethod(method), valueFilter);
        }

        /**
         * Creates an that copies the annotations of a given method description to its target.
         *
         * @param methodDescription The method description of which the annotations should be copied.
         * @param valueFilter       The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForMethod(MethodDescription methodDescription, AnnotationAppender.ValueFilter valueFilter) {
            this.methodDescription = methodDescription;
            this.valueFilter = valueFilter;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            if (this.methodDescription.getParameters().size() > methodDescription.getParameters().size()) {
                throw new IllegalArgumentException(this.methodDescription + " has more parameters than the instrumented method " + methodDescription);
            }
            AnnotationAppender methodAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethod(methodVisitor), valueFilter);
            for (AnnotationDescription annotation : this.methodDescription.getDeclaredAnnotations()) {
                methodAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
            int index = 0;
            for (ParameterDescription parameterDescription : this.methodDescription.getParameters()) {
                AnnotationAppender parameterAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethodParameter(methodVisitor, index++), valueFilter);
                for (AnnotationDescription annotation : parameterDescription.getDeclaredAnnotations()) {
                    parameterAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
                }
            }
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodDescription.equals(((ForMethod) other).methodDescription)
                    && valueFilter.equals(((ForMethod) other).valueFilter);
        }

        @Override
        public int hashCode() {
            return 31 * methodDescription.hashCode() + valueFilter.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForMethod{" +
                    "methodDescription=" + methodDescription +
                    ", valueFilter=" + valueFilter +
                    '}';
        }
    }

    /**
     * A method attribute appender that combines several method attribute appenders to be represented as a single
     * method attribute appender.
     */
    class Compound implements MethodAttributeAppender {

        /**
         * The method attribute appenders this compound appender represents in their application order.
         */
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
            return "MethodAttributeAppender.Compound{methodAttributeAppender=" + Arrays.toString(methodAttributeAppender) + '}';
        }
    }
}
