package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * A method transformer allows to transform a method prior to its definition. This way, previously defined methods
 * can be substituted by a different method description. It is the responsibility of the method transformer that
 * the substitute method remains compatible to the substituted method.
 */
public interface MethodTransformer {

    /**
     * Transforms a method. The transformed method is <b>not</b> validated by Byte Buddy and it is the responsibility
     * of the transformer to assure the validity of the transformation.
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
     * A method transformer that modifies method properties by applying a {@link TokenTransformer}.
     */
    class Simple implements MethodTransformer {

        /**
         * The transformer to be applied.
         */
        private final TokenTransformer tokenTransformer;

        /**
         * Creates a new transforming method transformer.
         *
         * @param tokenTransformer The transformer to be applied.
         */
        public Simple(TokenTransformer tokenTransformer) {
            this.tokenTransformer = tokenTransformer;
        }

        /**
         * Creates a transformer that enforces the supplied modifier contributors. All ranges of each contributor is first cleared and then overridden
         * by the specified modifiers in the order they are supplied.
         *
         * @param modifierTransformer The modifier transformers in their application order.
         * @return A method transformer where each method's modifiers are adapted to the given modifiers.
         */
        public static MethodTransformer withModifiers(ModifierContributor.ForMethod... modifierTransformer) {
            return new Simple(new TokenTransformer.ForModifierTransformation(Arrays.asList(modifierTransformer)));
        }

        @Override
        public MethodDescription transform(TypeDescription instrumentedType, MethodDescription methodDescription) {
            return new TransformedMethod(methodDescription.getDeclaringType(),
                    tokenTransformer.transform(methodDescription.asToken(is(instrumentedType))), methodDescription.asDefined());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && tokenTransformer.equals(((Simple) other).tokenTransformer);
        }

        @Override
        public int hashCode() {
            return tokenTransformer.hashCode();
        }

        @Override
        public String toString() {
            return "MethodTransformer.Simple{" +
                    "tokenTransformer=" + tokenTransformer +
                    '}';
        }

        /**
         * A transformer is responsible for transforming a method token into its transformed form.
         */
        public interface TokenTransformer {

            /**
             * Transforms a method token.
             *
             * @param token The original method's token.
             * @return The transformed method token.
             */
            MethodDescription.Token transform(MethodDescription.Token token);

            /**
             * A transformation for a modifier transformation.
             */
            class ForModifierTransformation implements TokenTransformer {

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
                public MethodDescription.Token transform(MethodDescription.Token token) {
                    return new MethodDescription.Token(token.getName(),
                            ModifierContributor.Resolver.of(modifierContributors).resolve(token.getModifiers()),
                            token.getTypeVariableTokens(),
                            token.getReturnType(),
                            token.getParameterTokens(),
                            token.getExceptionTypes(),
                            token.getAnnotations(),
                            token.getDefaultValue(),
                            token.getReceiverType());
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
                    return "MethodTransformer.Simple.TokenTransformer.ForModifierTransformation{" +
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
            private final TypeDefinition declaringType;

            /**
             * The method representing the transformed method.
             */
            private final MethodDescription.Token token;

            /**
             * The defined shape of the transformed method.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * Creates a new transformed method.
             *
             * @param declaringType The method's declaring type.
             * @param token   The method representing the transformed method.
             * @param methodDescription  The defined shape of the transformed method.
             */
            protected TransformedMethod(TypeDefinition declaringType,
                                        MethodDescription.Token token,
                                        MethodDescription.InDefinedShape methodDescription) {
                this.declaringType = declaringType;
                this.token = token;
                this.methodDescription = methodDescription;
            }

            @Override
            public TypeList.Generic getTypeVariables() {
                return TypeList.Generic.ForDetachedTypes.attachVariables(this, token.getTypeVariableTokens());
            }

            @Override
            public TypeDescription.Generic getReturnType() {
                return token.getReturnType().accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
            }

            @Override
            public ParameterList<?> getParameters() {
                return new TransformedParameterList();
            }

            @Override
            public TypeList.Generic getExceptionTypes() {
                return TypeList.Generic.ForDetachedTypes.attach(this, token.getExceptionTypes());
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return token.getAnnotations();
            }

            @Override
            public String getInternalName() {
                return token.getName();
            }

            @Override
            public TypeDefinition getDeclaringType() {
                return declaringType;
            }

            @Override
            public int getModifiers() {
                return token.getModifiers();
            }

            @Override
            public Object getDefaultValue() {
                return token.getDefaultValue();
            }

            @Override
            public InDefinedShape asDefined() {
                return methodDescription;
            }

            @Override
            public TypeDescription.Generic getReceiverType() {
                return methodDescription.getReceiverType();
            }

            /**
             * A parameter list representing the transformed method's parameters.
             */
            protected class TransformedParameterList extends ParameterList.AbstractBase<ParameterDescription> {

                @Override
                public ParameterDescription get(int index) {
                    return new TransformedParameter(index, token.getParameterTokens().get(index));
                }

                @Override
                public int size() {
                    return token.getParameterTokens().size();
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
                public TypeDescription.Generic getType() {
                    return parameterToken.getType().accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
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
                    return methodDescription.getParameters().get(index);
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
