package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * A method transformer allows to transform a method prior to its definition. This way, previously defined methods
 * can be substituted by a different method description. It is the responsibility of the method transformer that
 * the substitute method remains compatible to the substituted method.
 */
public interface MethodTransformer {

    /**
     * Transforms a method.
     *
     * @param methodDescription The method to be transformed.
     * @return The transformed method.
     */
    MethodDescription transform(MethodDescription methodDescription);

    /**
     * A method transformer that returns the original method.
     */
    enum NoOp implements MethodTransformer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public MethodDescription transform(MethodDescription methodDescription) {
            return methodDescription;
        }

        @Override
        public String toString() {
            return "MethodTransformer.NoOp." + name();
        }
    }

    /**
     * A method transformer that modifies method properties by applying a {@link net.bytebuddy.dynamic.MethodTransformer.Transforming.Transformer}.
     */
    class Transforming implements MethodTransformer {

        /**
         * The transformer to be applied.
         */
        private final Transformer transformer;

        /**
         * Creates a new transforming method transformer.
         *
         * @param transformer The transformer to be applied.
         */
        public Transforming(Transformer transformer) {
            this.transformer = transformer;
        }

        /**
         * Creates a transformer that enforces the supplied modifier contributors. All ranges of each contributor is first cleared and then overridden
         * by the specified modifiers in the order they are supplied.
         *
         * @param modifierTransformer The modifier transformers in their application order.
         * @return A method transformer where each method's modifiers are adapted to the given modifiers.
         */
        public static MethodTransformer modifiers(ModifierContributor.ForMethod... modifierTransformer) {
            return new Transforming(new Transformer.ForModifierTransformation(Arrays.asList(nonNull(modifierTransformer))));
        }

        @Override
        public MethodDescription transform(MethodDescription methodDescription) {
            return new TransformedMethod(methodDescription, transformer);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && transformer.equals(((Transforming) other).transformer);
        }

        @Override
        public int hashCode() {
            return transformer.hashCode();
        }

        @Override
        public String toString() {
            return "MethodTransformer.Transforming{" +
                    "transformer=" + transformer +
                    '}';
        }

        /**
         * A transformer is responsible for transforming a method's properties.
         *
         * @see net.bytebuddy.dynamic.MethodTransformer.Transforming.Transformer.AbstractBase
         */
        public interface Transformer {

            /**
             * Transforms a method's return type.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed return type.
             */
            GenericTypeDescription resolveReturnType(MethodDescription methodDescription);

            /**
             * Transforms a method's parameters.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed parameters.
             */
            ParameterList<?> resolveParameters(MethodDescription methodDescription);

            /**
             * Transforms a method's exception types.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed exception types.
             */
            GenericTypeList resolveExceptionTypes(MethodDescription methodDescription);

            /**
             * Transforms a method's annotations.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed annotations.
             */
            AnnotationList resolveAnnotations(MethodDescription methodDescription);

            /**
             * Transforms a method's modifiers.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed modifiers.
             */
            int resolveModifiers(MethodDescription methodDescription);

            /**
             * Transforms a method define shape's parameters.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed return type.
             */
            GenericTypeDescription resolveReturnType(MethodDescription.InDefinedShape methodDescription);

            /**
             * Transforms a method defined shape's parameters.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed parameters.
             */
            ParameterList<ParameterDescription.InDefinedShape> resolveParameters(MethodDescription.InDefinedShape methodDescription);

            /**
             * Transforms a method defined shape's exception types.
             *
             * @param methodDescription The method being transformed.
             * @return The transformed exception types.
             */
            GenericTypeList resolveExceptionTypes(MethodDescription.InDefinedShape methodDescription);

            /**
             * An adapter implementation of a transformer that simply returns the untransformed value for each transformation.
             */
            class AbstractBase implements Transformer {

                @Override
                public GenericTypeDescription resolveReturnType(MethodDescription methodDescription) {
                    return methodDescription.getReturnType();
                }

                @Override
                public ParameterList<?> resolveParameters(MethodDescription methodDescription) {
                    return methodDescription.getParameters();
                }

                @Override
                public GenericTypeList resolveExceptionTypes(MethodDescription methodDescription) {
                    return methodDescription.getExceptionTypes();
                }

                @Override
                public AnnotationList resolveAnnotations(MethodDescription methodDescription) {
                    return methodDescription.getDeclaredAnnotations();
                }

                @Override
                public int resolveModifiers(MethodDescription methodDescription) {
                    return methodDescription.getModifiers();
                }

                @Override
                public GenericTypeDescription resolveReturnType(MethodDescription.InDefinedShape methodDescription) {
                    return methodDescription.getReturnType();
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> resolveParameters(MethodDescription.InDefinedShape methodDescription) {
                    return methodDescription.getParameters();
                }

                @Override
                public GenericTypeList resolveExceptionTypes(MethodDescription.InDefinedShape methodDescription) {
                    return methodDescription.getExceptionTypes();
                }
            }

            /**
             * A transformer that changes a method's modifiers to imply the instances modifier contributors.
             */
            class ForModifierTransformation extends AbstractBase {

                /**
                 * The modifier contributors to apply on each transformation.
                 */
                private final List<? extends ModifierContributor.ForMethod> modifierContributors;

                /**
                 * Creates a new modifier transformation.
                 *
                 * @param modifierContributors The modifier contributors to apply on each transformation in their application order.
                 */
                public ForModifierTransformation(List<? extends ModifierContributor.ForMethod> modifierContributors) {
                    this.modifierContributors = modifierContributors;
                }

                @Override
                public int resolveModifiers(MethodDescription methodDescription) {
                    int modifiers = methodDescription.getModifiers();
                    for (ModifierContributor.ForMethod modifierContributor : modifierContributors) {
                        modifiers = (modifiers & ~modifierContributor.getRange()) | modifierContributor.getMask();
                    }
                    return modifiers;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && modifierContributors.equals(((ForModifierTransformation) other).modifierContributors);
                }

                @Override
                public int hashCode() {
                    return modifierContributors.hashCode();
                }

                @Override
                public String toString() {
                    return "MethodTransformer.Transforming.Transformer.ForModifierTransformation{" +
                            "modifierContributors=" + modifierContributors +
                            '}';
                }
            }
        }

        /**
         * A representation of a transformed method.
         */
        protected static class TransformedMethod extends MethodDescription.AbstractBase {

            /**
             * The original method that is being transformed.
             */
            private final MethodDescription methodDescription;

            /**
             * The transformer to be applied.
             */
            private final Transformer transformer;

            /**
             * Creates a new transformed method.
             *
             * @param methodDescription The original method that is being transformed.
             * @param transformer       The transformer to be applied.
             */
            protected TransformedMethod(MethodDescription methodDescription, Transformer transformer) {
                this.methodDescription = methodDescription;
                this.transformer = transformer;
            }

            @Override
            public GenericTypeDescription getReturnType() {
                return transformer.resolveReturnType(methodDescription);
            }

            @Override
            public ParameterList<?> getParameters() {
                return transformer.resolveParameters(methodDescription);
            }

            @Override
            public GenericTypeList getExceptionTypes() {
                return transformer.resolveExceptionTypes(methodDescription);
            }

            @Override
            public Object getDefaultValue() {
                return methodDescription.getDefaultValue();
            }

            @Override
            public GenericTypeList getTypeVariables() {
                return transformer.resolveExceptionTypes(methodDescription);
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return transformer.resolveAnnotations(methodDescription);
            }

            @Override
            public GenericTypeDescription getDeclaringType() {
                return methodDescription.getDeclaringType();
            }

            @Override
            public int getModifiers() {
                return transformer.resolveModifiers(methodDescription);
            }

            @Override
            public InDefinedShape asDefined() {
                return new InDefinedShape(methodDescription.asDefined(), transformer);
            }

            @Override
            public String getInternalName() {
                return methodDescription.getInternalName();
            }

            /**
             * Representation of a transformed method in its defined shape.
             */
            protected static class InDefinedShape extends MethodDescription.InDefinedShape.AbstractBase {

                /**
                 * The transformed method.
                 */
                private final MethodDescription.InDefinedShape methodDescription;

                /**
                 * The transformer to be applied.
                 */
                private final Transformer transformer;

                /**
                 * Creates a new transformed method in its defined shape.
                 *
                 * @param methodDescription The transformed method.
                 * @param transformer       The transformer to be applied.
                 */
                protected InDefinedShape(MethodDescription.InDefinedShape methodDescription, Transformer transformer) {
                    this.methodDescription = methodDescription;
                    this.transformer = transformer;
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return transformer.resolveReturnType(methodDescription);
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return transformer.resolveParameters(methodDescription);
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return transformer.resolveExceptionTypes(methodDescription);
                }

                @Override
                public Object getDefaultValue() {
                    return methodDescription.getDefaultValue();
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return transformer.resolveExceptionTypes(methodDescription);
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return transformer.resolveAnnotations(methodDescription);
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return methodDescription.getDeclaringType();
                }

                @Override
                public int getModifiers() {
                    return transformer.resolveModifiers(methodDescription);
                }

                @Override
                public String getInternalName() {
                    return methodDescription.getInternalName();
                }
            }
        }
    }

    /**
     * A method transformer that applies several method transformers in a row.
     */
    class Compound implements MethodTransformer {

        /**
         * The method transformers in their application order.
         */
        private final List<? extends MethodTransformer> methodTransformers;

        /**
         * Creates a new compound method transformer.
         *
         * @param methodTransformer The method transformers in their application order.
         */
        public Compound(MethodTransformer... methodTransformer) {
            this(Arrays.asList(methodTransformer));
        }

        /**
         * Creates a new compound method transformer.
         *
         * @param methodTransformers The method transformers in their application order.
         */
        public Compound(List<? extends MethodTransformer> methodTransformers) {
            this.methodTransformers = methodTransformers;
        }

        @Override
        public MethodDescription transform(MethodDescription methodDescription) {
            MethodDescription transformed = methodDescription;
            for (MethodTransformer methodTransformer : methodTransformers) {
                transformed = methodTransformer.transform(transformed);
            }
            return transformed;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other != null && getClass() == other.getClass()
                    && methodTransformers.equals(((Compound) other).methodTransformers);
        }

        @Override
        public int hashCode() {
            return methodTransformers.hashCode();
        }

        @Override
        public String toString() {
            return "MethodTransformer.Compound{" +
                    "methodTransformers=" + methodTransformers +
                    '}';
        }
    }
}
