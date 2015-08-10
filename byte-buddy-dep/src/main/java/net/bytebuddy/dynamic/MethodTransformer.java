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
 * the substitute method remains signature compatible to the substituted method.
 */
public interface MethodTransformer {

    /**
     * Transforms a method.
     *
     * @param instrumentedType  The instrumented type.
     * @param methodDescription The method to be transformed.
     * @return The transformed method.
     */
    MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription);

    /**
     * A method transformer that returns the original method.
     */
    enum NoOp implements MethodTransformer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription) {
            return methodDescription;
        }

        @Override
        public String toString() {
            return "MethodTransformer.NoOp." + name();
        }
    }

    /**
     * A method transformer that modifies method properties by applying a {@link Simple.Transformer}.
     */
    class Simple implements MethodTransformer {

        /**
         * The transformer to be applied.
         */
        private final Transformer transformer;

        /**
         * Creates a new transforming method transformer.
         *
         * @param transformer The transformer to be applied.
         */
        public Simple(Transformer transformer) {
            this.transformer = transformer;
        }

        /**
         * Creates a transformer that enforces the supplied modifier contributors. All ranges of each contributor is first cleared and then overridden
         * by the specified modifiers in the order they are supplied.
         *
         * @param modifierTransformer The modifier transformers in their application order.
         * @return A method transformer where each method's modifiers are adapted to the given modifiers.
         */
        public static MethodTransformer withModifiers(ModifierContributor.ForMethod... modifierTransformer) {
            return new Simple(new Transformer.ForModifierTransformation(Arrays.asList(nonNull(modifierTransformer))));
        }

        @Override
        public MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription) {
            return new TransformedMethod(methodDescription.getDeclaringType(), transformer.transform(methodDescription.asToken()), methodDescription.asDefined());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && transformer.equals(((Simple) other).transformer);
        }

        @Override
        public int hashCode() {
            return transformer.hashCode();
        }

        @Override
        public String toString() {
            return "MethodTransformer.Simple{" +
                    "transformer=" + transformer +
                    '}';
        }

        /**
         * A transformer is responsible for transforming a method token into its transformed form.
         */
        public interface Transformer {

            /**
             * Transforms a method token.
             *
             * @param methodToken The original method's token.
             * @return The transformed method token.
             */
            MethodDescription.Token transform(MethodDescription.Token methodToken);

            /**
             * A transformation for a modifier transformation.
             */
            class ForModifierTransformation implements Transformer {

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
                public MethodDescription.Token transform(MethodDescription.Token methodToken) {
                    int modifiers = methodToken.getModifiers();
                    for (ModifierContributor.ForMethod modifierContributor : modifierContributors) {
                        modifiers = (modifiers & ~modifierContributor.getRange()) | modifierContributor.getMask();
                    }
                    return new MethodDescription.Token(methodToken.getInternalName(),
                            modifiers,
                            methodToken.getTypeVariables(),
                            methodToken.getReturnType(),
                            methodToken.getParameterTokens(),
                            methodToken.getExceptionTypes(),
                            methodToken.getAnnotations(),
                            methodToken.getDefaultValue());
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
                    return "MethodTransformer.Simple.Transformer.ForModifierTransformation{" +
                            "modifierContributors=" + modifierContributors +
                            '}';
                }
            }
        }

        /**
         * The transformed method.
         */
        protected static class TransformedMethod extends MethodDescription.AbstractBase {

            /**
             * The method's declaring type.
             */
            private final GenericTypeDescription declaringType;

            /**
             * The method representing the transformed method.
             */
            private final MethodDescription.Token methodToken;

            /**
             * The defined shape of the transformed method.
             */
            private final MethodDescription.InDefinedShape definedShape;

            /**
             * Creates a new transformed method.
             *
             * @param declaringType The method's declaring type.
             * @param methodToken   The method representing the transformed method.
             * @param definedShape  The defined shape of the transformed method.
             */
            protected TransformedMethod(GenericTypeDescription declaringType,
                                        MethodDescription.Token methodToken,
                                        MethodDescription.InDefinedShape definedShape) {
                this.declaringType = declaringType;
                this.methodToken = methodToken;
                this.definedShape = definedShape;
            }

            @Override
            public GenericTypeList getTypeVariables() {
                return GenericTypeList.ForDetachedTypes.OfTypeVariable.attach(this, methodToken.getTypeVariables());
            }

            @Override
            public GenericTypeDescription getReturnType() {
                return methodToken.getReturnType().accept(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(this));
            }

            @Override
            public ParameterList<?> getParameters() {
                return new TransformedParameterList();
            }

            @Override
            public GenericTypeList getExceptionTypes() {
                return GenericTypeList.ForDetachedTypes.attach(this, methodToken.getExceptionTypes());
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return methodToken.getAnnotations();
            }

            @Override
            public String getInternalName() {
                return methodToken.getInternalName();
            }

            @Override
            public GenericTypeDescription getDeclaringType() {
                return declaringType;
            }

            @Override
            public int getModifiers() {
                return methodToken.getModifiers();
            }

            @Override
            public Object getDefaultValue() {
                return methodToken.getDefaultValue();
            }

            @Override
            public InDefinedShape asDefined() {
                return definedShape;
            }

            /**
             * A parameter list representing the transformed method's parameters.
             */
            protected class TransformedParameterList extends ParameterList.AbstractBase<ParameterDescription> {

                @Override
                public ParameterDescription get(int index) {
                    return new TransformedParameter(index, methodToken.getParameterTokens().get(index));
                }

                @Override
                public int size() {
                    return methodToken.getParameterTokens().size();
                }
            }

            /**
             * A transformed method's parameter.
             */
            protected class TransformedParameter extends ParameterDescription.AbstractBase {

                /**
                 * The index of the transformed method.
                 */
                private final int index;

                /**
                 * The token representing the transformed method parameter's properties.
                 */
                private final ParameterDescription.Token parameterToken;

                /**
                 * Creates a transformed parameter.
                 *
                 * @param index          The index of the transformed method.
                 * @param parameterToken The token representing the transformed method parameter's properties.
                 */
                protected TransformedParameter(int index, ParameterDescription.Token parameterToken) {
                    this.index = index;
                    this.parameterToken = parameterToken;
                }

                @Override
                public GenericTypeDescription getType() {
                    return parameterToken.getType().accept(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(this));
                }

                @Override
                public MethodDescription getDeclaringMethod() {
                    return TransformedMethod.this;
                }

                @Override
                public int getIndex() {
                    return index;
                }

                @Override
                public boolean isNamed() {
                    return parameterToken.getName() != null;
                }

                @Override
                public boolean hasModifiers() {
                    return parameterToken.getModifiers() != null;
                }

                @Override
                public String getName() {
                    return isNamed()
                            ? parameterToken.getName()
                            : super.getName();
                }

                @Override
                public int getModifiers() {
                    return hasModifiers()
                            ? parameterToken.getModifiers()
                            : super.getModifiers();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return parameterToken.getAnnotations();
                }

                @Override
                public InDefinedShape asDefined() {
                    return definedShape.getParameters().get(index);
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
        public MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription) {
            MethodDescription transformed = methodDescription;
            for (MethodTransformer methodTransformer : methodTransformers) {
                transformed = methodTransformer.transform(instrumentedType, transformed);
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
