package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.MethodVisitor}.
 */
public interface MethodAttributeAppender {

    /**
     * Applies this attribute appender to a given method visitor.
     *
     * @param methodVisitor         The method visitor to which the attributes that are represented by this attribute
     *                              appender are written to.
     * @param methodDescription     The description of the method for which the given method visitor creates an
     *                              instrumentation for.
     * @param annotationValueFilter The annotation value filter to apply when the annotations are written.
     */
    void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter);

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
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter) {
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
            private final List<Factory> factories;

            /**
             * Creates a new compound method attribute appender factory.
             *
             * @param factory The factories that are to be combined by this compound factory in the order of their application.
             */
            public Compound(Factory... factory) {
                this(Arrays.asList(factory));
            }

            /**
             * Creates a new compound method attribute appender factory.
             *
             * @param factories The factories that are to be combined by this compound factory in the order of their application.
             */
            public Compound(List<? extends Factory> factories) {
                this.factories = new ArrayList<Factory>();
                for (Factory factory : factories) {
                    if (factory instanceof Compound) {
                        this.factories.addAll(((Compound) factory).factories);
                    } else {
                        this.factories.add(factory);
                    }
                }
            }

            @Override
            public MethodAttributeAppender make(TypeDescription typeDescription) {
                List<MethodAttributeAppender> methodAttributeAppenders = new ArrayList<MethodAttributeAppender>(factories.size());
                for (Factory factory : factories) {
                    methodAttributeAppenders.add(factory.make(typeDescription));
                }
                return new MethodAttributeAppender.Compound(methodAttributeAppenders);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && factories.equals(((Compound) other).factories);
            }

            @Override
            public int hashCode() {
                return factories.hashCode();
            }

            @Override
            public String toString() {
                return "MethodAttributeAppender.Factory.Compound{factories=" + factories + '}';
            }
        }
    }

    /**
     * <p>
     * Implementation of a method attribute appender that writes all annotations of the instrumented method to the
     * method that is being created. This includes method and parameter annotations.
     * </p>
     * <p>
     * <b>Important</b>: This attribute appender does not apply for annotation types within the {@code jdk.internal.} namespace
     * which are silently ignored. If such annotations should be inherited, they need to be added explicitly.
     * </p>
     */
    enum ForInstrumentedMethod implements MethodAttributeAppender, Factory {

        /**
         * Appends all annotations of the instrumented method but not the annotations of the method's receiver type if such a type exists.
         */
        EXCLUDING_RECEIVER {
            @Override
            protected AnnotationAppender appendReceiver(AnnotationAppender annotationAppender,
                                                        AnnotationValueFilter annotationValueFilter,
                                                        MethodDescription methodDescription) {
                return annotationAppender;
            }
        },

        /**
         * <p>
         * Appends all annotations of the instrumented method including the annotations of the method's receiver type if such a type exists.
         * </p>
         * <p>
         * If a method is overridden, the annotations can be misplaced if the overriding class does not expose a similar structure to
         * the method that declared the method, i.e. the same amount of type variables and similar owner types. If this is not the case,
         * type annotations are appended as if the overridden method was declared by the original type. This does not corrupt the resulting
         * class file but it might result in type annotations not being visible via core reflection. This might however confuse other tools
         * that parse the resulting class file manually.
         * </p>
         */
        INCLUDING_RECEIVER {
            @Override
            protected AnnotationAppender appendReceiver(AnnotationAppender annotationAppender,
                                                        AnnotationValueFilter annotationValueFilter,
                                                        MethodDescription methodDescription) {
                TypeDescription.Generic receiverType = methodDescription.getReceiverType();
                return receiverType == null
                        ? annotationAppender
                        : receiverType.accept(AnnotationAppender.ForTypeAnnotations.ofReceiverType(annotationAppender, annotationValueFilter));
            }
        };

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethod(methodVisitor));
            annotationAppender = methodDescription.getReturnType().accept(AnnotationAppender.ForTypeAnnotations.ofMethodReturnType(annotationAppender,
                    annotationValueFilter));
            annotationAppender = AnnotationAppender.ForTypeAnnotations.ofTypeVariable(annotationAppender,
                    annotationValueFilter,
                    AnnotationAppender.ForTypeAnnotations.VARIABLE_ON_INVOKEABLE,
                    methodDescription.getTypeVariables());
            for (AnnotationDescription annotation : methodDescription.getDeclaredAnnotations().filter(not(annotationType(nameStartsWith("jdk.internal."))))) {
                annotationAppender = annotationAppender.append(annotation, annotationValueFilter);
            }
            for (ParameterDescription parameterDescription : methodDescription.getParameters()) {
                AnnotationAppender parameterAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethodParameter(methodVisitor,
                        parameterDescription.getIndex()));
                parameterAppender = parameterDescription.getType().accept(AnnotationAppender.ForTypeAnnotations.ofMethodParameterType(parameterAppender,
                        annotationValueFilter,
                        parameterDescription.getIndex()));
                for (AnnotationDescription annotation : parameterDescription.getDeclaredAnnotations()) {
                    parameterAppender = parameterAppender.append(annotation, annotationValueFilter);
                }
            }
            annotationAppender = appendReceiver(annotationAppender, annotationValueFilter, methodDescription);
            int exceptionTypeIndex = 0;
            for (TypeDescription.Generic exceptionType : methodDescription.getExceptionTypes()) {
                annotationAppender = exceptionType.accept(AnnotationAppender.ForTypeAnnotations.ofExceptionType(annotationAppender,
                        annotationValueFilter,
                        exceptionTypeIndex++));
            }
        }

        /**
         * Appends the annotations of the instrumented method's receiver type if this is enabled and such a type exists.
         *
         * @param annotationAppender    The annotation appender to use.
         * @param annotationValueFilter The annotation value filter to apply when the annotations are written.
         * @param methodDescription     The instrumented method.
         * @return The resulting annotation appender.
         */
        protected abstract AnnotationAppender appendReceiver(AnnotationAppender annotationAppender,
                                                             AnnotationValueFilter annotationValueFilter,
                                                             MethodDescription methodDescription);

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForInstrumentedMethod." + name();
        }
    }

    /**
     * Appends an annotation to a method or method parameter. The visibility of the annotation is determined by the
     * annotation type's {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    class Explicit implements MethodAttributeAppender, Factory {

        /**
         * The target to which the annotations are written to.
         */
        private final Target target;

        /**
         * the annotations this method attribute appender is writing to its target.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new appender for appending an annotation to a method.
         *
         * @param parameterIndex The index of the parameter to which the annotations should be written.
         * @param annotations    The annotations that should be written.
         */
        public Explicit(int parameterIndex, List<? extends AnnotationDescription> annotations) {
            this(new Target.OnMethodParameter(parameterIndex), annotations);
        }

        /**
         * Creates a new appender for appending an annotation to a method.
         *
         * @param annotations The annotations that should be written.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this(Target.OnMethod.INSTANCE, annotations);
        }

        /**
         * Creates an explicit annotation appender for a either a method or one of its parameters..
         *
         * @param target      The target to which the annotation should be written to.
         * @param annotations The annotations to write.
         */
        protected Explicit(Target target, List<? extends AnnotationDescription> annotations) {
            this.target = target;
            this.annotations = annotations;
        }

        /**
         * Creates a method attribute appender factory that writes all annotations of a given method, both the method
         * annotations themselves and all annotations that are defined for every parameter.
         *
         * @param methodDescription The method from which to extract the annotations.
         * @return A method attribute appender factory for an appender that writes all annotations of the supplied method.
         */
        public static Factory of(MethodDescription methodDescription) {
            ParameterList<?> parameters = methodDescription.getParameters();
            List<MethodAttributeAppender.Factory> methodAttributeAppenders = new ArrayList<MethodAttributeAppender.Factory>(parameters.size() + 1);
            methodAttributeAppenders.add(new Explicit(methodDescription.getDeclaredAnnotations()));
            for (ParameterDescription parameter : parameters) {
                methodAttributeAppenders.add(new Explicit(parameter.getIndex(), parameter.getDeclaredAnnotations()));
            }
            return new Factory.Compound(methodAttributeAppenders);
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(target.make(methodVisitor, methodDescription));
            for (AnnotationDescription annotation : annotations) {
                appender = appender.append(annotation, annotationValueFilter);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotations.equals(((Explicit) other).annotations)
                    && target.equals(((Explicit) other).target);
        }

        @Override
        public int hashCode() {
            return 31 * annotations.hashCode() + target.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.Explicit{" +
                    "annotations=" + annotations +
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
                    return "MethodAttributeAppender.Explicit.Target.OnMethod." + name();
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
                    return "MethodAttributeAppender.Explicit.Target.OnMethodParameter{parameterIndex=" + parameterIndex + '}';
                }
            }
        }
    }

    /**
     * A method attribute appender that writes a receiver type.
     */
    class ForReceiverType implements MethodAttributeAppender, Factory {

        /**
         * The receiver type for which annotations are appended to the instrumented method.
         */
        private final TypeDescription.Generic receiverType;

        /**
         * Creates a new attribute appender that writes a receiver type.
         *
         * @param receiverType The receiver type for which annotations are appended to the instrumented method.
         */
        public ForReceiverType(TypeDescription.Generic receiverType) {
            this.receiverType = receiverType;
        }

        @Override
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter) {
            receiverType.accept(AnnotationAppender.ForTypeAnnotations.ofReceiverType(new AnnotationAppender.Default(new AnnotationAppender.Target.OnMethod(methodVisitor)), annotationValueFilter));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && receiverType.equals(((ForReceiverType) other).receiverType);
        }

        @Override
        public int hashCode() {
            return receiverType.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.ForReceiverType{" +
                    "receiverType=" + receiverType +
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
        private final List<MethodAttributeAppender> methodAttributeAppenders;

        /**
         * Creates a new compound method attribute appender.
         *
         * @param methodAttributeAppender The method attribute appenders that are to be combined by this compound appender
         *                                in the order of their application.
         */
        public Compound(MethodAttributeAppender... methodAttributeAppender) {
            this(Arrays.asList(methodAttributeAppender));
        }

        /**
         * Creates a new compound method attribute appender.
         *
         * @param methodAttributeAppenders The method attribute appenders that are to be combined by this compound appender
         *                                 in the order of their application.
         */
        public Compound(List<? extends MethodAttributeAppender> methodAttributeAppenders) {
            this.methodAttributeAppenders = new ArrayList<MethodAttributeAppender>();
            for (MethodAttributeAppender methodAttributeAppender : methodAttributeAppenders) {
                if (methodAttributeAppender instanceof Compound) {
                    this.methodAttributeAppenders.addAll(((Compound) methodAttributeAppender).methodAttributeAppenders);
                } else {
                    this.methodAttributeAppenders.add(methodAttributeAppender);
                }
            }
        }

        @Override
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter) {
            for (MethodAttributeAppender methodAttributeAppender : methodAttributeAppenders) {
                methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodAttributeAppenders.equals(((Compound) other).methodAttributeAppenders);
        }

        @Override
        public int hashCode() {
            return methodAttributeAppenders.hashCode();
        }

        @Override
        public String toString() {
            return "MethodAttributeAppender.Compound{methodAttributeAppenders=" + methodAttributeAppenders + '}';
        }
    }
}
